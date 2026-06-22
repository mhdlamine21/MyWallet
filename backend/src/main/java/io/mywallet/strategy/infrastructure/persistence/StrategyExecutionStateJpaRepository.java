package io.mywallet.strategy.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StrategyExecutionStateJpaRepository extends JpaRepository<StrategyExecutionStateEntity, UUID> {
}
