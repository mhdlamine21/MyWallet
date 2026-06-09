package io.mywallet.infrastructure.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    @Test
    void allowsRequestsUpToTheLimitThenRejects() {
        var limiter = new InMemoryRateLimiter();

        assertThat(limiter.tryAcquire("ip:1.2.3.4", 3, 60_000)).isTrue();
        assertThat(limiter.tryAcquire("ip:1.2.3.4", 3, 60_000)).isTrue();
        assertThat(limiter.tryAcquire("ip:1.2.3.4", 3, 60_000)).isTrue();
        assertThat(limiter.tryAcquire("ip:1.2.3.4", 3, 60_000)).isFalse(); // 4th request in the window
    }

    @Test
    void differentKeysAreTrackedIndependently() {
        var limiter = new InMemoryRateLimiter();

        assertThat(limiter.tryAcquire("ip:1.1.1.1", 1, 60_000)).isTrue();
        assertThat(limiter.tryAcquire("ip:1.1.1.1", 1, 60_000)).isFalse();
        assertThat(limiter.tryAcquire("ip:2.2.2.2", 1, 60_000)).isTrue(); // unaffected by the other key's limit
    }

    @Test
    void allowsRequestsAgainAfterTheWindowSlidesPast() {
        var mutableClock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        var limiter = new InMemoryRateLimiter(mutableClock);

        assertThat(limiter.tryAcquire("ip:5.5.5.5", 1, 1000)).isTrue();
        assertThat(limiter.tryAcquire("ip:5.5.5.5", 1, 1000)).isFalse();

        mutableClock.advance(1100); // past the 1000ms window
        assertThat(limiter.tryAcquire("ip:5.5.5.5", 1, 1000)).isTrue();
    }

    /** A fixed-instant Clock whose instant can be advanced, for deterministic sliding-window tests. */
    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(long millis) {
            instant = instant.plusMillis(millis);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
