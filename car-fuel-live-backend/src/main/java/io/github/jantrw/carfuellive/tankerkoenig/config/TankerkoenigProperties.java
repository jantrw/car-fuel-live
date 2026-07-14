package io.github.jantrw.carfuellive.tankerkoenig.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the Tankerkönig integration.
 *
 * <p>The properties hold the upstream base URL, the API key supplied from the environment, and
 * conservative HTTP timeouts for the live-price lookup path.
 */
@ConfigurationProperties(prefix = "tankerkoenig")
public record TankerkoenigProperties(
    String baseUrl, String apiKey, Duration connectTimeout, Duration readTimeout) {

  private static final String DEFAULT_BASE_URL = "https://creativecommons.tankerkoenig.de";
  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
  private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);

  public TankerkoenigProperties {
    baseUrl = normalizeBaseUrl(baseUrl);
    apiKey = apiKey == null ? "" : apiKey;
    connectTimeout = connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
    readTimeout = readTimeout == null ? DEFAULT_READ_TIMEOUT : readTimeout;
  }

  private static String normalizeBaseUrl(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return DEFAULT_BASE_URL;
    }

    return baseUrl;
  }
}
