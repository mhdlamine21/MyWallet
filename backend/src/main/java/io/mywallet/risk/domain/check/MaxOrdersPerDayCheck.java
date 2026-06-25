package io.mywallet.risk.domain.check;

import io.mywallet.risk.domain.RiskCheck;
import io.mywallet.risk.domain.RiskCheckContext;
import io.mywallet.risk.domain.RiskCheckResult;

public class MaxOrdersPerDayCheck implements RiskCheck {

    @Override
    public String limitType() {
        return "MAX_ORDERS_PER_DAY";
    }

    @Override
    public RiskCheckResult evaluate(RiskCheckContext context) {
        Integer maxOrders = context.limits().maxOrdersPerDay();
        if (maxOrders == null) {
            return RiskCheckResult.pass(limitType());
        }
        if (context.ordersPlacedToday() >= maxOrders) {
            return RiskCheckResult.breach(limitType(),
                "Already placed %d orders today, configured daily maximum is %d"
                    .formatted(context.ordersPlacedToday(), maxOrders));
        }
        return RiskCheckResult.pass(limitType());
    }
}
