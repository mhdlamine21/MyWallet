package io.mywallet.infrastructure.admin;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KillSwitchJpaRepository extends JpaRepository<KillSwitchEntity, String> {
}
