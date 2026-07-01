package io.mywallet.backtest.domain;

import io.mywallet.ruleengine.domain.evaluator.MarketContext;

import java.math.BigDecimal;
import java.util.List;

/**
 * Single-asset backtest simplification: the rule is assumed to reference only the asset
 * being backtested, so {@code symbol} is ignored and the same window is returned
 * regardless of what symbol the rule's indicator calls name. Multi-asset backtests
 * (cahier des charges: "plusieurs actifs") are explicitly deferred - this context would
 * need to become a {@code Map<String, List<BigDecimal>>} keyed by symbol to support that.
 */
final class BacktestMarketContext implements MarketContext {

    private final List<BigDecimal> priceWindow;
    private final BigDecimal exposureFraction;

    BacktestMarketContext(List<BigDecimal> priceWindow, BigDecimal exposureFraction) {
        this.priceWindow = priceWindow;
        this.exposureFraction = exposureFraction;
    }

    @Override
    public List<BigDecimal> priceHistory(String symbol) {
        return priceWindow;
    }

    @Override
    public BigDecimal portfolioExposure() {
        return exposureFraction;
    }
}
