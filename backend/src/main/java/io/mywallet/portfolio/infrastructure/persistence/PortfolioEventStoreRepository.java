package io.mywallet.portfolio.infrastructure.persistence;

import io.mywallet.common.domain.DomainEvent;
import io.mywallet.common.domain.EventStoreRepository;
import io.mywallet.common.exception.OptimisticConcurrencyException;
import io.mywallet.eventstore.persistence.DomainEventEntity;
import io.mywallet.eventstore.persistence.DomainEventJpaRepository;
import io.mywallet.eventstore.serialization.JacksonDomainEventSerializer;
import io.mywallet.portfolio.domain.model.Portfolio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * JPA adapter for the {@link EventStoreRepository} port, specialized for {@link Portfolio}.
 * Structurally identical to {@code OrderEventStoreRepository} - both aggregates share the
 * same {@code domain_events} table (discriminated by {@code aggregate_type}), the same
 * optimistic-concurrency mechanism, and the same serialization approach. The duplication
 * between the two classes is deliberate at this stage rather than prematurely generified:
 * a third aggregate (Strategy, if it becomes event-sourced) would be the trigger to
 * extract a shared abstract base - see the "rule of three" note in
 * {@code docs/architecture/adr/} if this comes up in a future `/improve-architecture` pass.
 */
@Repository
public class PortfolioEventStoreRepository implements EventStoreRepository<Portfolio> {

    private static final String AGGREGATE_TYPE = "Portfolio";

    private final DomainEventJpaRepository jpaRepository;
    private final JacksonDomainEventSerializer serializer;
    private final PortfolioEventTypeRegistry typeRegistry;

    public PortfolioEventStoreRepository(
        DomainEventJpaRepository jpaRepository,
        JacksonDomainEventSerializer serializer,
        PortfolioEventTypeRegistry typeRegistry
    ) {
        this.jpaRepository = jpaRepository;
        this.serializer = serializer;
        this.typeRegistry = typeRegistry;
    }

    @Override
    @Transactional
    public void append(Portfolio aggregate, long expectedVersion) {
        List<DomainEvent> uncommitted = aggregate.getUncommittedEvents();
        if (uncommitted.isEmpty()) {
            return;
        }

        long currentVersion = jpaRepository.findCurrentVersion(aggregate.getId());
        if (currentVersion != expectedVersion) {
            throw new OptimisticConcurrencyException(aggregate.getId(), expectedVersion, currentVersion);
        }

        List<DomainEventEntity> rows = new ArrayList<>(uncommitted.size());
        long version = expectedVersion;
        for (DomainEvent event : uncommitted) {
            version++;
            rows.add(toEntity(event, version));
        }

        try {
            jpaRepository.saveAll(rows);
        } catch (DataIntegrityViolationException raceLostToAnotherWriter) {
            long actualVersion = jpaRepository.findCurrentVersion(aggregate.getId());
            throw new OptimisticConcurrencyException(aggregate.getId(), expectedVersion, actualVersion);
        }

        aggregate.markEventsAsCommitted();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Portfolio> load(UUID aggregateId, Function<UUID, Portfolio> emptyAggregateFactory) {
        List<DomainEvent> history = loadHistory(aggregateId);
        if (history.isEmpty()) {
            return Optional.empty();
        }
        Portfolio shell = emptyAggregateFactory.apply(aggregateId);
        shell.loadFromHistory(history);
        return Optional.of(shell);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainEvent> loadHistory(UUID aggregateId) {
        return jpaRepository.findByAggregateIdOrderByEventVersionAsc(aggregateId).stream()
            .map(this::toDomainEvent)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainEvent> loadHistory(UUID aggregateId, Instant until) {
        return jpaRepository.findByAggregateIdUntil(aggregateId, until).stream()
            .map(this::toDomainEvent)
            .toList();
    }

    private DomainEventEntity toEntity(DomainEvent event, long streamPosition) {
        return new DomainEventEntity(
            event.eventId(),
            event.aggregateId(),
            AGGREGATE_TYPE,
            event.eventType(),
            streamPosition,
            event.eventVersion(),
            serializer.serialize(event),
            event.occurredAt(),
            event.correlationId(),
            event.causationId(),
            event.actorId()
        );
    }

    private DomainEvent toDomainEvent(DomainEventEntity entity) {
        Class<? extends DomainEvent> targetType = typeRegistry.resolve(entity.getEventType());
        return serializer.deserialize(entity.getPayload(), targetType);
    }
}
