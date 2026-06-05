package io.mywallet.user.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityLockoutTest {

    @Test
    void isNotLockedByDefault() {
        UserEntity user = new UserEntity(UUID.randomUUID(), "a@b.com", "hash");
        assertThat(user.isLocked(Instant.now())).isFalse();
    }

    @Test
    void locksAfterFiveConsecutiveFailedAttempts() {
        UserEntity user = new UserEntity(UUID.randomUUID(), "a@b.com", "hash");
        Instant now = Instant.now();

        for (int i = 0; i < 4; i++) {
            user.recordFailedLogin(now);
            assertThat(user.isLocked(now)).isFalse();
        }
        user.recordFailedLogin(now); // 5th failure
        assertThat(user.isLocked(now)).isTrue();
    }

    @Test
    void unlockAutomaticallyOnceTheLockoutWindowPasses() {
        UserEntity user = new UserEntity(UUID.randomUUID(), "a@b.com", "hash");
        Instant now = Instant.now();
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(now);
        }
        assertThat(user.isLocked(now)).isTrue();
        assertThat(user.isLocked(now.plusSeconds(15 * 60 + 1))).isFalse();
    }

    @Test
    void successfulLoginResetsTheFailureCounter() {
        UserEntity user = new UserEntity(UUID.randomUUID(), "a@b.com", "hash");
        Instant now = Instant.now();
        user.recordFailedLogin(now);
        user.recordFailedLogin(now);
        user.recordFailedLogin(now);

        user.recordSuccessfulLogin();

        // Now it takes another full 5 failures to lock, not just 2 more.
        user.recordFailedLogin(now);
        user.recordFailedLogin(now);
        assertThat(user.isLocked(now)).isFalse();
    }
}
