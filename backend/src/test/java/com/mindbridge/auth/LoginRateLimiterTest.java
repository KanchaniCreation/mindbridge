package com.mindbridge.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    static class TestClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final TestClock clock = new TestClock();
    private final LoginRateLimiter limiter = new LoginRateLimiter(3, Duration.ofMinutes(15), clock);

    @Test
    void blocksAfterMaxFailures() {
        limiter.recordFailure("a");
        limiter.recordFailure("a");
        assertThat(limiter.isBlocked("a")).isFalse();

        limiter.recordFailure("a");
        assertThat(limiter.isBlocked("a")).isTrue();
        assertThat(limiter.isBlocked("someone-else")).isFalse();
    }

    @Test
    void successfulLoginResetsTheCounter() {
        limiter.recordFailure("a");
        limiter.recordFailure("a");
        limiter.reset("a");
        limiter.recordFailure("a");

        assertThat(limiter.isBlocked("a")).isFalse();
    }

    @Test
    void blockExpiresAfterTheWindow() {
        limiter.recordFailure("a");
        limiter.recordFailure("a");
        limiter.recordFailure("a");
        assertThat(limiter.isBlocked("a")).isTrue();

        clock.advance(Duration.ofMinutes(16));
        assertThat(limiter.isBlocked("a")).isFalse();
    }
}
