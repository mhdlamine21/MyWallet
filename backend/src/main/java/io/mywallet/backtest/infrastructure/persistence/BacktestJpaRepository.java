package io.mywallet.backtest.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BacktestJpaRepository extends JpaRepository<BacktestEntity, UUID> {
    List<BacktestEntity> findByStrategyIdOrderByCreatedAtDesc(UUID strategyId);
}
