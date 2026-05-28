package io.mywallet.order.interfaces.rest;

import io.mywallet.account.infrastructure.persistence.AccountEntity;
import io.mywallet.account.infrastructure.persistence.AccountJpaRepository;
import io.mywallet.common.domain.EventStoreRepository;
import io.mywallet.execution.application.RecordExecutionService;
import io.mywallet.order.application.CancelOrderService;
import io.mywallet.order.application.CreateOrderService;
import io.mywallet.order.domain.model.Order;
import io.mywallet.order.infrastructure.persistence.OrderProjectionEntity;
import io.mywallet.order.infrastructure.persistence.OrderProjectionJpaRepository;
import io.mywallet.order.interfaces.rest.dto.*;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders")
public class OrderController {

    private final CreateOrderService createOrderService;
    private final CancelOrderService cancelOrderService;
    private final RecordExecutionService recordExecutionService;
    private final OrderProjectionJpaRepository orderProjectionRepository;
    private final PortfolioProjectionJpaRepository portfolioProjectionRepository;
    private final AccountJpaRepository accountRepository;
    private final EventStoreRepository<Order> eventStoreRepository;

    public OrderController(
        CreateOrderService createOrderService,
        CancelOrderService cancelOrderService,
        RecordExecutionService recordExecutionService,
        OrderProjectionJpaRepository orderProjectionRepository,
        PortfolioProjectionJpaRepository portfolioProjectionRepository,
        AccountJpaRepository accountRepository,
        EventStoreRepository<Order> eventStoreRepository
    ) {
        this.createOrderService = createOrderService;
        this.cancelOrderService = cancelOrderService;
        this.recordExecutionService = recordExecutionService;
        this.orderProjectionRepository = orderProjectionRepository;
        this.portfolioProjectionRepository = portfolioProjectionRepository;
        this.accountRepository = accountRepository;
        this.eventStoreRepository = eventStoreRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new order (synchronous: validated and accepted, or rejected, immediately)")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        UUID userId = userId(authentication);
        UUID correlationId = UUID.randomUUID();

        UUID orderId = createOrderService.createOrder(
            request.portfolioId(), request.assetId(), request.orderType(), request.side(),
            request.quantity(), request.limitPrice(), userId, correlationId
        );

        OrderProjectionEntity projection = getOwnedOrderProjection(orderId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(projection));
    }

    @GetMapping
    @Operation(summary = "List orders for one of the caller's portfolios")
    public List<OrderResponse> list(@RequestParam UUID portfolioId, Authentication authentication) {
        assertPortfolioOwnership(portfolioId, userId(authentication));
        return orderProjectionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId).stream()
            .map(OrderResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id, Authentication authentication) {
        return OrderResponse.from(getOwnedOrderProjection(id, userId(authentication)));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Full event history for this order, in order - the raw event-sourcing record")
    public List<OrderEventResponse> events(@PathVariable UUID id, Authentication authentication) {
        getOwnedOrderProjection(id, userId(authentication)); // enforces ownership, throws if not found/owned
        return eventStoreRepository.loadHistory(id).stream()
            .map(OrderEventResponse::from)
            .toList();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID id, @RequestBody(required = false) CancelOrderRequest request,
                                        Authentication authentication) {
        String reason = request != null && request.reason() != null ? request.reason() : "Cancelled by user";
        cancelOrderService.cancelOrder(id, reason, userId(authentication), UUID.randomUUID());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/executions")
    @Operation(summary = "Simulate an execution being reported for this order (stand-in for a real broker feed - idempotent on externalReference)")
    public ResponseEntity<Void> simulateExecution(@PathVariable UUID id, @Valid @RequestBody SimulateExecutionRequest request,
                                                   Authentication authentication) {
        UUID userId = userId(authentication);
        getOwnedOrderProjection(id, userId); // enforces ownership before allowing a simulated fill

        recordExecutionService.recordExecution(
            id, request.quantity(), request.executionPrice(), request.fees(),
            request.externalReference(), UUID.randomUUID(), userId
        );
        return ResponseEntity.accepted().build();
    }

    private OrderProjectionEntity getOwnedOrderProjection(UUID orderId, UUID requestingUserId) {
        OrderProjectionEntity projection = orderProjectionRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        assertPortfolioOwnership(projection.getPortfolioId(), requestingUserId);
        return projection;
    }

    private void assertPortfolioOwnership(UUID portfolioId, UUID requestingUserId) {
        var portfolio = portfolioProjectionRepository.findById(portfolioId)
            .orElseThrow(() -> new NoSuchElementException("Portfolio not found: " + portfolioId));

        AccountEntity account = accountRepository.findById(portfolio.getAccountId())
            .orElseThrow(() -> new NoSuchElementException("Portfolio not found: " + portfolioId));

        if (!account.getOwnerId().equals(requestingUserId)) {
            throw new NoSuchElementException("Portfolio not found: " + portfolioId);
        }
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
