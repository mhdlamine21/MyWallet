package io.mywallet.order.application;

import io.mywallet.account.infrastructure.persistence.AccountEntity;
import io.mywallet.account.infrastructure.persistence.AccountJpaRepository;
import io.mywallet.asset.infrastructure.persistence.AssetEntity;
import io.mywallet.asset.infrastructure.persistence.AssetJpaRepository;
import io.mywallet.common.domain.EventStoreRepository;
import io.mywallet.infrastructure.admin.KillSwitchActiveException;
import io.mywallet.infrastructure.admin.KillSwitchService;
import io.mywallet.infrastructure.messaging.DomainEventPublisher;
import io.mywallet.infrastructure.observability.MyWalletMetrics;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceJpaRepository;
import io.mywallet.order.application.exception.UnknownAssetException;
import io.mywallet.order.domain.model.Order;
import io.mywallet.order.domain.model.OrderSide;
import io.mywallet.order.domain.model.OrderType;
import io.mywallet.order.infrastructure.persistence.OrderProjectionEntity;
import io.mywallet.order.infrastructure.persistence.OrderProjectionJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import io.mywallet.risk.application.RiskAssessmentService;
import io.mywallet.risk.application.exception.RiskLimitBreachedException;
import io.mywallet.risk.domain.RiskEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Creates an order: checks the global kill switch, validates the portfolio belongs to the
 * caller, checks the asset exists, runs the full {@link RiskEngine} (via
 * {@link RiskAssessmentService}) - replacing the Phase 5 ad hoc solvency check - persists
 * the {@link Order} aggregate, and synchronously writes its read projection (order
 * creation itself needs to be immediately consistent - see
 * {@code docs/architecture/sequence-order-creation.md}).
 */
@Service
public class CreateOrderService {

    private final EventStoreRepository<Order> eventStoreRepository;
    private final OrderProjectionJpaRepository orderProjectionRepository;
    private final PortfolioProjectionJpaRepository portfolioProjectionRepository;
    private final AccountJpaRepository accountRepository;
    private final AssetJpaRepository assetRepository;
    private final MarketPriceJpaRepository marketPriceRepository;
    private final DomainEventPublisher eventPublisher;
    private final RiskAssessmentService riskAssessmentService;
    private final KillSwitchService killSwitchService;
    private final MyWalletMetrics metrics;

    public CreateOrderService(
        EventStoreRepository<Order> eventStoreRepository,
        OrderProjectionJpaRepository orderProjectionRepository,
        PortfolioProjectionJpaRepository portfolioProjectionRepository,
        AccountJpaRepository accountRepository,
        AssetJpaRepository assetRepository,
        MarketPriceJpaRepository marketPriceRepository,
        DomainEventPublisher eventPublisher,
        RiskAssessmentService riskAssessmentService,
        KillSwitchService killSwitchService,
        MyWalletMetrics metrics
    ) {
        this.eventStoreRepository = eventStoreRepository;
        this.orderProjectionRepository = orderProjectionRepository;
        this.portfolioProjectionRepository = portfolioProjectionRepository;
        this.accountRepository = accountRepository;
        this.assetRepository = assetRepository;
        this.marketPriceRepository = marketPriceRepository;
        this.eventPublisher = eventPublisher;
        this.riskAssessmentService = riskAssessmentService;
        this.killSwitchService = killSwitchService;
        this.metrics = metrics;
    }

    @Transactional
    public UUID createOrder(
        UUID portfolioId, UUID assetId, OrderType orderType, OrderSide side,
        BigDecimal quantity, BigDecimal limitPrice, UUID requestingUserId, UUID correlationId
    ) {
        var timerSample = metrics.startOrderCreationTimer();
        try {
            return doCreateOrder(portfolioId, assetId, orderType, side, quantity, limitPrice, requestingUserId, correlationId);
        } finally {
            metrics.stopOrderCreationTimer(timerSample);
        }
    }

    private UUID doCreateOrder(
        UUID portfolioId, UUID assetId, OrderType orderType, OrderSide side,
        BigDecimal quantity, BigDecimal limitPrice, UUID requestingUserId, UUID correlationId
    ) {
        try {
            killSwitchService.assertTradingAllowed();
        } catch (KillSwitchActiveException e) {
            metrics.recordOrderRejectedByKillSwitch();
            throw e;
        }

        PortfolioProjectionEntity portfolio = portfolioProjectionRepository.findById(portfolioId)
            .orElseThrow(() -> new NoSuchElementException("Portfolio not found: " + portfolioId));

        AccountEntity account = accountRepository.findById(portfolio.getAccountId())
            .orElseThrow(() -> new NoSuchElementException("Portfolio not found: " + portfolioId));
        if (!account.getOwnerId().equals(requestingUserId)) {
            // Same "404, not 403" isolation principle as PortfolioApplicationService.
            throw new NoSuchElementException("Portfolio not found: " + portfolioId);
        }

        AssetEntity asset = assetRepository.findById(assetId)
            .filter(AssetEntity::isEnabled)
            .orElseThrow(() -> new UnknownAssetException("Unknown or disabled asset: " + assetId));

        BigDecimal referencePrice = referencePriceFor(asset, orderType, limitPrice);
        BigDecimal orderValue = referencePrice.multiply(quantity);

        RiskEngine.RiskAssessment assessment = metrics.timeRiskAssessment(() ->
            riskAssessmentService.assess(portfolioId, asset, side, quantity, orderValue, portfolio));
        if (!assessment.accepted()) {
            metrics.recordOrderRejectedByRisk();
            throw new RiskLimitBreachedException(assessment.breaches());
        }

        Order order = Order.create(portfolioId, assetId, orderType, side, quantity, limitPrice, correlationId, requestingUserId);
        var createdEvent = order.getUncommittedEvents().get(0); // capture before append() clears the list
        eventStoreRepository.append(order, 0L);

        orderProjectionRepository.save(new OrderProjectionEntity(
            order.getId(), portfolioId, assetId, orderType.name(), side.name(),
            quantity, limitPrice, BigDecimal.ZERO, "CREATED", order.getVersion(), Instant.now()
        ));

        eventPublisher.publish(createdEvent, "order.created");
        metrics.recordOrderCreated();

        return order.getId();
    }

    private BigDecimal referencePriceFor(AssetEntity asset, OrderType orderType, BigDecimal limitPrice) {
        if (orderType == OrderType.MARKET || limitPrice == null) {
            return marketPriceRepository.findFirstByAssetIdOrderByObservedAtDesc(asset.getId())
                .map(price -> price.getPrice())
                .orElse(asset.getInitialPrice());
        }
        return limitPrice;
    }
}
