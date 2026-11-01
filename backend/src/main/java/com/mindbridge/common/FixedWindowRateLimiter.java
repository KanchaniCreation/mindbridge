package com.mindbridge.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows at most {@code limit} calls per key in each time window. In memory, so it is per
 * application instance (a known limitation, documented in the README).
 */
public final class FixedWindowRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private record Window(Instant start, int count) {
    }

    private final int limit;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.window = window;
        this.clock = clock;
    }

    /** Returns true if the call is allowed (and counts it), false if the limit is reached. */
    public boolean tryAcquire(String key) {
        Instant now = clock.instant();
        boolean[] allowed = {false};
        windows.compute(key, (k, current) -> {
            if (current == null || !now.isBefore(current.start().plus(window))) {
                allowed[0] = true;
                return new Window(now, 1);
            }
            if (current.count() < limit) {
                allowed[0] = true;
                return new Window(current.start(), current.count() + 1);
            }
            return current;
        });
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(e -> !now.isBefore(e.getValue().start().plus(window)));
        }
        return allowed[0];
    }
}
