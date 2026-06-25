package io.mywallet.risk.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.asset.infrastructure.persistence.AssetEntity;
import io.mywallet.asset.infrastructure.persistence.AssetJpaRepository;
import io.mywallet.auth.interfaces.rest.dto.LoginRequest;
import io.mywallet.auth.interfaces.rest.dto.RegisterRequest;
import io.mywallet.risk.infrastructure.persistence.RiskLimitEntity;
import io.mywallet.risk.infrastructure.persistence.RiskLimitJpaRepository;
import io.mywallet.support.PostgresIntegrationTest;
import io.mywallet.user.infrastructure.persistence.RoleJpaRepository;
import io.mywallet.user.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RiskAndKillSwitchFlowIT extends PostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AssetJpaRepository assetRepository;
    @Autowired private RiskLimitJpaRepository riskLimitRepository;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private RoleJpaRepository roleRepository;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void orderExceedingConfiguredMaxOrderValueIsRejectedAndLoggedAsAnAlert() throws Exception {
        MockMvc mvc = mockMvc();
        String email = "riskuser+" + System.nanoTime() + "@mywallet.dev";
        String token = registerAndGetAccessToken(mvc, email);

        AssetEntity asset = assetRepository.save(new AssetEntity(
            UUID.randomUUID(), "RISK" + System.nanoTime(), AssetEntity.AssetClass.STOCK, "USD",
            "Risk Test Asset", new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("0.2"), 1L
        ));

        String accountId = createAccount(mvc, token);
        String portfolioId = createPortfolio(mvc, token, accountId, "100000.00");

        riskLimitRepository.save(new RiskLimitEntity(UUID.randomUUID(), UUID.fromString(portfolioId), "MAX_ORDER_VALUE", new BigDecimal("100")));

        // 10 * 50.00 = 500 > 100 configured limit
        mvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"portfolioId": "%s", "assetId": "%s", "orderType": "LIMIT", "side": "BUY", "quantity": 10, "limitPrice": 50.00}
                    """.formatted(portfolioId, asset.getId())))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("RISK_LIMIT_BREACHED"));

        mvc.perform(get("/api/risk/alerts").param("portfolioId", portfolioId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].limitType").value("MAX_ORDER_VALUE"))
            .andExpect(jsonPath("$[0].level").exists());
    }

    @Test
    void killSwitchBlocksAllNewOrdersUntilDeactivated() throws Exception {
        MockMvc mvc = mockMvc();
        String email = "admin+" + System.nanoTime() + "@mywallet.dev";
        String investorToken = registerAndGetAccessToken(mvc, email);
        promoteToAdmin(email);
        String adminToken = login(mvc, email);

        AssetEntity asset = assetRepository.save(new AssetEntity(
            UUID.randomUUID(), "KS" + System.nanoTime(), AssetEntity.AssetClass.STOCK, "USD",
            "Kill Switch Test Asset", new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("0.2"), 1L
        ));
        String accountId = createAccount(mvc, adminToken);
        String portfolioId = createPortfolio(mvc, adminToken, accountId, "100000.00");

        mvc.perform(post("/api/admin/emergency-stop")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason": "integration test"}
                    """))
            .andExpect(status().isOk());

        mvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"portfolioId": "%s", "assetId": "%s", "orderType": "LIMIT", "side": "BUY", "quantity": 1, "limitPrice": 10.00}
                    """.formatted(portfolioId, asset.getId())))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("KILL_SWITCH_ACTIVE"));

        mvc.perform(post("/api/admin/emergency-stop/deactivate").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"portfolioId": "%s", "assetId": "%s", "orderType": "LIMIT", "side": "BUY", "quantity": 1, "limitPrice": 10.00}
                    """.formatted(portfolioId, asset.getId())))
            .andExpect(status().isCreated());
    }

    @Test
    void nonAdminCannotTriggerEmergencyStop() throws Exception {
        MockMvc mvc = mockMvc();
        String token = registerAndGetAccessToken(mvc, "notadmin+" + System.nanoTime() + "@mywallet.dev");

        mvc.perform(post("/api/admin/emergency-stop").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    private void promoteToAdmin(String email) {
        var user = userRepository.findByEmail(email).orElseThrow();
        var adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        user.assignRole(adminRole);
        userRepository.save(user);
    }

    private String registerAndGetAccessToken(MockMvc mvc, String email) throws Exception {
        String response = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "a-strong-password-123"))))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String login(MockMvc mvc, String email) throws Exception {
        String response = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "a-strong-password-123"))))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createAccount(MockMvc mvc, String token) throws Exception {
        String response = mvc.perform(post("/api/accounts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type": "SIMULATED", "displayName": "Risk test account"}
                    """))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createPortfolio(MockMvc mvc, String token, String accountId, String initialCash) throws Exception {
        String response = mvc.perform(post("/api/portfolios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "%s", "name": "Risk portfolio", "mode": "SIMULATED", "initialCashBalance": %s}
                    """.formatted(accountId, initialCash)))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }
}
