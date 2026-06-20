package io.mywallet.ruleengine.domain.analysis;

import io.mywallet.ruleengine.domain.parser.RuleParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleSymbolExtractorTest {

    @Test
    void extractsTheSingleSymbolFromASimpleRule() {
        var rule = RuleParser.parse("SMA(BTCUSDT, 20) > SMA(BTCUSDT, 50) AND RSI(BTCUSDT, 14) < 70");
        assertThat(RuleSymbolExtractor.extractSymbols(rule)).containsExactly("BTCUSDT");
        assertThat(RuleSymbolExtractor.extractSingleSymbol(rule)).isEqualTo("BTCUSDT");
    }

    @Test
    void extractsMultipleDistinctSymbolsWhenPresent() {
        var rule = RuleParser.parse("SMA(BTCUSDT, 20) > 100 AND SMA(ETHUSDT, 10) > 50");
        assertThat(RuleSymbolExtractor.extractSymbols(rule)).containsExactlyInAnyOrder("BTCUSDT", "ETHUSDT");
    }

    @Test
    void singleSymbolExtractionThrowsForMultiAssetRules() {
        var rule = RuleParser.parse("SMA(BTCUSDT, 20) > 100 AND SMA(ETHUSDT, 10) > 50");
        assertThatThrownBy(() -> RuleSymbolExtractor.extractSingleSymbol(rule))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("multiple symbols");
    }

    @Test
    void singleSymbolExtractionThrowsForRulesWithNoIndicatorAtAll() {
        var rule = RuleParser.parse("PortfolioExposure < 50%");
        assertThatThrownBy(() -> RuleSymbolExtractor.extractSingleSymbol(rule))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no indicator");
    }

    @Test
    void handlesNotAndParenthesesCorrectly() {
        var rule = RuleParser.parse("NOT (SMA(BTCUSDT, 20) > 100)");
        assertThat(RuleSymbolExtractor.extractSingleSymbol(rule)).isEqualTo("BTCUSDT");
    }
}
