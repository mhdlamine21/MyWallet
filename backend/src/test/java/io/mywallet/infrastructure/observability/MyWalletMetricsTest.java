package io.mywallet.infrastructure.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MyWalletMetricsTest {

    @Test
    void countersIncrementIndependentlyByTagValue() {
        var registry = new SimpleMeterRegistry();
        var metrics = new MyWalletMetrics(registry);

        metrics.recordOrderCreated();
        metrics.recordOrderCreated();
        metrics.recordOrderRejectedByRisk();

        assertThat(registry.counter("mywallet.orders.created").count()).isEqualTo(2.0);
        assertThat(registry.counter("mywallet.orders.rejected", "reason", "risk_limit").count()).isEqualTo(1.0);
        assertThat(registry.counter("mywallet.orders.rejected", "reason", "kill_switch").count()).isEqualTo(0.0);
    }

    @Test
    void orderCreationTimerRecordsAtLeastOneSample() {
        var registry = new SimpleMeterRegistry();
        var metrics = new MyWalletMetrics(registry);

        var sample = metrics.startOrderCreationTimer();
        metrics.stopOrderCreationTimer(sample);

        assertThat(registry.timer("mywallet.orders.creation_duration").count()).isEqualTo(1L);
    }

    @Test
    void riskAlertCounterAccumulatesAcrossMultipleBreaches() {
        var registry = new SimpleMeterRegistry();
        var metrics = new MyWalletMetrics(registry);

        metrics.recordRiskAlertRaised(3);
        metrics.recordRiskAlertRaised(2);

        assertThat(registry.counter("mywallet.risk.alerts_raised").count()).isEqualTo(5.0);
    }
}
