package io.mywallet.infrastructure.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * The default JWT secret in application.yml ({@code dev-only-secret-...}) is public - it's
 * sitting in the GitHub repo. Anyone who reads it can forge a valid access token for any
 * user, including ADMIN, if the running instance never overrode it. This check fails the
 * application at startup (not silently logs a warning) if that default secret is still in
 * effect on anything other than a recognized local-development profile.
 *
 * <p>Deliberately checked at {@link ApplicationReadyEvent} rather than earlier in the
 * Spring lifecycle: by then {@code Environment} reliably reflects every property source
 * (env vars, application.yml, active profiles), so there's no risk of a false negative
 * from checking too early.</p>
 */
@Component
public class JwtSecretSafetyCheck {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretSafetyCheck.class);

    /**
     * Known placeholder values that must never reach a real deployment. There are two
     * distinct ones in this repo, not one: application.yml's own {@code ${JWT_SECRET:...}}
     * fallback, and docker-compose.yml's separate {@code ${JWT_SECRET:...}} fallback for
     * when no `.env` file is present at all - the two were written independently and
     * don't share the same literal string, so both must be listed explicitly here rather
     * than assuming a single "the" default.
     */
    private static final Set<String> KNOWN_INSECURE_SECRETS = Set.of(
        "dev-only-secret-never-use-in-prod-change-me",
        "change-me-in-env-file-never-commit-a-real-secret",
        "test-only-secret-not-used-for-real-security"
    );

    /** Below this length an HMAC-SHA secret is weak regardless of which placeholder it is. */
    private static final int MINIMUM_SECRET_LENGTH = 32;

    /** Profiles where a weak/placeholder secret is tolerated - local dev and automated tests only. */
    private static final Set<String> SAFE_PROFILES = Set.of("test", "default");

    private final String configuredSecret;
    private final Environment environment;

    public JwtSecretSafetyCheck(
        @Value("${mywallet.jwt.secret}") String configuredSecret,
        Environment environment
    ) {
        this.configuredSecret = configuredSecret;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verifySecretIsNotTheInsecureDefault() {
        boolean isKnownPlaceholder = KNOWN_INSECURE_SECRETS.contains(configuredSecret);
        boolean isTooShort = configuredSecret.length() < MINIMUM_SECRET_LENGTH;

        if (!isKnownPlaceholder && !isTooShort) {
            return; // a real, sufficiently long secret is configured - nothing to check
        }

        boolean runningInSafeProfile = environment.getActiveProfiles().length == 0
            || java.util.Arrays.stream(environment.getActiveProfiles()).anyMatch(SAFE_PROFILES::contains);

        String reason = isKnownPlaceholder
            ? "it is one of this repo's known placeholder values (visible in application.yml or docker-compose.yml on GitHub)"
            : "it is only " + configuredSecret.length() + " characters long (minimum " + MINIMUM_SECRET_LENGTH + " for a safe HMAC-SHA secret)";

        if (!runningInSafeProfile) {
            String message = "REFUSING TO START: mywallet.jwt.secret is insecure - " + reason + ". "
                + "Anyone with this secret can forge valid JWTs for any user, including ADMIN. Set a real "
                + "secret via the JWT_SECRET environment variable (see .env.example: `openssl rand -base64 64`) "
                + "before starting this instance with profile(s): " + java.util.Arrays.toString(environment.getActiveProfiles());
            log.error(message);
            throw new IllegalStateException(message);
        }

        log.warn("mywallet.jwt.secret is insecure ({}) - acceptable only because the active profile(s) {} " +
            "are treated as local development. This MUST NOT reach a real deployment.",
            reason, java.util.Arrays.toString(environment.getActiveProfiles()));
    }
}
