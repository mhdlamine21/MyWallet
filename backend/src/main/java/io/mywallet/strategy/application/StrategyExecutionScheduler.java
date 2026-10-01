package io.mywallet.strategy.application;

import io.mywallet.asset.infrastructure.persistence.AssetEntity;
import io.mywallet.asset.infrastructure.persistence.AssetJpaRepository;
import io.mywallet.execution.application.RecordExecutionService;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceEntity;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceJpaRepository;
import io.mywallet.order.application.CreateOrderService;
import io.mywallet.order.domain.model.OrderSide;
import io.mywallet.order.domain.model.OrderType;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import io.mywallet.ruleengine.domain.analysis.RuleSymbolExtractor;
import io.mywallet.ruleengine.domain.ast.BooleanExpression;
import io.mywallet.ruleengine.domain.evaluator.MarketContext;
import io.mywallet.ruleengine.domain.evaluator.RuleEvaluator;
import io.mywallet.ruleengine.domain.parser.RuleParser;
import io.mywallet.strategy.domain.StrategyStatus;
import io.mywallet.strategy.infrastructure.persistence.StrategyEntity;
import io.mywallet.strategy.infrastructure.persistence.StrategyExecutionStateEntity;
import io.mywallet.strategy.infrastructure.persistence.StrategyExecutionStateJpaRepository;
import io.mywallet.strategy.infrastructure.persistence.StrategyJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * This is what makes an {@code ACTIVE} strategy actually do something - without it,
 * {@code Strategy}'s lifecycle (Phase 6) and the RuleEngine (Phase 6) were both fully
 * built but nothing ever consumed a rule's boolean output. Once per tick: for every
 * {@code ACTIVE} strategy, evaluate its rule against the same simulated price data the
 * live market runs on, and act on a flat->long or long->flat *transition* - not on every
 * tick the signal happens to still read true/false - exactly mirroring
 * {@code BacktestEngine}'s own logic, so a strategy's live behavior matches what backtesting
 * the same rule predicted.
 *
 * <p><strong>Deliberate simplifications, consistent with the rest of this codebase's
 * honesty about scope:</strong></p>
 * <ul>
 *   <li>Single-asset only (via {@link RuleSymbolExtractor#extractSingleSymbol}) - a
 *       strategy naming two symbols is skipped with a warning, not silently mistraded.</li>
 *   <li>Position sizing is "all available cash" on entry, "entire position" on exit - the
 *       same all-in/all-out model {@code BacktestEngine} uses, so live results stay
 *       comparable to a backtest of the same rule.</li>
 *   <li>Orders are placed as {@code MARKET} and immediately simulated as filled at the
 *       current price via {@link RecordExecutionService} - there is no real order book to
 *       wait on in a simulator, so instant fill is the honest model here, not an
 *       approximation of something more realistic that doesn't exist in this project.</li>
 *   <li>If {@link CreateOrderService} rejects the order (RiskEngine breach, kill switch,
 *       insufficient funds), the execution state is deliberately left unchanged so the
 *       scheduler retries the same transition on the next tick rather than silently
 *       giving up.</li>
 * </ul>
 */
@Service
public class StrategyExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(StrategyExecutionScheduler.class);
    private static final int PRICE_HISTORY_WINDOW = 500;

    private final StrategyJpaRepository strategyRepository;
    private final StrategyExecutionStateJpaRepository executionStateRepository;
    private final AssetJpaRepository assetRepository;
    private final MarketPriceJpaRepository marketPriceRepository;
    private final PortfolioProjectionJpaRepository portfolioRepository;
    private final PortfolioPositionJpaRepository positionRepository;
    private final CreateOrderService createOrderService;
    private final RecordExecutionService recordExecutionService;
    private final LeaderboardService leaderboardService;

    public StrategyExecutionScheduler(
        StrategyJpaRepository strategyRepository,
        StrategyExecutionStateJpaRepository executionStateRepository,
        AssetJpaRepository assetRepository,
        MarketPriceJpaRepository marketPriceRepository,
        PortfolioProjectionJpaRepository portfolioRepository,
        PortfolioPositionJpaRepository positionRepository,
        CreateOrderService createOrderService,
        RecordExecutionService recordExecutionService,
        LeaderboardService leaderboardService
    ) {
        this.strategyRepository = strategyRepository;
        this.executionStateRepository = executionStateRepository;
        this.assetRepository = assetRepository;
        this.marketPriceRepository = marketPriceRepository;
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.createOrderService = createOrderService;
        this.recordExecutionService = recordExecutionService;
        this.leaderboardService = leaderboardService;
    }

    @Scheduled(fixedDelayString = "${mywallet.strategy-execution.interval-ms:5000}")
    public void evaluateActiveStrategies() {
        List<StrategyEntity> activeStrategies = strategyRepository.findByStatus(StrategyStatus.ACTIVE);
        for (StrategyEntity strategy : activeStrategies) {
            try {
                evaluateOne(strategy);
            } catch (Exception e) {
                // One strategy's failure (bad data, a transient DB hiccup) must not stop
                // every other active strategy from being evaluated this tick - same
                // per-item resilience pattern as MarketDataGeneratorService.tick().
                log.error("Failed to evaluate strategy {} ({})", strategy.getId(), strategy.getName(), e);
            }
        }

        if (!activeStrategies.isEmpty()) {
            leaderboardService.broadcastSnapshot();
        }
    }

    /**
     * Package-private (not called directly outside this class besides its own scheduled
     * loop) and deliberately <strong>not</strong> {@code @Transactional}: called via
     * self-invocation from {@link #evaluateActiveStrategies()}, Spring's proxy-based AOP
     * would silently ignore a transactional annotation here anyway (same reasoning as
     * {@code IdempotencyGuardImpl}'s javadoc) - an annotation that doesn't actually apply
     * is worse than none, since it would misrepresent the guarantee this method has.
     *
     * <p>What this means in practice: {@link CreateOrderService#createOrder} and
     * {@link RecordExecutionService#recordExecution} each still run in their own real
     * transaction (they're separate beans, called through their own proxies), so an order
     * is either fully created or not at all, and a fill is either fully applied or not.
     * The one gap this leaves: if the process crashes in the narrow window between a
     * successful order+fill and {@code executionStateRepository.save(state)}, the next
     * tick would see the same flat->long transition and could issue a second order. This
     * is an accepted, documented risk for a demo project - closing it properly would mean
     * making the order and the state update part of one outbox-pattern unit, which is
     * more machinery than this scheduler's simplicity is worth right now.</p>
     */
    void evaluateOne(StrategyEntity strategy) {
        BooleanExpression rule = RuleParser.parse(strategy.getRuleExpression());

        String symbol;
        try {
            symbol = RuleSymbolExtractor.extractSingleSymbol(rule);
        } catch (IllegalArgumentException notSingleAsset) {
            log.warn("Strategy {} skipped this tick: {}", strategy.getId(), notSingleAsset.getMessage());
            return;
        }

        Optional<AssetEntity> maybeAsset = assetRepository.findBySymbol(symbol).filter(AssetEntity::isEnabled);
        if (maybeAsset.isEmpty()) {
            log.warn("Strategy {} references unknown/disabled asset {} - skipped this tick", strategy.getId(), symbol);
            return;
        }
        AssetEntity asset = maybeAsset.get();

        List<MarketPriceEntity> recentPrices = marketPriceRepository.findRecent(asset.getId(), PRICE_HISTORY_WINDOW);
        if (recentPrices.isEmpty()) {
            return; // no price data yet for this asset - nothing to evaluate against
        }
        List<BigDecimal> priceHistory = recentPrices.reversed().stream().map(MarketPriceEntity::getPrice).toList();

        PortfolioProjectionEntity portfolio = portfolioRepository.findById(strategy.getPortfolioId()).orElse(null);
        if (portfolio == null) {
            log.warn("Strategy {} references a missing portfolio {} - skipped this tick", strategy.getId(), strategy.getPortfolioId());
            return;
        }

        PortfolioPositionEntity existingPosition = positionRepository.findByPortfolioId(portfolio.getId()).stream()
            .filter(p -> p.getAssetId().equals(asset.getId()))
            .findFirst()
            .orElse(null);

        boolean signal = evaluateSignal(rule, priceHistory, portfolio, existingPosition);

        StrategyExecutionStateEntity state = executionStateRepository.findById(strategy.getId())
            .orElseGet(() -> new StrategyExecutionStateEntity(strategy.getId()));

        if (signal && !state.isCurrentlyLong()) {
            act(strategy, asset, portfolio, OrderSide.BUY, state);
        } else if (!signal && state.isCurrentlyLong()) {
            act(strategy, asset, portfolio, OrderSide.SELL, state);
        }
    }

    private boolean evaluateSignal(BooleanExpression rule, List<BigDecimal> priceHistory,
                                    PortfolioProjectionEntity portfolio, PortfolioPositionEntity existingPosition) {
        BigDecimal positionValue = existingPosition == null
            ? BigDecimal.ZERO
            : existingPosition.getQuantity().multiply(existingPosition.getAverageAcquisitionPrice());
        BigDecimal totalValue = portfolio.getCashBalance().add(positionValue);
        BigDecimal exposure = totalValue.signum() == 0 ? BigDecimal.ZERO : positionValue.divide(totalValue, 8, RoundingMode.HALF_UP);

        MarketContext context = new MarketContext() {
            @Override
            public List<BigDecimal> priceHistory(String symbol) {
                return priceHistory;
            }

            @Override
            public BigDecimal portfolioExposure() {
                return exposure;
            }
        };

        try {
            return new RuleEvaluator(context).evaluate(rule);
        } catch (IllegalArgumentException notEnoughHistoryYet) {
            return false; // still warming up - stay flat, same convention as BacktestEngine
        }
    }

    private void act(StrategyEntity strategy, AssetEntity asset, PortfolioProjectionEntity portfolio,
                      OrderSide side, StrategyExecutionStateEntity state) {
        BigDecimal referencePrice = marketPriceRepository.findFirstByAssetIdOrderByObservedAtDesc(asset.getId())
            .map(MarketPriceEntity::getPrice)
            .orElse(asset.getInitialPrice());

        BigDecimal quantity = side == OrderSide.BUY
            ? buyQuantity(portfolio, referencePrice)
            : sellQuantity(portfolio.getId(), asset.getId());

        if (quantity == null || quantity.signum() <= 0) {
            return; // nothing to buy (no cash) or nothing to sell (no position) - not an error, just a no-op this tick
        }

        UUID correlationId = UUID.randomUUID();
        UUID orderId;
        try {
            orderId = createOrderService.createOrder(
                strategy.getPortfolioId(), asset.getId(), OrderType.MARKET, side,
                quantity, null, strategy.getOwnerId(), correlationId
            );
        } catch (RuntimeException rejected) {
            // RiskEngine breach, kill switch, insufficient funds, ... - leave `state`
            // unchanged so the same transition is retried next tick rather than lost.
            log.info("Strategy {} auto-order rejected this tick: {}", strategy.getId(), rejected.getMessage());
            return;
        }

        String externalReference = "strategy-" + strategy.getId() + "-" + orderId;
        recordExecutionService.recordExecution(
            orderId, quantity, referencePrice, BigDecimal.ZERO, externalReference, correlationId, strategy.getOwnerId()
        );

        state.update(side == OrderSide.BUY, Instant.now());
        executionStateRepository.save(state);
    }

    private BigDecimal buyQuantity(PortfolioProjectionEntity portfolio, BigDecimal referencePrice) {
        if (referencePrice.signum() <= 0 || portfolio.getCashBalance().signum() <= 0) {
            return null;
        }
        return portfolio.getCashBalance().divide(referencePrice, 8, RoundingMode.DOWN);
    }

    private BigDecimal sellQuantity(UUID portfolioId, UUID assetId) {
        return positionRepository.findByPortfolioId(portfolioId).stream()
            .filter(p -> p.getAssetId().equals(assetId))
            .map(PortfolioPositionEntity::getQuantity)
            .findFirst()
            .orElse(null);
    }
}
