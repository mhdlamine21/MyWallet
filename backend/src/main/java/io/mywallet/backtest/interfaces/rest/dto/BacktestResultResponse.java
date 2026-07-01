package io.mywallet.backtest.interfaces.rest.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.backtest.infrastructure.persistence.BacktestResultEntity;

import java.math.BigDecimal;
import java.util.UUID;

public record BacktestResultResponse(
    UUID id,
    UUID backtestId,
    BigDecimal finalCapital,
    BigDecimal totalReturn,
    BigDecimal annualizedReturn,
    int numberOfTrades,
    BigDecimal winRate,
    BigDecimal averageGain,
    BigDecimal averageLoss,
    BigDecimal maxDrawdown,
    BigDecimal sharpeRatio,
    BigDecimal sortinoRatio,
    JsonNode equityCurve,
    JsonNode buyAndHoldEquityCurve,
    JsonNode monteCarloP5,
    JsonNode monteCarloP50,
    JsonNode monteCarloP95,
    JsonNode trades
) {
    public static BacktestResultResponse from(BacktestResultEntity entity, ObjectMapper objectMapper) {
        try {
            return new BacktestResultResponse(
                entity.getId(), entity.getBacktestId(), entity.getFinalCapital(), entity.getTotalReturn(),
                entity.getAnnualizedReturn(), entity.getNumberOfTrades(), entity.getWinRate(),
                entity.getAverageGain(), entity.getAverageLoss(), entity.getMaxDrawdown(),
                entity.getSharpeRatio(), entity.getSortinoRatio(),
                objectMapper.readTree(entity.getEquityCurveJson()),
                objectMapper.readTree(entity.getBuyAndHoldEquityCurveJson()),
                objectMapper.readTree(entity.getMonteCarloP5Json()),
                objectMapper.readTree(entity.getMonteCarloP50Json()),
                objectMapper.readTree(entity.getMonteCarloP95Json()),
                objectMapper.readTree(entity.getTradesJson())
            );
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Corrupted backtest result JSON for id " + entity.getId(), e);
        }
    }
}
