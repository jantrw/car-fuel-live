package io.github.jantrw.carfuellive.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

/**
 * Resolves the effective client IP for public rate limiting.
 *
 * <p>{@code Forwarded} and {@code X-Forwarded-For} headers are trusted only when the direct remote
 * address belongs to a configured proxy range, which avoids letting arbitrary clients spoof their
 * limiter key.
 */
@Component
public class ClientAddressResolver {

  private final List<IpAddressMatcher> trustedProxyMatchers;

  public ClientAddressResolver(TrustedProxyProperties trustedProxyProperties) {
    this.trustedProxyMatchers =
        trustedProxyProperties.trustedProxies().stream().map(IpAddressMatcher::new).toList();
  }

  public String resolveClientAddress(HttpServletRequest request) {
    final String remoteAddress = normalizeAddress(request.getRemoteAddr());
    if (!isTrustedProxy(remoteAddress)) {
      return remoteAddress;
    }

    final String forwardedAddress = extractForwardedAddress(request);
    return forwardedAddress != null ? forwardedAddress : remoteAddress;
  }

  private boolean isTrustedProxy(String remoteAddress) {
    return trustedProxyMatchers.stream().anyMatch(matcher -> matcher.matches(remoteAddress));
  }

  private static String extractForwardedAddress(HttpServletRequest request) {
    final String standardizedForwardedAddress =
        extractForwardedHeaderAddress(request.getHeader("Forwarded"));
    if (standardizedForwardedAddress != null) {
      return standardizedForwardedAddress;
    }

    return extractXForwardedForAddress(request.getHeader("X-Forwarded-For"));
  }

  private static String extractForwardedHeaderAddress(String forwardedHeader) {
    if (forwardedHeader == null || forwardedHeader.isBlank()) {
      return null;
    }

    for (String entry : forwardedHeader.split(",")) {
      for (String directive : entry.split(";")) {
        final String trimmedDirective = directive.trim();
        if (!trimmedDirective.regionMatches(true, 0, "for=", 0, 4)) {
          continue;
        }

        return sanitizeForwardedAddress(trimmedDirective.substring(4));
      }
    }

    return null;
  }

  private static String extractXForwardedForAddress(String forwardedForHeader) {
    if (forwardedForHeader == null || forwardedForHeader.isBlank()) {
      return null;
    }

    final String[] addresses = forwardedForHeader.split(",");
    return addresses.length == 0 ? null : sanitizeForwardedAddress(addresses[0]);
  }

  // Accept either raw IPs or common proxy formats such as quoted Forwarded values, bracketed
  // IPv6 literals, and IPv4 host:port pairs.
  private static String sanitizeForwardedAddress(String candidate) {
    if (candidate == null) {
      return null;
    }

    String normalized = candidate.trim();
    if (normalized.isEmpty()) {
      return null;
    }

    if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
      normalized = normalized.substring(1, normalized.length() - 1).trim();
    }

    if (normalized.startsWith("[")) {
      final int bracketEnd = normalized.indexOf(']');
      if (bracketEnd > 1) {
        normalized = normalized.substring(1, bracketEnd);
      }
    } else if (normalized.chars().filter(character -> character == ':').count() == 1) {
      normalized = normalized.substring(0, normalized.indexOf(':'));
    }

    if (normalized.isBlank()
        || normalized.equalsIgnoreCase("unknown")
        || !looksLikeIpAddress(normalized)) {
      return null;
    }

    return normalized;
  }

  private static String normalizeAddress(String address) {
    if (address == null || address.isBlank()) {
      return "unknown";
    }

    return address.trim();
  }

  // The resolver only needs a defensive plausibility check before Spring's IP matcher handles
  // trusted proxy ranges.
  private static boolean looksLikeIpAddress(String candidate) {
    return candidate.matches("[0-9.]+") || candidate.matches("[0-9a-fA-F:]+");
  }
}
