package io.mywallet.portfolio.application;

import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortfolioValuationServiceTest {

    private final PortfolioPositionJpaRepository positionRepository = mock(PortfolioPositionJpaRepository.class);
    private final PortfolioValuationService service = new PortfolioValuationService(positionRepository);

    @Test
    void currentValueIsCashOnlyWhenThereAreNoPositions() {
        UUID portfolioId = UUID.randomUUID();
        when(positionRepository.findByPortfolioId(portfolioId)).thenReturn(List.of());
        var portfolio = new PortfolioProjectionEntity(portfolioId, UUID.randomUUID(), "P", "DEMO",
            new BigDecimal("1000.00"), 1L, Instant.now());

        assertThat(service.currentValue(portfolio)).isEqualByComparingTo("1000.00");
    }

    @Test
    void currentValueAddsPositionValuesAtTheirAverageAcquisitionPrice() {
        UUID portfolioId = UUID.randomUUID();
        when(positionRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(
            new PortfolioPositionEntity(UUID.randomUUID(), portfolioId, UUID.randomUUID(), new BigDecimal("10"), new BigDecimal("50.00")),
            new PortfolioPositionEntity(UUID.randomUUID(), portfolioId, UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("100.00"))
        ));
        var portfolio = new PortfolioProjectionEntity(portfolioId, UUID.randomUUID(), "P", "DEMO",
            new BigDecimal("1000.00"), 1L, Instant.now());

        // 1000 cash + (10*50) + (2*100) = 1000 + 500 + 200 = 1700
        assertThat(service.currentValue(portfolio)).isEqualByComparingTo("1700.00");
    }
}
