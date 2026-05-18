package io.mywallet.portfolio.domain.model;

import io.mywallet.portfolio.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortfolioTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Test
    void creatingAPortfolioRaisesExactlyOnePortfolioCreatedEvent() {
        Portfolio portfolio = Portfolio.create(
            ACCOUNT_ID, "Main simulated portfolio", PortfolioMode.SIMULATED,
            new BigDecimal("10000.00"), UUID.randomUUID(), ACTOR_ID
        );

        assertThat(portfolio.getUncommittedEvents()).hasSize(1);
        assertThat(portfolio.name()).isEqualTo("Main simulated portfolio");
        assertThat(portfolio.mode()).isEqualTo(PortfolioMode.SIMULATED);
        assertThat(portfolio.cashBalance()).isEqualByComparingTo("10000.00");
        assertThat(portfolio.getVersion()).isEqualTo(1L);
    }

    @Test
    void rejectsNegativeInitialCashBalance() {
        assertThatThrownBy(() -> Portfolio.create(
            ACCOUNT_ID, "Bad portfolio", PortfolioMode.DEMO,
            new BigDecimal("-1.00"), UUID.randomUUID(), ACTOR_ID
        )).isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Portfolio.create(
            ACCOUNT_ID, "  ", PortfolioMode.DEMO,
            BigDecimal.TEN, UUID.randomUUID(), ACTOR_ID
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reconstructingFromHistoryProducesTheSameStateAsTheLiveAggregate() {
        Portfolio live = Portfolio.create(
            ACCOUNT_ID, "Reconstructed", PortfolioMode.REAL,
            new BigDecimal("500.00"), UUID.randomUUID(), ACTOR_ID
        );

        Portfolio reconstructed = Portfolio.reconstruct(live.getId(), live.getUncommittedEvents());

        assertThat(reconstructed.name()).isEqualTo(live.name());
        assertThat(reconstructed.mode()).isEqualTo(live.mode());
        assertThat(reconstructed.cashBalance()).isEqualByComparingTo(live.cashBalance());
        assertThat(reconstructed.getVersion()).isEqualTo(live.getVersion());
    }
}
