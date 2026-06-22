package io.mywallet.strategy.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StrategyPerformanceBaselineJpaRepository extends JpaRepository<StrategyPerformanceBaselineEntity, UUID> {
    List<StrategyPerformanceBaselineEntity> findAll();
}
