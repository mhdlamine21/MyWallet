package io.mywallet.order.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.asset.infrastructure.persistence.AssetEntity;
import io.mywallet.asset.infrastructure.persistence.AssetJpaRepository;
import io.mywallet.auth.interfaces.rest.dto.RegisterRequest;
import io.mywallet.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The end-to-end proof that Phase 5's wiring actually works: create an order, simulate a
 * fill, and confirm - asynchronously, through the real RabbitMQ round trip - that the
 * position and cash balance update. Also proves idempotence (test scenario #14) by
 * resubmitting the exact same execution and confirming nothing double-applies.
 */
class OrderExecutionFlowIT extends PostgresIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssetJpaRepository assetRepository;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void fullOrderLifecycle_createFillAndVerifyPositionAndCashBalanceAsynchronously() throws Exception {
        MockMvc mvc = mockMvc();

        AssetEntity asset = assetRepository.save(new AssetEntity(
            UUID.randomUUID(), "TEST" + System.nanoTime(), AssetEntity.AssetClass.CRYPTO, "USD",
            "Test Asset", new BigDecimal("50.00"), new BigDecimal("0.05"), new BigDecimal("0.30"), 123L
        ));

        String token = registerAndGetAccessToken(mvc, "trader+" + System.nanoTime() + "@mywallet.dev");

        String accountId = createAccount(mvc, token);
        String portfolioId = createPortfolio(mvc, token, accountId, "100000.00");

        String orderResponse = mvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"portfolioId": "%s", "assetId": "%s", "orderType": "LIMIT", "side": "BUY",
                     "quantity": 10, "limitPrice": 50.00}
                    """.formatted(portfolioId, asset.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(orderResponse).get("id").asText();

        String externalReference = "exec-" + UUID.randomUUID();
        mvc.perform(post("/api/orders/" + orderId + "/executions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"quantity": 10, "executionPrice": 51.00, "fees": 1.00, "externalReference": "%s"}
                    """.formatted(externalReference)))
            .andExpect(status().isAccepted());

        // Order status flips synchronously (RecordExecutionService updates the order
        // projection in the same transaction as the fill) - no waiting needed here.
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FILLED"))
            .andExpect(jsonPath("$.filledQuantity").value(10));

        // Position and cash balance update asynchronously via RabbitMQ - poll for them.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            mvc.perform(get("/api/portfolios/" + portfolioId + "/positions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assetId").value(asset.getId().toString()))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].averageAcquisitionPrice").value(51.0))
        );

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            mvc.perform(get("/api/portfolios/" + portfolioId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // 100000 - (10 * 51.00 + 1.00 fee) = 99489.00
                .andExpect(jsonPath("$.cashBalance").value(99489.00))
        );

        // Event history shows exactly the two events this lifecycle raised.
        String eventsResponse = mvc.perform(get("/api/orders/" + orderId + "/events").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode events = objectMapper.readTree(eventsResponse);
        org.assertj.core.api.Assertions.assertThat(events).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(events.get(0).get("eventType").asText()).isEqualTo("OrderCreated");
        org.assertj.core.api.Assertions.assertThat(events.get(1).get("eventType").asText()).isEqualTo("OrderFilled");

        // Idempotence: resubmitting the exact same execution must not double-apply.
        mvc.perform(post("/api/orders/" + orderId + "/executions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"quantity": 10, "executionPrice": 51.00, "fees": 1.00, "externalReference": "%s"}
                    """.formatted(externalReference)))
            .andExpect(status().isAccepted());

        Thread.sleep(2000); // give a would-be (incorrect) double-processing time to happen
        mvc.perform(get("/api/portfolios/" + portfolioId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashBalance").value(99489.00)); // unchanged
    }

    private String registerAndGetAccessToken(MockMvc mvc, String email) throws Exception {
        String response = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "a-strong-password-123"))))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createAccount(MockMvc mvc, String token) throws Exception {
        String response = mvc.perform(post("/api/accounts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type": "SIMULATED", "displayName": "Trading account"}
                    """))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createPortfolio(MockMvc mvc, String token, String accountId, String initialCash) throws Exception {
        String response = mvc.perform(post("/api/portfolios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "%s", "name": "Main portfolio", "mode": "SIMULATED", "initialCashBalance": %s}
                    """.formatted(accountId, initialCash)))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }
}
