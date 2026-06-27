package io.mywallet.infrastructure.admin;

import io.mywallet.common.exception.DomainException;

public class KillSwitchActiveException extends DomainException {

    public KillSwitchActiveException(String reason) {
        super("Trading is currently suspended by the global kill switch" + (reason != null ? ": " + reason : ""));
    }

    @Override
    public String errorCode() {
        return "KILL_SWITCH_ACTIVE";
    }
}
