package io.mywallet.risk.application;

import io.mywallet.account.application.AccountApplicationService;
import io.mywallet.account.infrastructure.persistence.AccountEntity;
import io.mywallet.order.infrastructure.persistence.OrderProjectionEntity;
import io.mywallet.order.infrastructure.persistence.OrderProjectionJpaRepository;
import io.mywallet.portfolio.application.PortfolioApplicationService;
import io.mywallet.portfolio.domain.model.PortfolioMode;
import io.mywallet.risk.infrastructure.persistence.RiskAlertJpaRepository;
import io.mywallet.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviorAnomalyDetectionServiceIT extends PostgresIntegrationTest {

    @Autowired private AccountApplicationService accountApplicationService;
    @Autowired private PortfolioApplicationService portfolioApplicationService;
    @Autowired private OrderProjectionJpaRepository orderProjectionRepository;
    @Autowired private RiskAlertJpaRepository riskAlertRepository;
    @Autowired private BehaviorAnomalyDetectionService behaviorAnomalyDetectionService;

    @Test
    void flagsThePortfolioWithFarMoreOrdersTodayThanTheRestOfThePopulation() {
        UUID normalPortfolio1 = createPortfolio();
        UUID normalPortfolio2 = createPortfolio();
        UUID normalPortfolio3 = createPortfolio();
        UUID outlierPortfolio = createPortfolio();

        seedOrdersToday(normalPortfolio1, 1);
        seedOrdersToday(normalPortfolio2, 2);
        seedOrdersToday(normalPortfolio3, 1);
        seedOrdersToday(outlierPortfolio, 80); // wildly more active than everyone else

        behaviorAnomalyDetectionService.detectAnomalousPortfolios();

        var outlierAlerts = riskAlertRepository.findByPortfolioIdOrderByRaisedAtDesc(outlierPortfolio);
        assertThat(outlierAlerts).isNotEmpty();
        assertThat(outlierAlerts.get(0).getLimitType()).isEqualTo("ANOMALOUS_BEHAVIOR");

        assertThat(riskAlertRepository.findByPortfolioIdOrderByRaisedAtDesc(normalPortfolio1)).isEmpty();
        assertThat(riskAlertRepository.findByPortfolioIdOrderByRaisedAtDesc(normalPortfolio2)).isEmpty();
        assertThat(riskAlertRepository.findByPortfolioIdOrderByRaisedAtDesc(normalPortfolio3)).isEmpty();
    }

    /**
     * Note: a second test asserting "does nothing below MINIMUM_POPULATION" was
     * deliberately not added here - state accumulates across test methods sharing this
     * class's Testcontainer (see {@code PostgresIntegrationTest}), so a later test method
     * cannot reliably observe a small population once an earlier method has already
     * created portfolios. The size-guard itself is a simple, low-risk early return; the
     * detection logic it guards is what the test above actually exercises.
     */

    private UUID createPortfolio() {
        UUID ownerId = UUID.randomUUID(); // no real user needed - account ownership isn't checked by this service
        AccountEntity account = accountApplicationService.createAccount(ownerId, AccountEntity.AccountType.SIMULATED, "Behavior test account");
        return portfolioApplicationService.createPortfolio(
            account.getId(), "Behavior test portfolio", PortfolioMode.SIMULATED,
            new BigDecimal("10000.00"), ownerId, UUID.randomUUID()
        );
    }

    private void seedOrdersToday(UUID portfolioId, int count) {
        for (int i = 0; i < count; i++) {
            orderProjectionRepository.save(new OrderProjectionEntity(
                UUID.randomUUID(), portfolioId, UUID.randomUUID(), "MARKET", "BUY",
                BigDecimal.ONE, null, BigDecimal.ONE, "FILLED", 1L, Instant.now()
            ));
        }
    }
}
