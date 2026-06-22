package io.mywallet.strategy.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.account.application.AccountApplicationService;
import io.mywallet.account.infrastructure.persistence.AccountEntity;
import io.mywallet.asset.infrastructure.persistence.AssetEntity;
import io.mywallet.asset.infrastructure.persistence.AssetJpaRepository;
import io.mywallet.auth.interfaces.rest.dto.RegisterRequest;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceEntity;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceJpaRepository;
import io.mywallet.order.infrastructure.persistence.OrderProjectionJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionJpaRepository;
import io.mywallet.strategy.domain.RiskLevel;
import io.mywallet.strategy.domain.StrategyMode;
import io.mywallet.strategy.infrastructure.persistence.StrategyEntity;
import io.mywallet.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class StrategyExecutionSchedulerIT extends PostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AssetJpaRepository assetRepository;
    @Autowired private MarketPriceJpaRepository marketPriceRepository;
    @Autowired private AccountApplicationService accountApplicationService;
    @Autowired private StrategyApplicationService strategyApplicationService;
    @Autowired private StrategyExecutionScheduler scheduler;
    @Autowired private LeaderboardService leaderboardService;
    @Autowired private OrderProjectionJpaRepository orderProjectionRepository;
    @Autowired private PortfolioPositionJpaRepository positionRepository;
    @Autowired private io.mywallet.portfolio.application.PortfolioApplicationService portfolioApplicationService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void anActiveStrategyAutomaticallyEntersOnASignalAndDoesNotDoubleEnterOnSubsequentTicks() throws Exception {
        MockMvc mvc = mockMvc();
        String email = "autobot+" + System.nanoTime() + "@mywallet.dev";
        String response = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest(email, "a-strong-password-123"))))
            .andReturn().getResponse().getContentAsString();
        UUID userId = UUID.fromString(userIdFromToken(response));

        AssetEntity asset = assetRepository.save(new AssetEntity(
            UUID.randomUUID(), "AUTO" + System.nanoTime(), AssetEntity.AssetClass.CRYPTO, "USD",
            "Auto-trading test asset", new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("0.2"), 1L
        ));
        marketPriceRepository.save(new MarketPriceEntity(UUID.randomUUID(), asset.getId(), new BigDecimal("100.00"), Instant.now()));

        AccountEntity account = accountApplicationService.createAccount(userId, AccountEntity.AccountType.SIMULATED, "Autobot account");
        UUID portfolioId = portfolioApplicationService.createPortfolio(
            account.getId(), "Autobot portfolio", io.mywallet.portfolio.domain.model.PortfolioMode.SIMULATED,
            new BigDecimal("10000.00"), userId, UUID.randomUUID()
        );

        // SMA(symbol, 1) > 0 is always true given any positive price and exactly one data point.
        StrategyEntity strategy = strategyApplicationService.createDraft(
            userId, portfolioId, "Always-long test bot", null, StrategyMode.LIVE_SIMULATION, RiskLevel.LOW,
            null, null, "SMA(" + asset.getSymbol() + ", 1) > 0"
        );
        strategyApplicationService.activate(strategy.getId(), userId);

        scheduler.evaluateActiveStrategies();

        var ordersAfterFirstTick = orderProjectionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId);
        assertThat(ordersAfterFirstTick).hasSize(1);
        assertThat(ordersAfterFirstTick.get(0).getStatus()).isEqualTo("FILLED");
        assertThat(ordersAfterFirstTick.get(0).getSide()).isEqualTo("BUY");

        var positions = positionRepository.findByPortfolioId(portfolioId);
        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).getQuantity()).isGreaterThan(BigDecimal.ZERO);

        // Second tick: signal is still true, already long -> no new order (no double entry).
        scheduler.evaluateActiveStrategies();
        var ordersAfterSecondTick = orderProjectionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId);
        assertThat(ordersAfterSecondTick).hasSize(1); // unchanged

        // Leaderboard: baseline was captured at activation (before the trade), current
        // value right after an all-in buy at the same reference price should be ~unchanged.
        var leaderboard = leaderboardService.snapshot();
        var entry = leaderboard.stream().filter(e -> e.strategyId().equals(strategy.getId())).findFirst().orElseThrow();
        assertThat(entry.returnFraction().doubleValue()).isCloseTo(0.0, within(0.01));
    }

    private String userIdFromToken(String registerResponseBody) throws Exception {
        // Decode the JWT payload just enough to read the "sub" claim, without validating
        // it - this is test-only convenience, never how the app itself trusts a token.
        String accessToken = objectMapper.readTree(registerResponseBody).get("accessToken").asText();
        String payloadBase64 = accessToken.split("\\.")[1];
        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(payloadBase64));
        return objectMapper.readTree(payloadJson).get("sub").asText();
    }
}
