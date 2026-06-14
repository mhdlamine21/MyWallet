package io.mywallet.infrastructure.messaging;

import io.mywallet.common.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes an already-persisted {@link DomainEvent} to the domain events exchange, for
 * whichever asynchronous consumers care about it (position updates today; audit,
 * notifications, and risk re-evaluation in later phases - see the project plan's
 * "événements asynchrones" list).
 *
 * <p>Called <strong>after</strong> the event store append succeeds, never before - if
 * publishing fails, the event is still durably persisted and can be replayed by any
 * consumer that re-subscribes or by a future reconciliation job; the reverse (publishing
 * before persisting) would risk a consumer reacting to an event that a subsequent
 * persistence failure then rolls back.</p>
 */
@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public DomainEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(DomainEvent event, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.DOMAIN_EVENTS_EXCHANGE, routingKey, event);
        } catch (Exception e) {
            // Deliberately does not rethrow: the event is already safely in domain_events.
            // A broker outage should not fail the user-facing request that just succeeded.
            // What it *should* do is page someone - this log line is the placeholder for
            // that alert until Phase 10 wires up real alerting on ERROR-level logs.
            log.error("Failed to publish event {} (routingKey={}) to RabbitMQ - event is persisted, " +
                "async projections may lag until this is resolved", event.eventId(), routingKey, e);
        }
    }
}
