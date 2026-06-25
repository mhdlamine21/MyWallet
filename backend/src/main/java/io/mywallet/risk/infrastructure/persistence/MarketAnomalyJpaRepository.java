package io.mywallet.risk.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarketAnomalyJpaRepository extends JpaRepository<MarketAnomalyEntity, UUID> {
    List<MarketAnomalyEntity> findByAssetIdOrderByDetectedAtDesc(UUID assetId);
}
