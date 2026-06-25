package io.mywallet.risk.application;

import io.mywallet.infrastructure.websocket.WebSocketBroadcaster;
import io.mywallet.order.infrastructure.persistence.OrderProjectionJpaRepository;
import io.mywallet.portfolio.application.PortfolioValuationService;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import io.mywallet.risk.domain.detection.IsolationForest;
import io.mywallet.risk.infrastructure.persistence.RiskAlertEntity;
import io.mywallet.risk.infrastructure.persistence.RiskAlertJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Periodically fits an {@link IsolationForest} over the <em>whole current population</em>
 * of portfolios' behavioral features - orders placed today, largest single-asset exposure
 * fraction, and portfolio value - and flags any portfolio the forest scores as an outlier
 * relative to everyone else. This is genuinely unsupervised: there is no pre-labeled
 * "normal" dataset anywhere, the population <em>is</em> the training data, refit fresh
 * every cycle so the definition of "normal" adapts as the population's own behavior
 * shifts (e.g. once the market-shock differentiator from the brainstorm exists, a broad
 * volatility spike would move what "normal order frequency" looks like for everyone at
 * once, not just flag whoever happens to be trading during it).
 *
 * <p>Requires a minimum population size ({@link #MINIMUM_POPULATION}) before it runs at
 * all - comparing one portfolio's behavior to "everyone" when there are only one or two
 * portfolios total isn't a meaningful anomaly signal, it's just noise.</p>
 */
@Service
public class BehaviorAnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(BehaviorAnomalyDetectionService.class);
    private static final String LIMIT_TYPE = "ANOMALOUS_BEHAVIOR";
    private static final int MINIMUM_POPULATION = 3;
    private static final int FOREST_TREES = 100;
    private static final int SAMPLE_SIZE = 64;
    private static final long COOLDOWN_MINUTES = 30; // don't re-alert the same portfolio more than once per this window

    private final PortfolioProjectionJpaRepository portfolioRepository;
    private final PortfolioPositionJpaRepository positionRepository;
    private final OrderProjectionJpaRepository orderProjectionRepository;
    private final PortfolioValuationService portfolioValuationService;
    private final RiskAlertJpaRepository riskAlertRepository;
    private final WebSocketBroadcaster broadcaster;
    private final double anomalyScoreThreshold;

    public BehaviorAnomalyDetectionService(
        PortfolioProjectionJpaRepository portfolioRepository,
        PortfolioPositionJpaRepository positionRepository,
        OrderProjectionJpaRepository orderProjectionRepository,
        PortfolioValuationService portfolioValuationService,
        RiskAlertJpaRepository riskAlertRepository,
        WebSocketBroadcaster broadcaster,
        @Value("${mywallet.detection.behavior-anomaly.anomaly-score-threshold:0.65}") double anomalyScoreThreshold
    ) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.orderProjectionRepository = orderProjectionRepository;
        this.portfolioValuationService = portfolioValuationService;
        this.riskAlertRepository = riskAlertRepository;
        this.broadcaster = broadcaster;
        this.anomalyScoreThreshold = anomalyScoreThreshold;
    }

    @Scheduled(fixedDelayString = "${mywallet.detection.behavior-anomaly.interval-ms:30000}")
    @Transactional
    public void detectAnomalousPortfolios() {
        List<PortfolioProjectionEntity> portfolios = portfolioRepository.findAll();
        if (portfolios.size() < MINIMUM_POPULATION) {
            return; // not enough of a population to compare against yet
        }

        Map<PortfolioProjectionEntity, double[]> featuresByPortfolio = new java.util.LinkedHashMap<>();
        for (PortfolioProjectionEntity portfolio : portfolios) {
            featuresByPortfolio.put(portfolio, computeFeatures(portfolio));
        }

        List<double[]> population = new ArrayList<>(featuresByPortfolio.values());
        IsolationForest forest = IsolationForest.fit(population, FOREST_TREES, SAMPLE_SIZE, System.nanoTime());

        for (var entry : featuresByPortfolio.entrySet()) {
            double score = forest.anomalyScore(entry.getValue());
            if (score >= anomalyScoreThreshold) {
                raiseAlertIfNotOnCooldown(entry.getKey(), entry.getValue(), score);
            }
        }
    }

    private double[] computeFeatures(PortfolioProjectionEntity portfolio) {
        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long ordersToday = orderProjectionRepository.countByPortfolioIdAndCreatedAtAfter(portfolio.getId(), startOfToday);

        List<PortfolioPositionEntity> positions = positionRepository.findByPortfolioId(portfolio.getId());
        BigDecimal totalValue = portfolioValuationService.currentValue(portfolio);
        BigDecimal largestPositionValue = positions.stream()
            .map(p -> p.getQuantity().multiply(p.getAverageAcquisitionPrice()))
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        double maxExposureFraction = totalValue.signum() == 0
            ? 0.0
            : largestPositionValue.divide(totalValue, 8, RoundingMode.HALF_UP).doubleValue();

        return new double[]{ (double) ordersToday, maxExposureFraction, totalValue.doubleValue() };
    }

    private void raiseAlertIfNotOnCooldown(PortfolioProjectionEntity portfolio, double[] features, double score) {
        Instant cooldownCutoff = Instant.now().minus(COOLDOWN_MINUTES, ChronoUnit.MINUTES);
        if (riskAlertRepository.existsByPortfolioIdAndLimitTypeAndRaisedAtAfter(portfolio.getId(), LIMIT_TYPE, cooldownCutoff)) {
            return;
        }

        int riskScore = (int) Math.min(100, Math.round(score * 100));
        String level = riskScore >= 90 ? "CRITICAL" : riskScore >= 75 ? "HIGH" : "MEDIUM";
        String explanation = "Portfolio behavior flagged as anomalous relative to the current population "
            + "(orders today=%.0f, max single-asset exposure=%.1f%%, anomaly score=%.2f)"
                .formatted(features[0], features[1] * 100, score);

        riskAlertRepository.save(new RiskAlertEntity(java.util.UUID.randomUUID(), portfolio.getId(), LIMIT_TYPE, level, riskScore, explanation));
        broadcaster.broadcastRiskAlert(portfolio.getId(), LIMIT_TYPE, level, riskScore, explanation);
        log.info("Flagged anomalous behavior for portfolio {} (score={})", portfolio.getId(), score);
    }
}
