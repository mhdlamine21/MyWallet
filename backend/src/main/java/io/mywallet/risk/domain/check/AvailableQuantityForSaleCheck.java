package io.mywallet.risk.domain.check;

import io.mywallet.order.domain.model.OrderSide;
import io.mywallet.risk.domain.RiskCheck;
import io.mywallet.risk.domain.RiskCheckContext;
import io.mywallet.risk.domain.RiskCheckResult;

/** Always active - cannot sell more of an asset than is actually held. */
public class AvailableQuantityForSaleCheck implements RiskCheck {

    @Override
    public String limitType() {
        return "AVAILABLE_QUANTITY_FOR_SALE";
    }

    @Override
    public RiskCheckResult evaluate(RiskCheckContext context) {
        if (context.orderSide() != OrderSide.SELL) {
            return RiskCheckResult.pass(limitType());
        }
        if (context.orderQuantity().compareTo(context.existingPositionQuantity()) > 0) {
            return RiskCheckResult.breach(limitType(),
                "Cannot sell %s units - only %s currently held"
                    .formatted(context.orderQuantity(), context.existingPositionQuantity()));
        }
        return RiskCheckResult.pass(limitType());
    }
}
