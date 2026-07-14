package io.github.jantrw.carfuellive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Bootstraps the Spring Boot backend for the public fuel-price API.
 *
 * <p>The application scans grouped configuration properties and deliberately excludes Spring
 * Security's default in-memory user setup because this product slice is a stateless no-auth API.
 */
// The project is a public no-auth API, so the default in-memory user setup must stay disabled.
@ConfigurationPropertiesScan
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }
}
