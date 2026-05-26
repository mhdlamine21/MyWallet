package io.mywallet.portfolio.application;

import io.mywallet.account.infrastructure.persistence.AccountEntity;
import io.mywallet.account.infrastructure.persistence.AccountJpaRepository;
import io.mywallet.common.domain.EventStoreRepository;
import io.mywallet.portfolio.domain.model.Portfolio;
import io.mywallet.portfolio.domain.model.PortfolioMode;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Orchestrates the Portfolio aggregate and keeps its read projection
 * ({@code portfolio_projections}) in sync - synchronously, in the same transaction, for
 * now. Phase 5 moves this projection update to an asynchronous RabbitMQ consumer instead
 * (see the project plan's "événements asynchrones pour la mise à jour des positions"),
 * at which point this class's projection-writing responsibility moves to a dedicated
 * {@code PortfolioProjector} listening on the event bus.
 */
@Service
public class PortfolioApplicationService {

    private final EventStoreRepository<Portfolio> eventStoreRepository;
    private final PortfolioProjectionJpaRepository projectionRepository;
    private final PortfolioPositionJpaRepository positionRepository;
    private final AccountJpaRepository accountRepository;

    public PortfolioApplicationService(
        EventStoreRepository<Portfolio> eventStoreRepository,
        PortfolioProjectionJpaRepository projectionRepository,
        PortfolioPositionJpaRepository positionRepository,
        AccountJpaRepository accountRepository
    ) {
        this.eventStoreRepository = eventStoreRepository;
        this.projectionRepository = projectionRepository;
        this.positionRepository = positionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public UUID createPortfolio(UUID accountId, String name, PortfolioMode mode,
                                 BigDecimal initialCashBalance, UUID requestingUserId, UUID correlationId) {
        AccountEntity account = accountRepository.findById(accountId)
            .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));
        if (!account.getOwnerId().equals(requestingUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Account does not belong to the requesting user");
        }

        Portfolio portfolio = Portfolio.create(accountId, name, mode, initialCashBalance, correlationId, requestingUserId);
        eventStoreRepository.append(portfolio, 0L);

        projectionRepository.save(new PortfolioProjectionEntity(
            portfolio.getId(), accountId, name, mode.name(), initialCashBalance,
            portfolio.getVersion(), java.time.Instant.now()
        ));

        return portfolio.getId();
    }

    @Transactional(readOnly = true)
    public List<PortfolioProjectionEntity> listForOwner(UUID ownerId) {
        return projectionRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public PortfolioProjectionEntity getOwnedPortfolio(UUID portfolioId, UUID requestingUserId) {
        PortfolioProjectionEntity portfolio = projectionRepository.findById(portfolioId)
            .orElseThrow(() -> new NoSuchElementException("Portfolio not found: " + portfolioId));

        AccountEntity account = accountRepository.findById(portfolio.getAccountId())
            .orElseThrow(() -> new NoSuchElementException("Account not found: " + portfolio.getAccountId()));
        if (!account.getOwnerId().equals(requestingUserId)) {
            // 404, not 403 - do not reveal that a portfolio with this id exists at all to
            // a user who doesn't own it (same isolation principle as the risk register's
            // "isolation multi-comptes" item flagged during discovery).
            throw new NoSuchElementException("Portfolio not found: " + portfolioId);
        }
        return portfolio;
    }

    @Transactional(readOnly = true)
    public List<PortfolioPositionEntity> getPositions(UUID portfolioId, UUID requestingUserId) {
        getOwnedPortfolio(portfolioId, requestingUserId); // enforces ownership, throws if not found/owned
        return positionRepository.findByPortfolioId(portfolioId);
    }
}
