package com.mindbridge.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Blocks a (email, IP) pair after too many failed logins within a time window.
 * In-memory on purpose: simple and enough for one instance. A multi-instance
 * deployment would need a shared store (documented as a known limitation).
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private record Window(Instant start, int failures) {
    }

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public LoginRateLimiter(Clock clock) {
        this(5, Duration.ofMinutes(15), clock);
    }

    public LoginRateLimiter(int maxFailures, Duration window, Clock clock) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        Window current = windows.get(key);
        if (current == null) {
            return false;
        }
        if (isExpired(current, clock.instant())) {
            windows.remove(key, current);
            return false;
        }
        return current.failures() >= maxFailures;
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        windows.compute(key, (k, current) -> current == null || isExpired(current, now)
            ? new Window(now, 1)
            : new Window(current.start(), current.failures() + 1));
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        }
    }

    public void reset(String key) {
        windows.remove(key);
    }

    private boolean isExpired(Window w, Instant now) {
        return !now.isBefore(w.start().plus(window));
    }
}
