package io.mywallet.strategy.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "strategy_execution_states")
public class StrategyExecutionStateEntity {

    @Id
    @Column(name = "strategy_id")
    private UUID strategyId;

    @Column(name = "currently_long", nullable = false)
    private boolean currentlyLong;

    @Column(name = "last_evaluated_at")
    private Instant lastEvaluatedAt;

    protected StrategyExecutionStateEntity() {
        // JPA
    }

    public StrategyExecutionStateEntity(UUID strategyId) {
        this.strategyId = strategyId;
        this.currentlyLong = false;
    }

    public void update(boolean currentlyLong, Instant evaluatedAt) {
        this.currentlyLong = currentlyLong;
        this.lastEvaluatedAt = evaluatedAt;
    }

    public UUID getStrategyId() { return strategyId; }
    public boolean isCurrentlyLong() { return currentlyLong; }
    public Instant getLastEvaluatedAt() { return lastEvaluatedAt; }
}
