package io.mywallet.portfolio.infrastructure.messaging;

import io.mywallet.eventstore.idempotency.IdempotencyGuard;
import io.mywallet.infrastructure.messaging.RabbitMqConfig;
import io.mywallet.infrastructure.observability.MyWalletMetrics;
import io.mywallet.order.domain.event.OrderFilled;
import io.mywallet.order.domain.event.OrderPartiallyFilled;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Reacts to {@code OrderFilled} / {@code OrderPartiallyFilled} - the asynchronous half of
 * order handling (see {@code docs/architecture/sequence-partial-execution.md}): updates
 * the position's quantity and weighted-average acquisition price, and debits/credits the
 * portfolio's cash balance.
 *
 * <p>Content-based routing on a single queue via {@code @RabbitHandler}: both event types
 * arrive on {@code portfolio.position-updates} (see {@code RabbitMqConfig}), and Spring
 * AMQP dispatches to whichever {@code @RabbitHandler} method's parameter type matches the
 * deserialized payload.</p>
 *
 * <p><strong>Order side is not on the event</strong> ({@code OrderFilled} doesn't carry
 * BUY/SELL) - the projector re-derives it from {@code order_projections}, which is safe
 * because a projection row for the order is guaranteed to already exist by the time a
 * fill event exists (created synchronously in {@code CreateOrderService} before any order
 * can be filled).</p>
 */
@Component
@RabbitListener(queues = RabbitMqConfig.POSITION_UPDATES_QUEUE)
public class PositionUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(PositionUpdateListener.class);
    private static final String PROJECTOR_NAME = "position-projector";

    private final IdempotencyGuard idempotencyGuard;
    private final PortfolioPositionJpaRepository positionRepository;
    private final PortfolioProjectionJpaRepository portfolioRepository;
    private final io.mywallet.order.infrastructure.persistence.OrderProjectionJpaRepository orderProjectionRepository;
    private final MyWalletMetrics metrics;

    public PositionUpdateListener(
        IdempotencyGuard idempotencyGuard,
        PortfolioPositionJpaRepository positionRepository,
        PortfolioProjectionJpaRepository portfolioRepository,
        io.mywallet.order.infrastructure.persistence.OrderProjectionJpaRepository orderProjectionRepository,
        MyWalletMetrics metrics
    ) {
        this.idempotencyGuard = idempotencyGuard;
        this.positionRepository = positionRepository;
        this.portfolioRepository = portfolioRepository;
        this.orderProjectionRepository = orderProjectionRepository;
        this.metrics = metrics;
    }

    @RabbitHandler
    @Transactional
    public void on(OrderFilled event) {
        applyFill(event.eventId(), event.aggregateId(), event.filledQuantity(), event.executionPrice(), event.fees());
    }

    @RabbitHandler
    @Transactional
    public void on(OrderPartiallyFilled event) {
        applyFill(event.eventId(), event.aggregateId(), event.filledQuantity(), event.executionPrice(), event.fees());
    }

    private void applyFill(UUID eventId, UUID orderId, BigDecimal fillQuantity, BigDecimal executionPrice, BigDecimal fees) {
        if (!idempotencyGuard.tryClaim(eventId, PROJECTOR_NAME)) {
            metrics.recordPositionUpdateDuplicateSkipped();
            log.info("Duplicate delivery of event {} for {} ignored", eventId, PROJECTOR_NAME);
            return;
        }

        var orderProjection = orderProjectionRepository.findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order projection not found for fill event: " + orderId));

        UUID portfolioId = orderProjection.getPortfolioId();
        UUID assetId = orderProjection.getAssetId();
        boolean isBuy = "BUY".equals(orderProjection.getSide());

        updatePosition(portfolioId, assetId, isBuy, fillQuantity, executionPrice);
        updateCashBalance(portfolioId, isBuy, fillQuantity, executionPrice, fees);
        metrics.recordPositionUpdateEventProcessed();
    }

    private void updatePosition(UUID portfolioId, UUID assetId, boolean isBuy, BigDecimal fillQuantity, BigDecimal executionPrice) {
        PortfolioPositionEntity existing = positionRepository.findByPortfolioId(portfolioId).stream()
            .filter(p -> p.getAssetId().equals(assetId))
            .findFirst()
            .orElse(null);

        if (existing == null) {
            if (!isBuy) {
                // Selling an asset with no existing position would go negative - in a
                // full system this is exactly what RiskEngine's Phase 7 "quantité
                // disponible à la vente" check prevents before the order is even
                // accepted; here we defensively skip rather than create a nonsensical
                // negative position, and log loudly since it means an earlier check failed.
                log.error("SELL fill for portfolio {} asset {} with no existing position - skipping position update", portfolioId, assetId);
                return;
            }
            positionRepository.save(new PortfolioPositionEntity(
                UUID.randomUUID(), portfolioId, assetId, fillQuantity, executionPrice));
            return;
        }

        BigDecimal newQuantity = isBuy
            ? existing.getQuantity().add(fillQuantity)
            : existing.getQuantity().subtract(fillQuantity);

        BigDecimal newAveragePrice = isBuy
            ? weightedAveragePrice(existing.getQuantity(), existing.getAverageAcquisitionPrice(), fillQuantity, executionPrice)
            : existing.getAverageAcquisitionPrice(); // selling doesn't change the average cost basis of what remains

        positionRepository.save(new PortfolioPositionEntity(existing.getId(), portfolioId, assetId, newQuantity, newAveragePrice));
    }

    private BigDecimal weightedAveragePrice(BigDecimal existingQty, BigDecimal existingAvgPrice,
                                             BigDecimal newQty, BigDecimal newPrice) {
        BigDecimal existingCost = existingQty.multiply(existingAvgPrice);
        BigDecimal newCost = newQty.multiply(newPrice);
        BigDecimal totalQty = existingQty.add(newQty);
        if (totalQty.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return existingCost.add(newCost).divide(totalQty, 8, RoundingMode.HALF_UP);
    }

    private void updateCashBalance(UUID portfolioId, boolean isBuy, BigDecimal fillQuantity, BigDecimal executionPrice, BigDecimal fees) {
        PortfolioProjectionEntity portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new NoSuchElementException("Portfolio not found: " + portfolioId));

        BigDecimal grossAmount = fillQuantity.multiply(executionPrice);
        BigDecimal cashDelta = isBuy
            ? grossAmount.add(fees).negate()  // buying debits cash (cost + fees)
            : grossAmount.subtract(fees);     // selling credits cash (proceeds - fees)

        BigDecimal newBalance = portfolio.getCashBalance().add(cashDelta);
        portfolio.updateCashBalance(newBalance, portfolio.getVersion());
        portfolioRepository.save(portfolio);
    }
}
