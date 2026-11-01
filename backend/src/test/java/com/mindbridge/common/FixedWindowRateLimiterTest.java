package com.mindbridge.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTest {

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
    private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(2, Duration.ofMinutes(1), clock);

    @Test
    void allowsUpToTheLimitThenRefuses() {
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).as("other keys are independent").isTrue();
    }

    @Test
    void allowsAgainAfterTheWindowPasses() {
        limiter.tryAcquire("a");
        limiter.tryAcquire("a");
        assertThat(limiter.tryAcquire("a")).isFalse();

        clock.advance(Duration.ofMinutes(1));
        assertThat(limiter.tryAcquire("a")).isTrue();
    }
}
