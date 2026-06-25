package io.mywallet.risk.domain.check;

import io.mywallet.risk.domain.RiskCheck;
import io.mywallet.risk.domain.RiskCheckContext;
import io.mywallet.risk.domain.RiskCheckResult;

public class MaxOrderValueCheck implements RiskCheck {

    @Override
    public String limitType() {
        return "MAX_ORDER_VALUE";
    }

    @Override
    public RiskCheckResult evaluate(RiskCheckContext context) {
        var maxOrderValue = context.limits().maxOrderValue();
        if (maxOrderValue == null) {
            return RiskCheckResult.pass(limitType());
        }
        if (context.orderValue().compareTo(maxOrderValue) > 0) {
            return RiskCheckResult.breach(limitType(),
                "Order value %s exceeds the configured maximum order value %s".formatted(context.orderValue(), maxOrderValue));
        }
        return RiskCheckResult.pass(limitType());
    }
}
