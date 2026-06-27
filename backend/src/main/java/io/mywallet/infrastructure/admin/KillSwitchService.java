package io.mywallet.infrastructure.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class KillSwitchService {

    private final KillSwitchJpaRepository repository;

    public KillSwitchService(KillSwitchJpaRepository repository) {
        this.repository = repository;
    }

    /** Called by CreateOrderService before accepting any new order. */
    @Transactional(readOnly = true)
    public void assertTradingAllowed() {
        KillSwitchEntity killSwitch = repository.findById(KillSwitchEntity.SINGLETON_ID)
            .orElseThrow(() -> new IllegalStateException("kill_switch row missing - check V5__risk.sql seed data"));
        if (killSwitch.isEnabled()) {
            throw new KillSwitchActiveException(killSwitch.getReason());
        }
    }

    @Transactional
    public void activate(UUID activatedBy, String reason) {
        KillSwitchEntity killSwitch = repository.findById(KillSwitchEntity.SINGLETON_ID).orElseThrow();
        killSwitch.activate(activatedBy, reason);
        repository.save(killSwitch);
    }

    @Transactional
    public void deactivate() {
        KillSwitchEntity killSwitch = repository.findById(KillSwitchEntity.SINGLETON_ID).orElseThrow();
        killSwitch.deactivate();
        repository.save(killSwitch);
    }

    @Transactional(readOnly = true)
    public boolean isEnabled() {
        return repository.findById(KillSwitchEntity.SINGLETON_ID).map(KillSwitchEntity::isEnabled).orElse(false);
    }
}
