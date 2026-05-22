package io.mywallet.eventstore.idempotency;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

interface ProcessedProjectorEventJpaRepository
    extends JpaRepository<ProcessedProjectorEventEntity, ProcessedProjectorEventEntity.Key> {
}

/**
 * Reusable idempotency guard for any RabbitMQ event consumer: "have I already applied
 * event X for projector Y?" - insert-first, relying on the composite primary key to
 * reject a duplicate atomically rather than a racy check-then-insert.
 *
 * <p>Used by {@code PositionUpdateListener} today; any future projector (audit,
 * notifications, risk re-evaluation) reuses this same component rather than
 * reimplementing its own idempotency table.</p>
 */
@Component
class IdempotencyGuardImpl implements IdempotencyGuard {

    private final ProcessedProjectorEventJpaRepository repository;

    IdempotencyGuardImpl(ProcessedProjectorEventJpaRepository repository) {
        this.repository = repository;
    }

    /**
     * Attempts to claim this (eventId, projectorName) pair. Returns {@code true} if this
     * call is the first to process it (caller should proceed), {@code false} if it was
     * already processed (caller should skip - this is a duplicate delivery).
     *
     * <p><strong>Propagation.NESTED, deliberately</strong>: PostgreSQL aborts an entire
     * transaction after any failed statement unless the failure happens inside a
     * savepoint - plain {@code try/catch} around this insert would otherwise poison the
     * caller's whole transaction on a duplicate delivery, not just this one insert.
     * {@code NESTED} wraps the claim in a savepoint that rolls back in isolation on
     * conflict, while the claim itself still commits atomically with the caller's business
     * update when both succeed (same underlying transaction, no separate commit).</p>
     */
    @Override
    @Transactional(propagation = Propagation.NESTED)
    public boolean tryClaim(UUID eventId, String projectorName) {
        try {
            repository.saveAndFlush(new ProcessedProjectorEventEntity(eventId, projectorName));
            return true;
        } catch (DataIntegrityViolationException alreadyProcessed) {
            return false;
        }
    }
}
