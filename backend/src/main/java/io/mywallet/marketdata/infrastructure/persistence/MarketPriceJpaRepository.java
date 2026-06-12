package io.mywallet.marketdata.infrastructure.persistence;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketPriceJpaRepository extends JpaRepository<MarketPriceEntity, UUID> {

    Optional<MarketPriceEntity> findFirstByAssetIdOrderByObservedAtDesc(UUID assetId);

    List<MarketPriceEntity> findByAssetIdAndObservedAtBetweenOrderByObservedAtAsc(
        UUID assetId, Instant from, Instant to);

    default List<MarketPriceEntity> findRecent(UUID assetId, int limit) {
        return findByAssetIdOrderByObservedAtDesc(assetId, Limit.of(limit));
    }

    List<MarketPriceEntity> findByAssetIdOrderByObservedAtDesc(UUID assetId, Limit limit);
}
