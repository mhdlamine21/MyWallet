package io.mywallet.risk.domain.check;

import io.mywallet.order.domain.model.OrderSide;
import io.mywallet.risk.domain.RiskCheck;
import io.mywallet.risk.domain.RiskCheckContext;
import io.mywallet.risk.domain.RiskCheckResult;

/**
 * Always active (not subject to a configurable threshold - solvency isn't optional). This
 * replaces the ad hoc check that lived directly in {@code CreateOrderService} before the
 * RiskEngine existed.
 */
public class AvailableCashForBuyCheck implements RiskCheck {

    @Override
    public String limitType() {
        return "AVAILABLE_CASH";
    }

    @Override
    public RiskCheckResult evaluate(RiskCheckContext context) {
        if (context.orderSide() != OrderSide.BUY) {
            return RiskCheckResult.pass(limitType());
        }
        if (context.orderValue().compareTo(context.availableCashBalance()) > 0) {
            return RiskCheckResult.breach(limitType(),
                "Estimated cost %s exceeds available cash balance %s"
                    .formatted(context.orderValue(), context.availableCashBalance()));
        }
        return RiskCheckResult.pass(limitType());
    }
}
