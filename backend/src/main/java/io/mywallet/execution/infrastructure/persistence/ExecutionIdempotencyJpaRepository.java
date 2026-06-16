package io.mywallet.execution.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionIdempotencyJpaRepository extends JpaRepository<ExecutionIdempotencyEntity, String> {
}
