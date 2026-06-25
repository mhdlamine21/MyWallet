package io.mywallet.risk.application;

import io.mywallet.infrastructure.websocket.WebSocketBroadcaster;
import io.mywallet.risk.infrastructure.persistence.MarketAnomalyJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PriceAnomalyDetectionServiceTest {

    private final MarketAnomalyJpaRepository anomalyRepository = mock(MarketAnomalyJpaRepository.class);
    private final WebSocketBroadcaster broadcaster = mock(WebSocketBroadcaster.class);
    private final PriceAnomalyDetectionService service =
        new PriceAnomalyDetectionService(anomalyRepository, broadcaster, 4.0);

    @Test
    void doesNotFlagNormalPriceMovement() {
        UUID assetId = UUID.randomUUID();
        // Feed a stable, tight sequence - nothing here should ever reach a z-score of 4.
        service.observe(assetId, "STABLE", new BigDecimal("100.00"));
        service.observe(assetId, "STABLE", new BigDecimal("100.10"));
        service.observe(assetId, "STABLE", new BigDecimal("99.95"));
        service.observe(assetId, "STABLE", new BigDecimal("100.05"));

        verify(anomalyRepository, never()).save(any());
        verify(broadcaster, never()).broadcastMarketAnomaly(any(), any(), any(), any());
    }

    @Test
    void flagsAndPersistsAndBroadcastsAClearOutlierTick() {
        UUID assetId = UUID.randomUUID();
        // Establish a tight baseline first.
        service.observe(assetId, "SPIKY", new BigDecimal("100.00"));
        service.observe(assetId, "SPIKY", new BigDecimal("100.10"));
        service.observe(assetId, "SPIKY", new BigDecimal("99.95"));
        service.observe(assetId, "SPIKY", new BigDecimal("100.05"));

        // A wild outlier tick.
        service.observe(assetId, "SPIKY", new BigDecimal("500.00"));

        ArgumentCaptor<io.mywallet.risk.infrastructure.persistence.MarketAnomalyEntity> captor =
            ArgumentCaptor.forClass(io.mywallet.risk.infrastructure.persistence.MarketAnomalyEntity.class);
        verify(anomalyRepository).save(captor.capture());
        assertThat(captor.getValue().getAssetId()).isEqualTo(assetId);
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("500.00");

        verify(broadcaster).broadcastMarketAnomaly(eq("SPIKY"), eq(new BigDecimal("500.00")), any(), any());
    }

    @Test
    void continuesTrackingStatisticsAcrossManyObservationsWithoutError() {
        UUID assetId = UUID.randomUUID();
        // Sanity check that repeated calls (simulating many ticks over a long-running
        // process) don't throw or degrade - this is the whole point of using a streaming
        // (constant-memory) algorithm rather than something that recomputes from stored history.
        for (int i = 0; i < 1000; i++) {
            service.observe(assetId, "LONGRUN", new BigDecimal(100 + (i % 5)));
        }
        // No assertion beyond "it didn't throw" - this is a resilience/soak-style check.
    }
}
