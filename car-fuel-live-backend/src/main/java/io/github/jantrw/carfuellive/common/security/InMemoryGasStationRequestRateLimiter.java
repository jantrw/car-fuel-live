package io.github.jantrw.carfuellive.common.security;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Fixed-window in-memory limiter for the public fuel-price endpoint.
 *
 * <p>The limiter is intentionally simple for this slice: one counter per normalized client address
 * and minute window, plus opportunistic cleanup to keep the map bounded.
 */
@Component
public class InMemoryGasStationRequestRateLimiter implements GasStationRequestRateLimiter {

  private static final long WINDOW_SECONDS = 60;
  private static final int MAX_REQUESTS_PER_WINDOW = 15;
  private static final long RETAINED_WINDOWS = 10;
  private static final long CLEANUP_INTERVAL = 128;

  private final ConcurrentMap<String, RequestWindow> requestWindows = new ConcurrentHashMap<>();
  private final AtomicLong observedRequests = new AtomicLong();
  private final Clock clock;

  public InMemoryGasStationRequestRateLimiter() {
    this(Clock.systemUTC());
  }

  InMemoryGasStationRequestRateLimiter(Clock clock) {
    this.clock = clock;
  }

  @Override
  public boolean allowRequest(String clientAddress) {
    final long currentWindow = currentWindow();
    cleanupExpiredWindows(currentWindow);

    final AtomicBoolean allowed = new AtomicBoolean(false);
    requestWindows.compute(
        normalizeClientAddress(clientAddress),
        (_clientKey, currentState) -> nextState(currentState, currentWindow, allowed));
    return allowed.get();
  }

  private RequestWindow nextState(
      RequestWindow currentState, long currentWindow, AtomicBoolean allowed) {
    if (currentState == null || currentState.windowId() != currentWindow) {
      allowed.set(true);
      return new RequestWindow(currentWindow, 1);
    }

    if (currentState.requestCount() >= MAX_REQUESTS_PER_WINDOW) {
      allowed.set(false);
      return currentState;
    }

    allowed.set(true);
    return new RequestWindow(currentWindow, currentState.requestCount() + 1);
  }

  private void cleanupExpiredWindows(long currentWindow) {
    if (observedRequests.incrementAndGet() % CLEANUP_INTERVAL != 0) {
      return;
    }

    requestWindows
        .entrySet()
        .removeIf(entry -> currentWindow - entry.getValue().windowId() >= RETAINED_WINDOWS);
  }

  private long currentWindow() {
    return Instant.now(clock).getEpochSecond() / WINDOW_SECONDS;
  }

  private static String normalizeClientAddress(String clientAddress) {
    if (clientAddress == null || clientAddress.isBlank()) {
      return "unknown";
    }

    return clientAddress.trim();
  }

  /** Immutable limiter state for one client address and one fixed time window. */
  private record RequestWindow(long windowId, int requestCount) {}
}
