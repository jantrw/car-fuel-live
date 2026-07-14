package io.github.jantrw.carfuellive.common.config;

import io.github.jantrw.carfuellive.common.security.GasStationRateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Declares the backend security baseline for the public API.
 *
 * <p>The chain stays stateless, disables interactive login mechanisms, and inserts the fuel-price
 * rate-limit filter before request authorization.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  // Keep development behavior aligned with the product contract: public, stateless, no login
  // flow.
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, GasStationRateLimitFilter gasStationRateLimitFilter) throws Exception {
    return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .rememberMe(AbstractHttpConfigurer::disable)
        .addFilterBefore(gasStationRateLimitFilter, AuthorizationFilter.class)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            headers ->
                headers
                    .frameOptions(frame -> frame.deny())
                    .contentTypeOptions(Customizer.withDefaults()))
        .build();
  }
}
