package io.mywallet.execution.application;

import io.mywallet.common.domain.EventStoreRepository;
import io.mywallet.execution.infrastructure.persistence.ExecutionIdempotencyGuard;
import io.mywallet.infrastructure.messaging.DomainEventPublisher;
import io.mywallet.infrastructure.messaging.RabbitMqConfig;
import io.mywallet.infrastructure.websocket.WebSocketBroadcaster;
import io.mywallet.order.domain.model.Order;
import io.mywallet.order.domain.model.OrderStatus;
import io.mywallet.order.infrastructure.persistence.OrderProjectionEntity;
import io.mywallet.order.infrastructure.persistence.OrderProjectionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Stands in for a real broker's execution feed (see the project's "simulated execution
 * platform" scope - no real broker is ever connected). Idempotent on
 * {@code externalReference} via {@link ExecutionIdempotencyGuard}: the same execution
 * reported twice (a common at-least-once delivery scenario, and explicitly test scenario
 * #14 - "double réception du même événement" - in the project plan) is a safe no-op the
 * second time.
 */
@Service
public class RecordExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RecordExecutionService.class);

    private final EventStoreRepository<Order> eventStoreRepository;
    private final OrderProjectionJpaRepository orderProjectionRepository;
    private final ExecutionIdempotencyGuard idempotencyGuard;
    private final DomainEventPublisher eventPublisher;
    private final WebSocketBroadcaster broadcaster;

    public RecordExecutionService(
        EventStoreRepository<Order> eventStoreRepository,
        OrderProjectionJpaRepository orderProjectionRepository,
        ExecutionIdempotencyGuard idempotencyGuard,
        DomainEventPublisher eventPublisher,
        WebSocketBroadcaster broadcaster
    ) {
        this.eventStoreRepository = eventStoreRepository;
        this.orderProjectionRepository = orderProjectionRepository;
        this.idempotencyGuard = idempotencyGuard;
        this.eventPublisher = eventPublisher;
        this.broadcaster = broadcaster;
    }

    @Transactional
    public void recordExecution(
        UUID orderId, BigDecimal quantity, BigDecimal executionPrice, BigDecimal fees,
        String externalReference, UUID correlationId, UUID actorId
    ) {
        if (!idempotencyGuard.tryClaim(externalReference, orderId)) {
            log.info("Duplicate execution delivery ignored (externalReference={}, orderId={})", externalReference, orderId);
            return;
        }

        Order order = eventStoreRepository.load(orderId, Order::emptyShell)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        long versionBeforeFill = order.getVersion();

        UUID executionId = UUID.randomUUID();
        order.recordPartialFill(executionId, quantity, executionPrice, fees, correlationId, null, actorId);
        var fillEvent = order.getUncommittedEvents().get(0); // capture before append() clears the list

        eventStoreRepository.append(order, versionBeforeFill);

        OrderProjectionEntity projection = orderProjectionRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order projection missing for order: " + orderId));
        String newStatus = order.status() == OrderStatus.FILLED ? "FILLED" : "PARTIALLY_FILLED";
        projection.applyFill(order.filledQuantity(), newStatus, order.getVersion());
        orderProjectionRepository.save(projection);
        broadcaster.broadcastOrderStatus(orderId, projection.getPortfolioId(), newStatus, order.filledQuantity().toString());

        String routingKey = order.status() == OrderStatus.FILLED
            ? RabbitMqConfig.ORDER_FILLED_ROUTING_KEY
            : RabbitMqConfig.ORDER_PARTIALLY_FILLED_ROUTING_KEY;
        eventPublisher.publish(fillEvent, routingKey);
    }
}
