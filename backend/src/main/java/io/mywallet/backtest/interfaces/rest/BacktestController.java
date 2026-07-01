package io.mywallet.backtest.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.backtest.application.RunBacktestService;
import io.mywallet.backtest.infrastructure.persistence.BacktestResultJpaRepository;
import io.mywallet.backtest.interfaces.rest.dto.BacktestResultResponse;
import io.mywallet.backtest.interfaces.rest.dto.RunBacktestRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/strategies/{strategyId}/backtest")
@Tag(name = "Backtesting")
public class BacktestController {

    private final RunBacktestService runBacktestService;
    private final BacktestResultJpaRepository backtestResultRepository;
    private final ObjectMapper objectMapper;

    public BacktestController(
        RunBacktestService runBacktestService,
        BacktestResultJpaRepository backtestResultRepository,
        ObjectMapper objectMapper
    ) {
        this.runBacktestService = runBacktestService;
        this.backtestResultRepository = backtestResultRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Operation(summary = "Run a backtest of this strategy over historical simulated price data, including a Monte Carlo confidence band")
    public ResponseEntity<BacktestResultResponse> run(
        @PathVariable UUID strategyId, @Valid @RequestBody RunBacktestRequest request, Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        BigDecimal feeRate = request.feeRate() != null ? request.feeRate() : BigDecimal.ZERO;
        BigDecimal slippageRate = request.slippageRate() != null ? request.slippageRate() : BigDecimal.ZERO;

        UUID backtestId = runBacktestService.runBacktest(
            strategyId, userId, request.assetId(), request.initialCapital(),
            request.periodStart(), request.periodEnd(), feeRate, slippageRate, request.seed()
        );

        var result = backtestResultRepository.findByBacktestId(backtestId)
            .orElseThrow(() -> new IllegalStateException("Backtest result missing immediately after being persisted"));

        return ResponseEntity.status(HttpStatus.CREATED).body(BacktestResultResponse.from(result, objectMapper));
    }
}
