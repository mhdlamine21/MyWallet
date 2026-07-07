package io.mywallet.infrastructure.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Wires Spring Data JPA's {@code @CreatedBy}/{@code @LastModifiedBy} to the currently
 * authenticated actor. Until Phase 3 (auth) lands, {@link #currentAuditor()} has nothing
 * to read from the security context and returns empty - audit columns are simply null for
 * anything persisted before authentication exists, which is expected and temporary.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            // Phase 3 sets the principal's name to the user's UUID string once JWT auth
            // is wired up. Until then this branch is unreachable in practice.
            try {
                return Optional.of(UUID.fromString(authentication.getName()));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        };
    }
}
