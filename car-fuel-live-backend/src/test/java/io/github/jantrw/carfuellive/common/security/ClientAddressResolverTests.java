package io.github.jantrw.carfuellive.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientAddressResolverTests {

  @Test
  void should_useRemoteAddress_when_noTrustedProxyMatches() {
    final ClientAddressResolver resolver = new ClientAddressResolver(trustedProxyProperties());
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("198.51.100.2");
    request.addHeader("X-Forwarded-For", "203.0.113.10");

    assertEquals("198.51.100.2", resolver.resolveClientAddress(request));
  }

  @Test
  void should_useFirstForwardedAddress_when_requestComesFromTrustedProxy() {
    final ClientAddressResolver resolver =
        new ClientAddressResolver(trustedProxyProperties("127.0.0.1/32"));
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    request.addHeader("X-Forwarded-For", "203.0.113.10, 127.0.0.1");

    assertEquals("203.0.113.10", resolver.resolveClientAddress(request));
  }

  @Test
  void should_preferStandardizedForwardedHeader_when_requestComesFromTrustedProxy() {
    final ClientAddressResolver resolver =
        new ClientAddressResolver(trustedProxyProperties("127.0.0.1/32"));
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    request.addHeader("Forwarded", "for=\"[2001:db8::9]:4711\";proto=https");
    request.addHeader("X-Forwarded-For", "203.0.113.10");

    assertEquals("2001:db8::9", resolver.resolveClientAddress(request));
  }

  @Test
  void should_fallBackToRemoteAddress_when_forwardedHeaderIsNotUsable() {
    final ClientAddressResolver resolver =
        new ClientAddressResolver(trustedProxyProperties("127.0.0.1/32"));
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    request.addHeader("Forwarded", "for=unknown");

    assertEquals("127.0.0.1", resolver.resolveClientAddress(request));
  }

  private static TrustedProxyProperties trustedProxyProperties(String... trustedProxies) {
    return new TrustedProxyProperties(java.util.List.of(trustedProxies));
  }
}
