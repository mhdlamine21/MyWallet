package io.mywallet.risk.domain;

import io.mywallet.order.domain.model.OrderSide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RiskEngineTest {

    private RiskCheckContext buyContext(BigDecimal orderValue, BigDecimal cash, RiskLimits limits) {
        return new RiskCheckContext(
            OrderSide.BUY, BigDecimal.TEN, orderValue, cash,
            BigDecimal.ZERO, BigDecimal.ZERO, cash, 0, limits
        );
    }

    @Test
    void acceptsAnOrderThatBreachesNothing() {
        var engine = RiskEngine.withDefaultChecks();
        var result = engine.assess(buyContext(new BigDecimal("500"), new BigDecimal("10000"), RiskLimits.none()));

        assertThat(result.accepted()).isTrue();
        assertThat(result.breaches()).isEmpty();
        assertThat(result.riskScore()).isZero();
        assertThat(result.level()).isEqualTo(RiskEngine.RiskAssessment.RiskLevel.LOW);
    }

    @Test
    void rejectsAnOrderExceedingAvailableCash() {
        var engine = RiskEngine.withDefaultChecks();
        var result = engine.assess(buyContext(new BigDecimal("5000"), new BigDecimal("1000"), RiskLimits.none()));

        assertThat(result.accepted()).isFalse();
        assertThat(result.breaches()).hasSize(1);
        assertThat(result.breaches().get(0).limitType()).isEqualTo("AVAILABLE_CASH");
        assertThat(result.breaches().get(0).explanation()).contains("5000").contains("1000");
    }

    @Test
    void rejectsAnOrderExceedingMaxOrderValue() {
        var limits = new RiskLimits(new BigDecimal("1000"), null, null);
        var engine = RiskEngine.withDefaultChecks();
        var result = engine.assess(buyContext(new BigDecimal("2000"), new BigDecimal("100000"), limits));

        assertThat(result.accepted()).isFalse();
        assertThat(result.breaches().get(0).limitType()).isEqualTo("MAX_ORDER_VALUE");
    }

    @Test
    void reportsMultipleSimultaneousBreachesNotJustTheFirst() {
        var limits = new RiskLimits(new BigDecimal("100"), null, null);
        var engine = RiskEngine.withDefaultChecks();
        // Breaches both MAX_ORDER_VALUE (2000 > 100) and AVAILABLE_CASH (2000 > 500)
        var result = engine.assess(buyContext(new BigDecimal("2000"), new BigDecimal("500"), limits));

        assertThat(result.breaches()).hasSize(2);
        assertThat(result.riskScore()).isEqualTo(70);
        assertThat(result.level()).isEqualTo(RiskEngine.RiskAssessment.RiskLevel.HIGH);
    }

    @Test
    void rejectsSellingMoreThanCurrentlyHeld() {
        var context = new RiskCheckContext(
            OrderSide.SELL, new BigDecimal("10"), new BigDecimal("500"), BigDecimal.ZERO,
            new BigDecimal("5"), new BigDecimal("250"), new BigDecimal("10000"), 0, RiskLimits.none()
        );
        var engine = RiskEngine.withDefaultChecks();
        var result = engine.assess(context);

        assertThat(result.accepted()).isFalse();
        assertThat(result.breaches().get(0).limitType()).isEqualTo("AVAILABLE_QUANTITY_FOR_SALE");
    }

    @Test
    void rejectsExceedingMaxExposurePerAsset() {
        var limits = new RiskLimits(null, new BigDecimal("0.25"), null);
        // Existing position already worth 2000 out of a 10000 total portfolio (20%);
        // buying another 1000 would push it to 3000/10000 = 30% > 25% limit.
        var context = new RiskCheckContext(
            OrderSide.BUY, BigDecimal.TEN, new BigDecimal("1000"), new BigDecimal("8000"),
            new BigDecimal("20"), new BigDecimal("2000"), new BigDecimal("10000"), 0, limits
        );
        var engine = RiskEngine.withDefaultChecks();
        var result = engine.assess(context);

        assertThat(result.accepted()).isFalse();
        assertThat(result.breaches().get(0).limitType()).isEqualTo("MAX_EXPOSURE_PER_ASSET");
        assertThat(result.breaches().get(0).explanation()).contains("30.0%").contains("25.0%");
    }

    @Test
    void rejectsExceedingMaxOrdersPerDay() {
        var limits = new RiskLimits(null, null, 5);
        var context = new RiskCheckContext(
            OrderSide.BUY, BigDecimal.TEN, new BigDecimal("100"), new BigDecimal("100000"),
            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100000"), 5, limits
        );
        var engine = RiskEngine.withDefaultChecks();
        var result = engine.assess(context);

        assertThat(result.accepted()).isFalse();
        assertThat(result.breaches().get(0).limitType()).isEqualTo("MAX_ORDERS_PER_DAY");
    }
}
