package io.mywallet.risk.application;

import io.mywallet.asset.infrastructure.persistence.AssetEntity;
import io.mywallet.infrastructure.observability.MyWalletMetrics;
import io.mywallet.infrastructure.websocket.WebSocketBroadcaster;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceJpaRepository;
import io.mywallet.order.domain.model.OrderSide;
import io.mywallet.order.infrastructure.persistence.OrderProjectionJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionJpaRepository;
import io.mywallet.portfolio.application.PortfolioValuationService;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import io.mywallet.risk.domain.RiskCheckContext;
import io.mywallet.risk.domain.RiskEngine;
import io.mywallet.risk.domain.RiskLimits;
import io.mywallet.risk.infrastructure.persistence.RiskAlertEntity;
import io.mywallet.risk.infrastructure.persistence.RiskAlertJpaRepository;
import io.mywallet.risk.infrastructure.persistence.RiskLimitEntity;
import io.mywallet.risk.infrastructure.persistence.RiskLimitJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bridges real portfolio/asset/order data into a {@link RiskCheckContext} and runs
 * {@link RiskEngine} against it - this is the application-layer glue the pure domain
 * {@code RiskCheck}s deliberately don't do themselves (no DB access from domain code).
 * Every breach is persisted as a {@link RiskAlertEntity} regardless of whether the order
 * is ultimately rejected, so the risk-alerts history reflects everything the engine ever
 * flagged, not just what got through.
 */
@Service
public class RiskAssessmentService {

    private final RiskLimitJpaRepository riskLimitRepository;
    private final RiskAlertJpaRepository riskAlertRepository;
    private final PortfolioPositionJpaRepository positionRepository;
    private final OrderProjectionJpaRepository orderProjectionRepository;
    private final MarketPriceJpaRepository marketPriceRepository;
    private final PortfolioValuationService portfolioValuationService;
    private final MyWalletMetrics metrics;
    private final WebSocketBroadcaster broadcaster;
    private final RiskEngine riskEngine = RiskEngine.withDefaultChecks();

    public RiskAssessmentService(
        RiskLimitJpaRepository riskLimitRepository,
        RiskAlertJpaRepository riskAlertRepository,
        PortfolioPositionJpaRepository positionRepository,
        OrderProjectionJpaRepository orderProjectionRepository,
        MarketPriceJpaRepository marketPriceRepository,
        PortfolioValuationService portfolioValuationService,
        MyWalletMetrics metrics,
        WebSocketBroadcaster broadcaster
    ) {
        this.riskLimitRepository = riskLimitRepository;
        this.riskAlertRepository = riskAlertRepository;
        this.positionRepository = positionRepository;
        this.orderProjectionRepository = orderProjectionRepository;
        this.marketPriceRepository = marketPriceRepository;
        this.portfolioValuationService = portfolioValuationService;
        this.metrics = metrics;
        this.broadcaster = broadcaster;
    }

    /**
     * @return the assessment; the caller (CreateOrderService) decides what to do with a
     *         rejection - this method's job ends at "assess and record", not "enforce".
     */
    @Transactional
    public RiskEngine.RiskAssessment assess(
        UUID portfolioId, AssetEntity asset, OrderSide side, BigDecimal quantity, BigDecimal orderValue,
        PortfolioProjectionEntity portfolio
    ) {
        RiskLimits limits = loadLimits(portfolioId);

        List<PortfolioPositionEntity> positions = positionRepository.findByPortfolioId(portfolioId);
        PortfolioPositionEntity existingPosition = positions.stream()
            .filter(p -> p.getAssetId().equals(asset.getId()))
            .findFirst()
            .orElse(null);
        BigDecimal existingQuantity = existingPosition != null ? existingPosition.getQuantity() : BigDecimal.ZERO;

        BigDecimal totalPortfolioValue = portfolioValuationService.currentValue(portfolio);
        BigDecimal existingPositionValue = existingQuantity.multiply(currentPrice(asset));

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long ordersToday = orderProjectionRepository.countByPortfolioIdAndCreatedAtAfter(portfolioId, startOfToday);

        RiskCheckContext context = new RiskCheckContext(
            side, quantity, orderValue, portfolio.getCashBalance(),
            existingQuantity, existingPositionValue, totalPortfolioValue, (int) ordersToday, limits
        );

        RiskEngine.RiskAssessment assessment = riskEngine.assess(context);
        recordAlerts(portfolioId, assessment);
        return assessment;
    }

    private RiskLimits loadLimits(UUID portfolioId) {
        Map<String, BigDecimal> configured = riskLimitRepository.findByPortfolioId(portfolioId).stream()
            .collect(java.util.stream.Collectors.toMap(RiskLimitEntity::getLimitType, RiskLimitEntity::getThreshold));

        Integer maxOrdersPerDay = configured.containsKey("MAX_ORDERS_PER_DAY")
            ? configured.get("MAX_ORDERS_PER_DAY").intValue() : null;

        return new RiskLimits(
            configured.get("MAX_ORDER_VALUE"),
            configured.get("MAX_EXPOSURE_PER_ASSET"),
            maxOrdersPerDay
        );
    }

    private BigDecimal currentPrice(AssetEntity asset) {
        return marketPriceRepository.findFirstByAssetIdOrderByObservedAtDesc(asset.getId())
            .map(price -> price.getPrice())
            .orElse(asset.getInitialPrice());
    }

    private void recordAlerts(UUID portfolioId, RiskEngine.RiskAssessment assessment) {
        for (var breach : assessment.breaches()) {
            riskAlertRepository.save(new RiskAlertEntity(
                UUID.randomUUID(), portfolioId, breach.limitType(),
                assessment.level().name(), assessment.riskScore(), breach.explanation()
            ));
            broadcaster.broadcastRiskAlert(portfolioId, breach.limitType(), assessment.level().name(),
                assessment.riskScore(), breach.explanation());
        }
        if (!assessment.breaches().isEmpty()) {
            metrics.recordRiskAlertRaised(assessment.breaches().size());
        }
    }
}
