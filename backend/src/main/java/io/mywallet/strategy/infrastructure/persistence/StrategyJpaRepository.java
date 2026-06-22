package io.mywallet.strategy.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StrategyJpaRepository extends JpaRepository<StrategyEntity, UUID> {
    List<StrategyEntity> findByOwnerId(UUID ownerId);
    List<StrategyEntity> findByStatus(io.mywallet.strategy.domain.StrategyStatus status);
}
