package io.mywallet.asset.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetJpaRepository extends JpaRepository<AssetEntity, UUID> {
    Optional<AssetEntity> findBySymbol(String symbol);
    List<AssetEntity> findByEnabledTrue();
}
