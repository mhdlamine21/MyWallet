package io.mywallet.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Central place for MyWallet's business-level metrics (as opposed to the JVM/HTTP
 * metrics Spring Boot Actuator already exposes for free). Naming convention:
 * {@code mywallet.<domain>.<event>} - this is what the project plan's "temps moyen de
 * traitement des ordres", "nombre d'événements traités", "nombre d'alertes", "nombre
 * d'ordres rejetés" observability requirements map to concretely.
 *
 * <p>Grafana dashboards (or just {@code /actuator/prometheus} scraped ad hoc) can graph
 * these directly - no custom exporter needed, Micrometer's Prometheus registry (already
 * in {@code pom.xml}) handles the format.</p>
 */
@Component
public class MyWalletMetrics {

    private final Counter ordersCreated;
    private final Counter ordersRejectedByRisk;
    private final Counter ordersRejectedByKillSwitch;
    private final Counter riskAlertsRaised;
    private final Counter positionUpdateEventsProcessed;
    private final Counter positionUpdateDuplicatesSkipped;
    private final Timer orderCreationDuration;
    private final Timer riskAssessmentDuration;

    public MyWalletMetrics(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("mywallet.orders.created")
            .description("Number of orders successfully accepted")
            .register(registry);
        this.ordersRejectedByRisk = Counter.builder("mywallet.orders.rejected")
            .tag("reason", "risk_limit")
            .description("Number of orders rejected by the RiskEngine")
            .register(registry);
        this.ordersRejectedByKillSwitch = Counter.builder("mywallet.orders.rejected")
            .tag("reason", "kill_switch")
            .description("Number of orders rejected because the global kill switch was active")
            .register(registry);
        this.riskAlertsRaised = Counter.builder("mywallet.risk.alerts_raised")
            .description("Number of RiskAlert rows created, across all limit types")
            .register(registry);
        this.positionUpdateEventsProcessed = Counter.builder("mywallet.eventstore.events_processed")
            .tag("projector", "position-projector")
            .description("Number of OrderFilled/OrderPartiallyFilled events successfully applied to positions")
            .register(registry);
        this.positionUpdateDuplicatesSkipped = Counter.builder("mywallet.eventstore.duplicate_deliveries_skipped")
            .tag("projector", "position-projector")
            .description("Number of duplicate message deliveries safely skipped by the idempotency guard")
            .register(registry);
        this.orderCreationDuration = Timer.builder("mywallet.orders.creation_duration")
            .description("End-to-end time to validate and persist a new order")
            .publishPercentileHistogram()
            .register(registry);
        this.riskAssessmentDuration = Timer.builder("mywallet.risk.assessment_duration")
            .description("Time to assemble the risk context and run all RiskChecks for one order")
            .register(registry);
    }

    public void recordOrderCreated() {
        ordersCreated.increment();
    }

    public void recordOrderRejectedByRisk() {
        ordersRejectedByRisk.increment();
    }

    public void recordOrderRejectedByKillSwitch() {
        ordersRejectedByKillSwitch.increment();
    }

    public void recordRiskAlertRaised(int count) {
        riskAlertsRaised.increment(count);
    }

    public void recordPositionUpdateEventProcessed() {
        positionUpdateEventsProcessed.increment();
    }

    public void recordPositionUpdateDuplicateSkipped() {
        positionUpdateDuplicatesSkipped.increment();
    }

    public Timer.Sample startOrderCreationTimer() {
        return Timer.start();
    }

    public void stopOrderCreationTimer(Timer.Sample sample) {
        sample.stop(orderCreationDuration);
    }

    public <T> T timeRiskAssessment(java.util.function.Supplier<T> block) {
        long start = System.nanoTime();
        try {
            return block.get();
        } finally {
            riskAssessmentDuration.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }
}
