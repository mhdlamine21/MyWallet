package io.mywallet.risk.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RiskLimitJpaRepository extends JpaRepository<RiskLimitEntity, UUID> {
    List<RiskLimitEntity> findByPortfolioId(UUID portfolioId);
}
