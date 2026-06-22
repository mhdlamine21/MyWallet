package io.mywallet.strategy.application;

import io.mywallet.infrastructure.websocket.WebSocketBroadcaster;
import io.mywallet.portfolio.application.PortfolioValuationService;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import io.mywallet.strategy.domain.StrategyStatus;
import io.mywallet.strategy.infrastructure.persistence.StrategyJpaRepository;
import io.mywallet.strategy.infrastructure.persistence.StrategyPerformanceBaselineEntity;
import io.mywallet.strategy.infrastructure.persistence.StrategyPerformanceBaselineJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * "Return since activation" for every currently-{@code ACTIVE} strategy, ranked highest
 * first - this is the multi-bot leaderboard differentiator from the original feature
 * brainstorm. Baseline capture happens in {@code StrategyApplicationService.activate()};
 * this service only reads what's already there and computes the delta.
 *
 * <p>Deliberately scoped to {@code ACTIVE} strategies only: a suspended or deactivated
 * strategy's baseline still exists in the database, but showing it on a "live" leaderboard
 * would be misleading - its portfolio value keeps moving from any manual trading even
 * after the strategy itself stopped acting.</p>
 */
@Service
public class LeaderboardService {

    private final StrategyJpaRepository strategyRepository;
    private final StrategyPerformanceBaselineJpaRepository baselineRepository;
    private final PortfolioProjectionJpaRepository portfolioRepository;
    private final PortfolioValuationService portfolioValuationService;
    private final WebSocketBroadcaster broadcaster;

    public LeaderboardService(
        StrategyJpaRepository strategyRepository,
        StrategyPerformanceBaselineJpaRepository baselineRepository,
        PortfolioProjectionJpaRepository portfolioRepository,
        PortfolioValuationService portfolioValuationService,
        WebSocketBroadcaster broadcaster
    ) {
        this.strategyRepository = strategyRepository;
        this.baselineRepository = baselineRepository;
        this.portfolioRepository = portfolioRepository;
        this.portfolioValuationService = portfolioValuationService;
        this.broadcaster = broadcaster;
    }

    @Transactional(readOnly = true)
    public List<WebSocketBroadcaster.LeaderboardEntry> snapshot() {
        List<StrategyPerformanceBaselineEntity> baselines = baselineRepository.findAll();

        return baselines.stream()
            .map(baseline -> strategyRepository.findById(baseline.getStrategyId())
                .filter(s -> s.getStatus() == StrategyStatus.ACTIVE)
                .flatMap(strategy -> portfolioRepository.findById(baseline.getPortfolioId())
                    .map(portfolio -> {
                        BigDecimal currentValue = portfolioValuationService.currentValue(portfolio);
                        BigDecimal returnFraction = baseline.getBaselineValue().signum() == 0
                            ? BigDecimal.ZERO
                            : currentValue.subtract(baseline.getBaselineValue())
                                .divide(baseline.getBaselineValue(), 6, RoundingMode.HALF_UP);
                        return new WebSocketBroadcaster.LeaderboardEntry(
                            strategy.getId(), strategy.getName(), currentValue, baseline.getBaselineValue(), returnFraction
                        );
                    }))
            )
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::get)
            .sorted(Comparator.comparing(WebSocketBroadcaster.LeaderboardEntry::returnFraction).reversed())
            .toList();
    }

    /** Recomputes and pushes the current standings to every connected leaderboard subscriber. */
    @Transactional(readOnly = true)
    public void broadcastSnapshot() {
        broadcaster.broadcastLeaderboard(snapshot());
    }
}
