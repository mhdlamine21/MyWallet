package io.mywallet.infrastructure.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A minimal in-memory sliding-window rate limiter - deliberately not pulling in Bucket4j
 * or another library: this project's build could never be verified against Maven Central
 * in the sandbox it was written in, so every dependency actually used had to already be in
 * the pom and provably resolvable. This is a real trade-off documented here rather than
 * hidden: an in-memory limiter does not share state across multiple backend instances
 * (fine for this project's single-node deployment target; would need a shared store -
 * Redis, already in the stack - if this ever runs behind a load balancer with more than
 * one instance).
 *
 * <p>Per-key sliding window: each call to {@link #tryAcquire} records "now" for the key
 * and evicts timestamps older than the window, then checks whether the count within the
 * window is still under the limit.</p>
 */
@Component
public class InMemoryRateLimiter {

    private final ConcurrentHashMap<String, Deque<Long>> requestTimestampsByKey = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    // package-private constructor for deterministic tests with a fixed/advanceable clock
    InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * @return true if the request is allowed (and is now counted against the window),
     *         false if the caller has exceeded {@code maxRequests} within {@code windowMillis}
     */
    public boolean tryAcquire(String key, int maxRequests, long windowMillis) {
        long now = clock.millis();
        Deque<Long> timestamps = requestTimestampsByKey.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMillis) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
