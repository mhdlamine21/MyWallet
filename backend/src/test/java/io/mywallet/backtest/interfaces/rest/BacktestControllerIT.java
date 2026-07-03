package io.mywallet.backtest.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.asset.infrastructure.persistence.AssetEntity;
import io.mywallet.asset.infrastructure.persistence.AssetJpaRepository;
import io.mywallet.auth.interfaces.rest.dto.RegisterRequest;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceEntity;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceJpaRepository;
import io.mywallet.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BacktestControllerIT extends PostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AssetJpaRepository assetRepository;
    @Autowired private MarketPriceJpaRepository marketPriceRepository;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void runsABacktestAgainstRealHistoricalPricesAndReturnsAMonteCarloBand() throws Exception {
        MockMvc mvc = mockMvc();
        String token = registerAndGetAccessToken(mvc, "backtester+" + System.nanoTime() + "@mywallet.dev");

        AssetEntity asset = assetRepository.save(new AssetEntity(
            UUID.randomUUID(), "BT" + System.nanoTime(), AssetEntity.AssetClass.CRYPTO, "USD",
            "Backtest asset", new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("0.3"), 1L
        ));

        Instant start = Instant.now().minus(30, ChronoUnit.DAYS);
        double[] dailyPrices = {100, 102, 105, 103, 108, 110, 107, 112, 115, 111};
        for (int i = 0; i < dailyPrices.length; i++) {
            marketPriceRepository.save(new MarketPriceEntity(
                UUID.randomUUID(), asset.getId(), BigDecimal.valueOf(dailyPrices[i]), start.plus(i, ChronoUnit.DAYS)
            ));
        }
        Instant end = start.plus(dailyPrices.length, ChronoUnit.DAYS);

        String accountId = createAccount(mvc, token);
        String portfolioId = createPortfolio(mvc, token, accountId);
        String strategyId = createStrategy(mvc, token, portfolioId, "1 == 1"); // always-long strategy

        String response = mvc.perform(post("/api/strategies/" + strategyId + "/backtest")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assetId": "%s", "initialCapital": 1000, "periodStart": "%s", "periodEnd": "%s", "seed": 42}
                    """.formatted(asset.getId(), start, end)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(response);
        // Always-long from tick 0 at price 100, final price 111 -> 11% total return, frictionless.
        assertThat(json.get("totalReturn").asDouble()).isCloseTo(0.11, org.assertj.core.api.Assertions.within(0.001));
        assertThat(json.get("numberOfTrades").asInt()).isEqualTo(1);
        assertThat(json.get("equityCurve").size()).isEqualTo(dailyPrices.length);
        assertThat(json.get("monteCarloP50").size()).isEqualTo(dailyPrices.length); // p50[0]=initial, p50[n-1]=final tick
        assertThat(json.get("monteCarloP5").get(0).asDouble()).isCloseTo(1000.0, org.assertj.core.api.Assertions.within(0.01));
    }

    @Test
    void rejectsABacktestWithNotEnoughPriceHistoryInThePeriod() throws Exception {
        MockMvc mvc = mockMvc();
        String token = registerAndGetAccessToken(mvc, "backtester2+" + System.nanoTime() + "@mywallet.dev");

        AssetEntity asset = assetRepository.save(new AssetEntity(
            UUID.randomUUID(), "BT2" + System.nanoTime(), AssetEntity.AssetClass.CRYPTO, "USD",
            "Sparse asset", new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("0.3"), 1L
        ));
        // Only one price point saved -> not enough for a backtest.
        marketPriceRepository.save(new MarketPriceEntity(UUID.randomUUID(), asset.getId(), new BigDecimal("100"), Instant.now()));

        String accountId = createAccount(mvc, token);
        String portfolioId = createPortfolio(mvc, token, accountId);
        String strategyId = createStrategy(mvc, token, portfolioId, "1 == 1");

        mvc.perform(post("/api/strategies/" + strategyId + "/backtest")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assetId": "%s", "initialCapital": 1000, "periodStart": "%s", "periodEnd": "%s"}
                    """.formatted(asset.getId(), Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
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
                    {"type": "SIMULATED", "displayName": "Backtest account"}
                    """))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createPortfolio(MockMvc mvc, String token, String accountId) throws Exception {
        String response = mvc.perform(post("/api/portfolios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accountId": "%s", "name": "Backtest portfolio", "mode": "SIMULATED", "initialCashBalance": 10000.00}
                    """.formatted(accountId)))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createStrategy(MockMvc mvc, String token, String portfolioId, String ruleExpression) throws Exception {
        String response = mvc.perform(post("/api/strategies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"portfolioId": "%s", "name": "Backtest strategy", "mode": "BACKTEST", "ruleExpression": "%s"}
                    """.formatted(portfolioId, ruleExpression)))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }
}
