package io.mywallet.strategy.interfaces.rest;

import io.mywallet.infrastructure.websocket.WebSocketBroadcaster;
import io.mywallet.strategy.application.LeaderboardService;
import io.mywallet.strategy.application.StrategyApplicationService;
import io.mywallet.strategy.domain.RiskLevel;
import io.mywallet.strategy.interfaces.rest.dto.CreateStrategyRequest;
import io.mywallet.strategy.interfaces.rest.dto.StrategyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/strategies")
@Tag(name = "Strategies")
public class StrategyController {

    private final StrategyApplicationService strategyApplicationService;
    private final LeaderboardService leaderboardService;

    public StrategyController(StrategyApplicationService strategyApplicationService, LeaderboardService leaderboardService) {
        this.strategyApplicationService = strategyApplicationService;
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Live ranking of ACTIVE strategies by return since activation")
    public List<WebSocketBroadcaster.LeaderboardEntry> leaderboard() {
        return leaderboardService.snapshot();
    }

    @PostMapping
    @Operation(summary = "Create a new strategy in DRAFT status (rule expression is validated immediately)")
    public ResponseEntity<StrategyResponse> create(@Valid @RequestBody CreateStrategyRequest request, Authentication authentication) {
        var strategy = strategyApplicationService.createDraft(
            userId(authentication), request.portfolioId(), request.name(), request.description(),
            request.mode(), request.riskLevel() != null ? request.riskLevel() : RiskLevel.MEDIUM,
            request.maximumCapital(), request.maximumLoss(), request.ruleExpression()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(StrategyResponse.from(strategy));
    }

    @GetMapping
    public List<StrategyResponse> list(Authentication authentication) {
        return strategyApplicationService.listForOwner(userId(authentication)).stream()
            .map(StrategyResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public StrategyResponse get(@PathVariable UUID id, Authentication authentication) {
        return StrategyResponse.from(strategyApplicationService.getOwned(id, userId(authentication)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id, Authentication authentication) {
        strategyApplicationService.activate(id, userId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspend(@PathVariable UUID id, Authentication authentication) {
        strategyApplicationService.suspend(id, userId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id, Authentication authentication) {
        strategyApplicationService.deactivate(id, userId(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
