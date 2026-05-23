package io.mywallet.order.application;

import io.mywallet.account.infrastructure.persistence.AccountEntity;
import io.mywallet.account.infrastructure.persistence.AccountJpaRepository;
import io.mywallet.common.domain.EventStoreRepository;
import io.mywallet.infrastructure.messaging.DomainEventPublisher;
import io.mywallet.infrastructure.websocket.WebSocketBroadcaster;
import io.mywallet.order.domain.model.Order;
import io.mywallet.order.infrastructure.persistence.OrderProjectionEntity;
import io.mywallet.order.infrastructure.persistence.OrderProjectionJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class CancelOrderService {

    private final EventStoreRepository<Order> eventStoreRepository;
    private final OrderProjectionJpaRepository orderProjectionRepository;
    private final PortfolioProjectionJpaRepository portfolioProjectionRepository;
    private final AccountJpaRepository accountRepository;
    private final DomainEventPublisher eventPublisher;
    private final WebSocketBroadcaster broadcaster;

    public CancelOrderService(
        EventStoreRepository<Order> eventStoreRepository,
        OrderProjectionJpaRepository orderProjectionRepository,
        PortfolioProjectionJpaRepository portfolioProjectionRepository,
        AccountJpaRepository accountRepository,
        DomainEventPublisher eventPublisher,
        WebSocketBroadcaster broadcaster
    ) {
        this.eventStoreRepository = eventStoreRepository;
        this.orderProjectionRepository = orderProjectionRepository;
        this.portfolioProjectionRepository = portfolioProjectionRepository;
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
        this.broadcaster = broadcaster;
    }

    @Transactional
    public void cancelOrder(UUID orderId, String reason, UUID requestingUserId, UUID correlationId) {
        OrderProjectionEntity projection = orderProjectionRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        assertOwnership(projection, requestingUserId);

        Order order = eventStoreRepository.load(orderId, Order::emptyShell)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        long versionBeforeCancel = order.getVersion();

        order.cancel(reason, correlationId, null, requestingUserId);
        var cancelledEvent = order.getUncommittedEvents().get(0); // capture before append() clears the list

        eventStoreRepository.append(order, versionBeforeCancel);

        projection.applyCancellation(order.getVersion());
        orderProjectionRepository.save(projection);
        broadcaster.broadcastOrderStatus(orderId, projection.getPortfolioId(), "CANCELLED", order.filledQuantity().toString());

        eventPublisher.publish(cancelledEvent, "order.cancelled");
    }

    /**
     * Same "404, not 403" isolation principle used everywhere else in the codebase: a
     * user probing another user's order id learns nothing more than "not found".
     */
    private void assertOwnership(OrderProjectionEntity orderProjection, UUID requestingUserId) {
        PortfolioProjectionEntity portfolio = portfolioProjectionRepository.findById(orderProjection.getPortfolioId())
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderProjection.getId()));

        AccountEntity account = accountRepository.findById(portfolio.getAccountId())
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderProjection.getId()));

        if (!account.getOwnerId().equals(requestingUserId)) {
            throw new NoSuchElementException("Order not found: " + orderProjection.getId());
        }
    }
}
