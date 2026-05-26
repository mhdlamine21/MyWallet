package io.mywallet.portfolio.application;

import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Shared "what is this portfolio worth right now" calculation - originally written inline
 * inside {@code RiskAssessmentService} (Phase 7), extracted here once
 * {@code StrategyApplicationService} (baseline capture on activation) needed the same
 * logic and a third copy would have been one too many.
 *
 * <p>Simplification carried over unchanged from where it first appeared: position value
 * uses each position's own recorded average acquisition price as a stand-in for current
 * market value, rather than fetching each position's asset's latest price individually. A
 * true mark-to-market valuation belongs in Phase 8's performance calculations, not here.</p>
 */
@Service
public class PortfolioValuationService {

    private final PortfolioPositionJpaRepository positionRepository;

    public PortfolioValuationService(PortfolioPositionJpaRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public BigDecimal currentValue(PortfolioProjectionEntity portfolio) {
        return portfolio.getCashBalance().add(totalPositionsValue(portfolio.getId()));
    }

    public BigDecimal totalPositionsValue(UUID portfolioId) {
        List<PortfolioPositionEntity> positions = positionRepository.findByPortfolioId(portfolioId);
        BigDecimal total = BigDecimal.ZERO;
        for (PortfolioPositionEntity position : positions) {
            total = total.add(position.getQuantity().multiply(position.getAverageAcquisitionPrice()));
        }
        return total;
    }
}
