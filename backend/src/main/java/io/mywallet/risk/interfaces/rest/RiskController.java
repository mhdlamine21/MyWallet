package io.mywallet.risk.interfaces.rest;

import io.mywallet.account.infrastructure.persistence.AccountEntity;
import io.mywallet.account.infrastructure.persistence.AccountJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import io.mywallet.risk.infrastructure.persistence.RiskAlertJpaRepository;
import io.mywallet.risk.interfaces.rest.dto.RiskAlertResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/risk")
@Tag(name = "Risk")
public class RiskController {

    private final RiskAlertJpaRepository riskAlertRepository;
    private final PortfolioProjectionJpaRepository portfolioProjectionRepository;
    private final AccountJpaRepository accountRepository;

    public RiskController(
        RiskAlertJpaRepository riskAlertRepository,
        PortfolioProjectionJpaRepository portfolioProjectionRepository,
        AccountJpaRepository accountRepository
    ) {
        this.riskAlertRepository = riskAlertRepository;
        this.portfolioProjectionRepository = portfolioProjectionRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/alerts")
    public List<RiskAlertResponse> alerts(@RequestParam UUID portfolioId, Authentication authentication) {
        assertPortfolioOwnership(portfolioId, UUID.fromString(authentication.getName()));
        return riskAlertRepository.findByPortfolioIdOrderByRaisedAtDesc(portfolioId).stream()
            .map(RiskAlertResponse::from)
            .toList();
    }

    private void assertPortfolioOwnership(UUID portfolioId, UUID requestingUserId) {
        var portfolio = portfolioProjectionRepository.findById(portfolioId)
            .orElseThrow(() -> new NoSuchElementException("Portfolio not found: " + portfolioId));
        AccountEntity account = accountRepository.findById(portfolio.getAccountId())
            .orElseThrow(() -> new NoSuchElementException("Portfolio not found: " + portfolioId));
        if (!account.getOwnerId().equals(requestingUserId)) {
            throw new NoSuchElementException("Portfolio not found: " + portfolioId);
        }
    }
}
