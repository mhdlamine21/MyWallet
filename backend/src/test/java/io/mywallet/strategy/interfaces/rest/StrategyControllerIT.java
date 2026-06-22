package io.mywallet.strategy.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.auth.interfaces.rest.dto.RegisterRequest;
import io.mywallet.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StrategyControllerIT extends PostgresIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String registerAndGetAccessToken(MockMvc mvc, String email) throws Exception {
        String response = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "a-strong-password-123"))))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createPortfolio(MockMvc mvc, String token) throws Exception {
        String accountResponse = mvc.perform(post("/api/accounts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type": "SIMULATED", "displayName": "Strategy test account"}
                    """))
            .andReturn().getResponse().getContentAsString();
        String accountId = objectMapper.readTree(accountResponse).get("id").asText();

        String portfolioResponse = mvc.perform(post("/api/portfolios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "%s", "name": "Strategy portfolio", "mode": "SIMULATED", "initialCashBalance": 10000.00}
                    """.formatted(accountId)))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(portfolioResponse).get("id").asText();
    }

    @Test
    void createActivateSuspendAndDeactivateLifecycle() throws Exception {
        MockMvc mvc = mockMvc();
        String token = registerAndGetAccessToken(mvc, "strategist+" + System.nanoTime() + "@mywallet.dev");
        String portfolioId = createPortfolio(mvc, token);

        String createResponse = mvc.perform(post("/api/strategies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"portfolioId": "%s", "name": "SMA crossover", "mode": "PAPER",
                     "ruleExpression": "SMA(BTCUSDT, 20) > SMA(BTCUSDT, 50) AND RSI(BTCUSDT, 14) < 70 AND PortfolioExposure < 50%%"}
                    """.formatted(portfolioId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();
        String strategyId = objectMapper.readTree(createResponse).get("id").asText();

        mvc.perform(post("/api/strategies/" + strategyId + "/activate").header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/strategies/" + strategyId).header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(post("/api/strategies/" + strategyId + "/suspend").header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/strategies/" + strategyId).header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mvc.perform(post("/api/strategies/" + strategyId + "/deactivate").header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/strategies/" + strategyId).header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.status").value("DEACTIVATED"));

        // DEACTIVATED is terminal - reactivating must fail.
        mvc.perform(post("/api/strategies/" + strategyId + "/activate").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_STRATEGY_TRANSITION"));
    }

    @Test
    void rejectsAnUnsafeOrInvalidRuleExpressionAtCreationTime() throws Exception {
        MockMvc mvc = mockMvc();
        String token = registerAndGetAccessToken(mvc, "strategist2+" + System.nanoTime() + "@mywallet.dev");
        String portfolioId = createPortfolio(mvc, token);

        mvc.perform(post("/api/strategies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"portfolioId": "%s", "name": "Malicious attempt", "mode": "PAPER",
                     "ruleExpression": "SMA(BTCUSDT, 20) > 100; Runtime.getRuntime().exec(\\"rm -rf /\\")"}
                    """.formatted(portfolioId)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_RULE_EXPRESSION"));
    }
}
