package io.mywallet.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One topic exchange for every domain event MyWallet publishes, routed by a key derived
 * from the event ({@code <aggregateType>.<eventType>} in snake-ish form, e.g.
 * {@code order.filled}). Each consumer declares its own queue bound to the routing
 * patterns it cares about - see {@code PositionUpdateListener} for the first one.
 *
 * <p>Per ADR-0002, RabbitMQ carries <em>already-persisted</em> events for asynchronous
 * fan-out; it is never the source of truth (that's {@code domain_events}), so losing a
 * queue's contents would be an availability problem, not a data-loss one - a projector can
 * always be replayed from the event store if needed.</p>
 */
@Configuration
public class RabbitMqConfig {

    public static final String DOMAIN_EVENTS_EXCHANGE = "mywallet.events";

    // Position projector: reacts to order fills to update portfolio positions & cash
    public static final String POSITION_UPDATES_QUEUE = "portfolio.position-updates";
    public static final String ORDER_FILLED_ROUTING_KEY = "order.filled";
    public static final String ORDER_PARTIALLY_FILLED_ROUTING_KEY = "order.partially_filled";

    @Bean
    public TopicExchange domainEventsExchange() {
        return new TopicExchange(DOMAIN_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue positionUpdatesQueue() {
        // Durable, not auto-delete: survives broker restarts, and a stopped consumer
        // doesn't lose queued position updates.
        return QueueBuilder.durable(POSITION_UPDATES_QUEUE).build();
    }

    @Bean
    public Binding positionUpdatesFilledBinding(Queue positionUpdatesQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(positionUpdatesQueue).to(domainEventsExchange).with(ORDER_FILLED_ROUTING_KEY);
    }

    @Bean
    public Binding positionUpdatesPartiallyFilledBinding(Queue positionUpdatesQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(positionUpdatesQueue).to(domainEventsExchange).with(ORDER_PARTIALLY_FILLED_ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.support.converter.DefaultClassMapper rabbitClassMapper() {
        var classMapper = new org.springframework.amqp.support.converter.DefaultClassMapper();
        // Explicit allow-list: never trust an arbitrary __TypeId__ header to instantiate
        // classes from any package - that's a deserialization-gadget attack surface. Only
        // this project's own event records may be resolved this way.
        classMapper.setTrustedPackages(
            "io.mywallet.order.domain.event",
            "io.mywallet.portfolio.domain.event"
        );
        return classMapper;
    }

    @Bean
    public MessageConverter jsonMessageConverter(
        com.fasterxml.jackson.databind.ObjectMapper objectMapper,
        org.springframework.amqp.support.converter.DefaultClassMapper rabbitClassMapper
    ) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setClassMapper(rabbitClassMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
