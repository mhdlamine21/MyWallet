package io.mywallet.order.infrastructure.persistence;

import io.mywallet.common.domain.DomainEvent;
import io.mywallet.common.domain.EventStoreRepository;
import io.mywallet.common.exception.OptimisticConcurrencyException;
import io.mywallet.eventstore.persistence.DomainEventEntity;
import io.mywallet.eventstore.persistence.DomainEventJpaRepository;
import io.mywallet.eventstore.serialization.JacksonDomainEventSerializer;
import io.mywallet.order.domain.model.Order;
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
 * JPA-backed adapter for the {@link EventStoreRepository} port, specialized for
 * {@link Order}. See {@code docs/architecture/adr/0003-postgres-event-store.md} for why
 * this is a plain table rather than a dedicated event store product, and
 * {@code EventStoreRepository}'s javadoc for the port contract this class must honor.
 */
@Repository
public class OrderEventStoreRepository implements EventStoreRepository<Order> {

    private static final String AGGREGATE_TYPE = "Order";

    private final DomainEventJpaRepository jpaRepository;
    private final JacksonDomainEventSerializer serializer;
    private final OrderEventTypeRegistry typeRegistry;

    public OrderEventStoreRepository(
        DomainEventJpaRepository jpaRepository,
        JacksonDomainEventSerializer serializer,
        OrderEventTypeRegistry typeRegistry
    ) {
        this.jpaRepository = jpaRepository;
        this.serializer = serializer;
        this.typeRegistry = typeRegistry;
    }

    @Override
    @Transactional
    public void append(Order aggregate, long expectedVersion) {
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
            // The unique (aggregate_id, event_version) index caught a concurrent append
            // that the version check above missed due to a race between the two checks.
            long actualVersion = jpaRepository.findCurrentVersion(aggregate.getId());
            throw new OptimisticConcurrencyException(aggregate.getId(), expectedVersion, actualVersion);
        }

        aggregate.markEventsAsCommitted();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> load(UUID aggregateId, Function<UUID, Order> emptyAggregateFactory) {
        List<DomainEvent> history = loadHistory(aggregateId);
        if (history.isEmpty()) {
            return Optional.empty();
        }
        Order shell = emptyAggregateFactory.apply(aggregateId);
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
            event.eventVersion(), // schema version, distinct from streamPosition - see ADR-0004 discussion
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
