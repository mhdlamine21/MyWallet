package io.mywallet.backtest.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BacktestResultJpaRepository extends JpaRepository<BacktestResultEntity, UUID> {
    Optional<BacktestResultEntity> findByBacktestId(UUID backtestId);
}
