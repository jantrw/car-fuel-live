package io.github.jantrw.carfuellive.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class InMemoryGasStationRequestRateLimiterTests {

  private static final int MAX_REQUESTS_PER_WINDOW = 15;

  @Test
  void should_allowFifteenRequestsPerClientWithinOneMinute() {
    final MutableClock clock = new MutableClock(Instant.parse("2026-05-20T10:15:30Z"));
    final InMemoryGasStationRequestRateLimiter limiter =
        new InMemoryGasStationRequestRateLimiter(clock);

    for (int requestIndex = 0; requestIndex < MAX_REQUESTS_PER_WINDOW; requestIndex++) {
      assertTrue(limiter.allowRequest("127.0.0.1"));
    }

    assertFalse(limiter.allowRequest("127.0.0.1"));
  }

  @Test
  void should_resetAllowanceWhenNextMinuteStarts() {
    final MutableClock clock = new MutableClock(Instant.parse("2026-05-20T10:15:59Z"));
    final InMemoryGasStationRequestRateLimiter limiter =
        new InMemoryGasStationRequestRateLimiter(clock);

    for (int requestIndex = 0; requestIndex < MAX_REQUESTS_PER_WINDOW; requestIndex++) {
      assertTrue(limiter.allowRequest("127.0.0.1"));
    }

    assertFalse(limiter.allowRequest("127.0.0.1"));

    clock.setCurrentInstant(Instant.parse("2026-05-20T10:16:00Z"));

    assertTrue(limiter.allowRequest("127.0.0.1"));
  }

  @Test
  void should_rateLimitPerClientAddress() {
    final MutableClock clock = new MutableClock(Instant.parse("2026-05-20T10:15:30Z"));
    final InMemoryGasStationRequestRateLimiter limiter =
        new InMemoryGasStationRequestRateLimiter(clock);

    for (int requestIndex = 0; requestIndex < MAX_REQUESTS_PER_WINDOW; requestIndex++) {
      assertTrue(limiter.allowRequest("127.0.0.1"));
    }

    assertFalse(limiter.allowRequest("127.0.0.1"));
    assertTrue(limiter.allowRequest("127.0.0.2"));
  }

  private static final class MutableClock extends Clock {

    private Instant currentInstant;

    private MutableClock(Instant currentInstant) {
      this.currentInstant = currentInstant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return currentInstant;
    }

    private void setCurrentInstant(Instant currentInstant) {
      this.currentInstant = currentInstant;
    }
  }
}
