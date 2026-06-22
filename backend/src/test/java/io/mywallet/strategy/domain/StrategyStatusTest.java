package io.mywallet.strategy.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StrategyStatusTest {

    @Test
    void draftCanActivateOrDeactivate() {
        assertThat(StrategyStatus.DRAFT.canTransitionTo(StrategyStatus.ACTIVE)).isTrue();
        assertThat(StrategyStatus.DRAFT.canTransitionTo(StrategyStatus.DEACTIVATED)).isTrue();
        assertThat(StrategyStatus.DRAFT.canTransitionTo(StrategyStatus.SUSPENDED)).isFalse();
    }

    @Test
    void activeCanSuspendOrDeactivate() {
        assertThat(StrategyStatus.ACTIVE.canTransitionTo(StrategyStatus.SUSPENDED)).isTrue();
        assertThat(StrategyStatus.ACTIVE.canTransitionTo(StrategyStatus.DEACTIVATED)).isTrue();
        assertThat(StrategyStatus.ACTIVE.canTransitionTo(StrategyStatus.DRAFT)).isFalse();
    }

    @Test
    void suspendedCanReactivateOrDeactivate() {
        assertThat(StrategyStatus.SUSPENDED.canTransitionTo(StrategyStatus.ACTIVE)).isTrue();
        assertThat(StrategyStatus.SUSPENDED.canTransitionTo(StrategyStatus.DEACTIVATED)).isTrue();
    }

    @Test
    void deactivatedIsTerminal() {
        for (StrategyStatus target : StrategyStatus.values()) {
            assertThat(StrategyStatus.DEACTIVATED.canTransitionTo(target)).isFalse();
        }
    }
}
