package io.mywallet.strategy.application;

import io.mywallet.account.infrastructure.persistence.AccountEntity;
import io.mywallet.account.infrastructure.persistence.AccountJpaRepository;
import io.mywallet.portfolio.application.PortfolioValuationService;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import io.mywallet.ruleengine.domain.exception.RuleParseException;
import io.mywallet.ruleengine.domain.parser.RuleParser;
import io.mywallet.strategy.domain.RiskLevel;
import io.mywallet.strategy.domain.StrategyMode;
import io.mywallet.strategy.infrastructure.persistence.StrategyEntity;
import io.mywallet.strategy.infrastructure.persistence.StrategyExecutionStateEntity;
import io.mywallet.strategy.infrastructure.persistence.StrategyExecutionStateJpaRepository;
import io.mywallet.strategy.infrastructure.persistence.StrategyJpaRepository;
import io.mywallet.strategy.infrastructure.persistence.StrategyPerformanceBaselineEntity;
import io.mywallet.strategy.infrastructure.persistence.StrategyPerformanceBaselineJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * A strategy's {@code ruleExpression} is validated by {@link RuleParser#parse} right here,
 * before the entity is ever persisted - this is the gate that guarantees every stored
 * strategy has a syntactically valid, safe (no {@code eval()}, closed AST) rule. A parse
 * failure surfaces as a 422 with a clear message (via {@code RuleParseException} being a
 * {@code RuntimeException} caught by a dedicated handler - see {@code StrategyController}
 * / {@code GlobalExceptionHandler}), not a 500.
 */
@Service
public class StrategyApplicationService {

    private final StrategyJpaRepository strategyRepository;
    private final PortfolioProjectionJpaRepository portfolioRepository;
    private final AccountJpaRepository accountRepository;
    private final PortfolioValuationService portfolioValuationService;
    private final StrategyExecutionStateJpaRepository executionStateRepository;
    private final StrategyPerformanceBaselineJpaRepository performanceBaselineRepository;

    public StrategyApplicationService(
        StrategyJpaRepository strategyRepository,
        PortfolioProjectionJpaRepository portfolioRepository,
        AccountJpaRepository accountRepository,
        PortfolioValuationService portfolioValuationService,
        StrategyExecutionStateJpaRepository executionStateRepository,
        StrategyPerformanceBaselineJpaRepository performanceBaselineRepository
    ) {
        this.strategyRepository = strategyRepository;
        this.portfolioRepository = portfolioRepository;
        this.accountRepository = accountRepository;
        this.portfolioValuationService = portfolioValuationService;
        this.executionStateRepository = executionStateRepository;
        this.performanceBaselineRepository = performanceBaselineRepository;
    }

    @Transactional
    public StrategyEntity createDraft(
        UUID ownerId, UUID portfolioId, String name, String description, StrategyMode mode,
        RiskLevel riskLevel, BigDecimal maximumCapital, BigDecimal maximumLoss, String ruleExpression
    ) {
        assertPortfolioOwnership(portfolioId, ownerId);

        // Validates the expression is syntactically valid and safe - see class javadoc.
        // The parsed AST itself isn't stored (Phase 6 doesn't need it persisted - it's
        // re-parsed on evaluation in Phase 6b); this call exists purely as a validation gate.
        try {
            RuleParser.parse(ruleExpression);
        } catch (RuleParseException e) {
            throw e; // rethrown as-is; GlobalExceptionHandler maps RuleParseException to 422
        }

        StrategyEntity strategy = new StrategyEntity(
            UUID.randomUUID(), name, description, ownerId, portfolioId,
            mode, riskLevel, maximumCapital, maximumLoss, ruleExpression
        );
        return strategyRepository.save(strategy);
    }

    @Transactional
    public void activate(UUID strategyId, UUID requestingUserId) {
        StrategyEntity strategy = getOwned(strategyId, requestingUserId);
        strategy.activate();
        strategyRepository.save(strategy);

        // Re-captured every time a strategy (re)activates - including reactivation after
        // a suspend - so "return since activation" always measures from the most recent
        // time this strategy started acting, not some earlier activation.
        PortfolioProjectionEntity portfolio = portfolioRepository.findById(strategy.getPortfolioId())
            .orElseThrow(() -> new NoSuchElementException("Portfolio not found: " + strategy.getPortfolioId()));
        BigDecimal baselineValue = portfolioValuationService.currentValue(portfolio);
        performanceBaselineRepository.save(new StrategyPerformanceBaselineEntity(strategyId, strategy.getPortfolioId(), baselineValue));

        executionStateRepository.save(new StrategyExecutionStateEntity(strategyId)); // reset to flat on (re)activation
    }

    @Transactional
    public void suspend(UUID strategyId, UUID requestingUserId) {
        StrategyEntity strategy = getOwned(strategyId, requestingUserId);
        strategy.suspend();
        strategyRepository.save(strategy);
    }

    @Transactional
    public void deactivate(UUID strategyId, UUID requestingUserId) {
        StrategyEntity strategy = getOwned(strategyId, requestingUserId);
        strategy.deactivate();
        strategyRepository.save(strategy);
    }

    @Transactional(readOnly = true)
    public List<StrategyEntity> listForOwner(UUID ownerId) {
        return strategyRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public StrategyEntity getOwned(UUID strategyId, UUID requestingUserId) {
        StrategyEntity strategy = strategyRepository.findById(strategyId)
            .orElseThrow(() -> new NoSuchElementException("Strategy not found: " + strategyId));
        if (!strategy.getOwnerId().equals(requestingUserId)) {
            throw new NoSuchElementException("Strategy not found: " + strategyId); // 404, not 403 - same isolation principle used everywhere else
        }
        return strategy;
    }

    private void assertPortfolioOwnership(UUID portfolioId, UUID requestingUserId) {
        PortfolioProjectionEntity portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new NoSuchElementException("Portfolio not found: " + portfolioId));
        AccountEntity account = accountRepository.findById(portfolio.getAccountId())
            .orElseThrow(() -> new NoSuchElementException("Portfolio not found: " + portfolioId));
        if (!account.getOwnerId().equals(requestingUserId)) {
            throw new NoSuchElementException("Portfolio not found: " + portfolioId);
        }
    }
}
