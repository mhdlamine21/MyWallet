package io.mywallet.portfolio.interfaces.rest;

import io.mywallet.portfolio.application.PortfolioApplicationService;
import io.mywallet.portfolio.interfaces.rest.dto.CreatePortfolioRequest;
import io.mywallet.portfolio.interfaces.rest.dto.PortfolioResponse;
import io.mywallet.portfolio.interfaces.rest.dto.PositionResponse;
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
@RequestMapping("/api/portfolios")
@Tag(name = "Portfolios")
public class PortfolioController {

    private final PortfolioApplicationService portfolioApplicationService;

    public PortfolioController(PortfolioApplicationService portfolioApplicationService) {
        this.portfolioApplicationService = portfolioApplicationService;
    }

    @PostMapping
    @Operation(summary = "Create a new portfolio under one of the caller's accounts")
    public ResponseEntity<PortfolioResponse> create(
        @Valid @RequestBody CreatePortfolioRequest request,
        Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        UUID correlationId = UUID.randomUUID(); // Phase 3+: could be sourced from CorrelationIdFilter's MDC instead

        UUID portfolioId = portfolioApplicationService.createPortfolio(
            request.accountId(), request.name(), request.mode(), request.initialCashBalance(),
            userId, correlationId
        );

        PortfolioResponse response = PortfolioResponse.from(
            portfolioApplicationService.getOwnedPortfolio(portfolioId, userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all portfolios owned by the authenticated user")
    public List<PortfolioResponse> list(Authentication authentication) {
        return portfolioApplicationService.listForOwner(currentUserId(authentication)).stream()
            .map(PortfolioResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single portfolio owned by the authenticated user")
    public PortfolioResponse get(@PathVariable UUID id, Authentication authentication) {
        return PortfolioResponse.from(portfolioApplicationService.getOwnedPortfolio(id, currentUserId(authentication)));
    }

    @GetMapping("/{id}/positions")
    @Operation(summary = "List current positions for a portfolio owned by the authenticated user")
    public List<PositionResponse> positions(@PathVariable UUID id, Authentication authentication) {
        return portfolioApplicationService.getPositions(id, currentUserId(authentication)).stream()
            .map(PositionResponse::from)
            .toList();
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
