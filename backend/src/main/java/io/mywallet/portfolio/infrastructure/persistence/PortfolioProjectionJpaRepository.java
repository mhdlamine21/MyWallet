package io.mywallet.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PortfolioProjectionJpaRepository extends JpaRepository<PortfolioProjectionEntity, UUID> {
    List<PortfolioProjectionEntity> findByAccountId(UUID accountId);

    /** All portfolios belonging to accounts owned by this user - powers GET /api/portfolios. */
    @org.springframework.data.jpa.repository.Query("""
        SELECT p FROM PortfolioProjectionEntity p
        WHERE p.accountId IN (SELECT a.id FROM io.mywallet.account.infrastructure.persistence.AccountEntity a WHERE a.ownerId = :ownerId)
        """)
    List<PortfolioProjectionEntity> findByOwnerId(UUID ownerId);
}
