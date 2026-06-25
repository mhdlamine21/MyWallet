package io.mywallet.risk.domain.check;

import io.mywallet.order.domain.model.OrderSide;
import io.mywallet.risk.domain.RiskCheck;
import io.mywallet.risk.domain.RiskCheckContext;
import io.mywallet.risk.domain.RiskCheckResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Projects what the asset's exposure (position value / total portfolio value) would
 * become if this order filled entirely, and rejects if that projected exposure exceeds
 * the configured limit. Deliberately projects <em>forward</em> rather than checking
 * current exposure - the whole point is to stop an order that would breach the limit,
 * not just flag it after the fact.
 */
public class MaxExposurePerAssetCheck implements RiskCheck {

    @Override
    public String limitType() {
        return "MAX_EXPOSURE_PER_ASSET";
    }

    @Override
    public RiskCheckResult evaluate(RiskCheckContext context) {
        BigDecimal maxFraction = context.limits().maxExposurePerAssetFraction();
        if (maxFraction == null || context.totalPortfolioValue().signum() == 0) {
            return RiskCheckResult.pass(limitType());
        }

        BigDecimal projectedPositionValue = context.orderSide() == OrderSide.BUY
            ? context.existingPositionValue().add(context.orderValue())
            : context.existingPositionValue().subtract(context.orderValue()).max(BigDecimal.ZERO);

        BigDecimal projectedExposure = projectedPositionValue.divide(context.totalPortfolioValue(), 4, RoundingMode.HALF_UP);

        if (projectedExposure.compareTo(maxFraction) > 0) {
            return RiskCheckResult.breach(limitType(),
                "Projected exposure would be %s%%, configured limit is %s%%"
                    .formatted(toPercent(projectedExposure), toPercent(maxFraction)));
        }
        return RiskCheckResult.pass(limitType());
    }

    private BigDecimal toPercent(BigDecimal fraction) {
        return fraction.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
    }
}
