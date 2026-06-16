package io.mywallet.execution.infrastructure.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * A dedicated bean (not a private method on {@code RecordExecutionService}) so that
 * {@code @Transactional(propagation = NESTED)} actually takes effect: Spring's
 * proxy-based AOP does not intercept self-invocation (a method calling another method on
 * {@code this}), so the same claim-then-continue logic implemented as a private method on
 * the caller would silently run without a savepoint and - on PostgreSQL - poison the
 * whole surrounding transaction on a duplicate delivery. Going through this separate
 * bean's proxy is what makes the savepoint real. See the identical reasoning in
 * {@code IdempotencyGuardImpl}.
 */
@Component
public class ExecutionIdempotencyGuard {

    private final ExecutionIdempotencyJpaRepository repository;

    public ExecutionIdempotencyGuard(ExecutionIdempotencyJpaRepository repository) {
        this.repository = repository;
    }

    /** @return true if this call claimed the reference (proceed), false if already claimed (skip, duplicate). */
    @Transactional(propagation = Propagation.NESTED)
    public boolean tryClaim(String externalReference, UUID orderId) {
        try {
            repository.saveAndFlush(new ExecutionIdempotencyEntity(externalReference, orderId));
            return true;
        } catch (DataIntegrityViolationException alreadyClaimed) {
            return false;
        }
    }
}
