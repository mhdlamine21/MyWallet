package io.mywallet.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderProjectionJpaRepository extends JpaRepository<OrderProjectionEntity, UUID> {
    List<OrderProjectionEntity> findByPortfolioIdOrderByCreatedAtDesc(UUID portfolioId);
    long countByPortfolioIdAndCreatedAtAfter(UUID portfolioId, Instant after);
}
