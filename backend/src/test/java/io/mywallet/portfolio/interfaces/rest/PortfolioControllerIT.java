package io.mywallet.portfolio.interfaces.rest;

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

/**
 * End-to-end: register -> create account -> create portfolio -> read positions (empty at
 * this phase) -> confirm a second user cannot see the first user's portfolio (404, not
 * 403 - see the isolation note in PortfolioApplicationService).
 */
class PortfolioControllerIT extends PostgresIntegrationTest {

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

    @Test
    void createAccountThenPortfolioThenReadEmptyPositions() throws Exception {
        MockMvc mvc = mockMvc();
        String token = registerAndGetAccessToken(mvc, "portfolio-owner+" + System.nanoTime() + "@mywallet.dev");

        String accountResponse = mvc.perform(post("/api/accounts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type": "SIMULATED", "displayName": "My simulated account"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String accountId = objectMapper.readTree(accountResponse).get("id").asText();

        String portfolioResponse = mvc.perform(post("/api/portfolios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "%s", "name": "Main portfolio", "mode": "SIMULATED", "initialCashBalance": 10000.00}
                    """.formatted(accountId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cashBalance").value(10000.00))
            .andReturn().getResponse().getContentAsString();
        String portfolioId = objectMapper.readTree(portfolioResponse).get("id").asText();

        mvc.perform(get("/api/portfolios/" + portfolioId + "/positions").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        mvc.perform(get("/api/portfolios").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(portfolioId));
    }

    @Test
    void anotherUserCannotSeeSomeoneElsesPortfolio() throws Exception {
        MockMvc mvc = mockMvc();
        String ownerToken = registerAndGetAccessToken(mvc, "owner+" + System.nanoTime() + "@mywallet.dev");
        String intruderToken = registerAndGetAccessToken(mvc, "intruder+" + System.nanoTime() + "@mywallet.dev");

        String accountResponse = mvc.perform(post("/api/accounts")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type": "DEMO", "displayName": "Owner's account"}
                    """))
            .andReturn().getResponse().getContentAsString();
        String accountId = objectMapper.readTree(accountResponse).get("id").asText();

        String portfolioResponse = mvc.perform(post("/api/portfolios")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "%s", "name": "Private portfolio", "mode": "DEMO", "initialCashBalance": 1000.00}
                    """.formatted(accountId)))
            .andReturn().getResponse().getContentAsString();
        String portfolioId = objectMapper.readTree(portfolioResponse).get("id").asText();

        mvc.perform(get("/api/portfolios/" + portfolioId).header("Authorization", "Bearer " + intruderToken))
            .andExpect(status().isNotFound());
    }
}
