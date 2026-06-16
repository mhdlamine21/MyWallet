package io.mywallet.execution.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "execution_idempotency")
public class ExecutionIdempotencyEntity {

    @Id
    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected ExecutionIdempotencyEntity() {
        // JPA
    }

    public ExecutionIdempotencyEntity(String externalReference, UUID orderId) {
        this.externalReference = externalReference;
        this.orderId = orderId;
        this.receivedAt = Instant.now();
    }
}
