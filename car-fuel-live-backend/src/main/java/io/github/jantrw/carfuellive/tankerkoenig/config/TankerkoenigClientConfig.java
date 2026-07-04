package io.github.jantrw.carfuellive.tankerkoenig.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Wires the dedicated HTTP client used for Tankerkönig calls.
 *
 * <p>The separate bean keeps upstream base URL and timeout settings centralized and avoids leaking
 * Tankerkönig-specific transport details into service code.
 */
@Configuration
public class TankerkoenigClientConfig {

  @Bean
  RestClient tankerkoenigApiRestClient(TankerkoenigProperties tankerkoenigProperties) {
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout((int) tankerkoenigProperties.connectTimeout().toMillis());
    requestFactory.setReadTimeout((int) tankerkoenigProperties.readTimeout().toMillis());

    return RestClient.builder()
        .baseUrl(tankerkoenigProperties.baseUrl())
        .requestFactory(requestFactory)
        .build();
  }
}
