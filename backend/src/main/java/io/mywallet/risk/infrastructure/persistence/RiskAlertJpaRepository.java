package io.mywallet.risk.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RiskAlertJpaRepository extends JpaRepository<RiskAlertEntity, UUID> {
    List<RiskAlertEntity> findByPortfolioIdOrderByRaisedAtDesc(UUID portfolioId);
    boolean existsByPortfolioIdAndLimitTypeAndRaisedAtAfter(UUID portfolioId, String limitType, Instant after);
}
