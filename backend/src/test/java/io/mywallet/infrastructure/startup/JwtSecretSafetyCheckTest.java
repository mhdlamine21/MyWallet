package io.mywallet.infrastructure.startup;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class JwtSecretSafetyCheckTest {

    @Test
    void refusesToStartWithKnownPlaceholderSecretOutsideSafeProfiles() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("docker", "seed");
        var check = new JwtSecretSafetyCheck("dev-only-secret-never-use-in-prod-change-me", env);

        assertThatThrownBy(check::verifySecretIsNotTheInsecureDefault).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesToStartWithTheDockerComposeSpecificPlaceholder() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("demo");
        var check = new JwtSecretSafetyCheck("change-me-in-env-file-never-commit-a-real-secret", env);

        assertThatThrownBy(check::verifySecretIsNotTheInsecureDefault).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesToStartWithAnyTooShortSecretEvenIfNotAKnownPlaceholder() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("demo");
        var check = new JwtSecretSafetyCheck("short", env);

        assertThatThrownBy(check::verifySecretIsNotTheInsecureDefault).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void allowsThePlaceholderInTheDefaultProfileForLocalDevelopment() {
        MockEnvironment env = new MockEnvironment(); // no active profiles set -> "default"
        var check = new JwtSecretSafetyCheck("dev-only-secret-never-use-in-prod-change-me", env);

        assertThatNoException().isThrownBy(check::verifySecretIsNotTheInsecureDefault);
    }

    @Test
    void allowsThePlaceholderInTheTestProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        var check = new JwtSecretSafetyCheck("test-only-secret-not-used-for-real-security", env);

        assertThatNoException().isThrownBy(check::verifySecretIsNotTheInsecureDefault);
    }

    @Test
    void allowsARealLongSecretInAnyProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("demo");
        String realSecret = "a".repeat(64); // simulates a real openssl-generated secret
        var check = new JwtSecretSafetyCheck(realSecret, env);

        assertThatNoException().isThrownBy(check::verifySecretIsNotTheInsecureDefault);
    }
}
