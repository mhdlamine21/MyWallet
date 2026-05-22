package io.mywallet.eventstore.idempotency;

import java.util.UUID;

public interface IdempotencyGuard {

    /**
     * @return true if this call is the first to claim this (eventId, projectorName) pair
     *         (caller should proceed), false if already claimed (caller should skip it as
     *         a duplicate delivery).
     */
    boolean tryClaim(UUID eventId, String projectorName);
}
