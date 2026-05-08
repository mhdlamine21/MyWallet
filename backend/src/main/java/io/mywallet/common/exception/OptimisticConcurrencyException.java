package io.mywallet.common.exception;

import java.util.UUID;

/**
 * Raised when appending events to an aggregate's stream fails because another writer
 * already advanced the aggregate past the version the caller expected - e.g. two
 * concurrent executions being recorded against the same Order (see the concurrency test
 * scenario in the project's test plan: "Exécution simultanée de deux ordres").
 */
public class OptimisticConcurrencyException extends DomainException {

    public OptimisticConcurrencyException(UUID aggregateId, long expectedVersion, long actualVersion) {
        super("Concurrent modification of aggregate %s: expected version %d, actual version %d"
            .formatted(aggregateId, expectedVersion, actualVersion));
    }

    @Override
    public String errorCode() {
        return "OPTIMISTIC_CONCURRENCY_CONFLICT";
    }
}
