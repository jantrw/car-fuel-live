package io.github.jantrw.carfuellive.common.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable list of trusted reverse-proxy address ranges.
 *
 * <p>Only requests coming through one of these ranges may influence client-IP resolution via
 * forwarding headers.
 */
@ConfigurationProperties(prefix = "app.security")
public record TrustedProxyProperties(List<String> trustedProxies) {

  public TrustedProxyProperties {
    trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
  }
}
