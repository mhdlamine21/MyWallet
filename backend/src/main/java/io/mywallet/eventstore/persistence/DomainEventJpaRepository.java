package io.mywallet.eventstore.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DomainEventJpaRepository extends JpaRepository<DomainEventEntity, UUID> {

    List<DomainEventEntity> findByAggregateIdOrderByEventVersionAsc(UUID aggregateId);

    @Query("""
        SELECT e FROM DomainEventEntity e
        WHERE e.aggregateId = :aggregateId AND e.occurredAt <= :until
        ORDER BY e.eventVersion ASC
        """)
    List<DomainEventEntity> findByAggregateIdUntil(@Param("aggregateId") UUID aggregateId,
                                                     @Param("until") Instant until);

    /** Current highest event_version for an aggregate - 0 if the stream doesn't exist yet. */
    @Query("SELECT COALESCE(MAX(e.eventVersion), 0) FROM DomainEventEntity e WHERE e.aggregateId = :aggregateId")
    long findCurrentVersion(@Param("aggregateId") UUID aggregateId);
}
