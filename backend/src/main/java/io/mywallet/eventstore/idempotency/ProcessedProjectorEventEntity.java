package io.mywallet.eventstore.idempotency;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "processed_projector_events")
public class ProcessedProjectorEventEntity {

    @EmbeddedId
    private Key id;

    protected ProcessedProjectorEventEntity() {
        // JPA
    }

    public ProcessedProjectorEventEntity(UUID eventId, String projectorName) {
        this.id = new Key(eventId, projectorName);
    }

    @jakarta.persistence.Embeddable
    public static class Key implements Serializable {
        private UUID eventId;
        private String projectorName;

        protected Key() {
            // JPA
        }

        public Key(UUID eventId, String projectorName) {
            this.eventId = eventId;
            this.projectorName = projectorName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(eventId, key.eventId) && Objects.equals(projectorName, key.projectorName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, projectorName);
        }
    }
}
