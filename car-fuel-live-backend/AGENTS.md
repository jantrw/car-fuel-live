# Backend Agent Instructions
# Scope: car-fuel-live-backend/
# Root package: io.github.jantrw.carfuellive

---

## Stack
| Technology   | Version                     |
|--------------|-----------------------------|
| Java         | 21                          |
| Spring Boot  | 4.0.3                       |
| Gradle       | 9.3.1 |
| PostgreSQL   | 17-alpine                   |

## Java Style

- Follow Google Java Style — enforced by Spotless, no manual formatting debates.
- Prefer `final` for fields that do not change after construction.
- Keep methods short and focused (single responsibility).
- Meaningful class names (nouns), method names (verbs).
- No abbreviations.

### Static Analysis
- Checkstyle + SpotBugs run in CI. Both must pass before merge.

---

## DB

- Use Flyway for DB migrations. Migration scripts live in src/main/resources/db/migration/, named V<n>__<description>.sql. Never edit an existing migration — always add a new one.

---

## Spring Boot Style

- Group by **feature/domain**, never by layer:
  ```
  stations/controller/
  stations/service/
  stations/repository/
  stations/model/
  stations/dto/
  tankerkoenig/
  common/exception/
  common/validation/
  ```
- **Constructor injection** for all beans. Never use field injection (`@Autowired` on fields).
- Controllers are thin — no business logic. All logic belongs in the service layer.
- `@Transactional` boundaries on service methods, not controllers.
- Use `@ConfigurationProperties` for grouped config. Avoid scattered `@Value`.
- Centralize REST error handling with `@ControllerAdvice` and structured error DTOs.

### REST API Naming
- Base path: `/api/v1/`
- Resource names: plural nouns, kebab-case (e.g., `/api/v1/gas-stations`)
- Query parameters: camelCase (e.g., `?fuelType=E5&radius=5`)
- JSON fields: camelCase (Jackson default — enforce via `spring.jackson.property-naming-strategy=LOWER_CAMEL_CASE`)
- Never expose raw database IDs in URLs if a stable natural key exists

```
GET  /api/v1/gas-stations?lat=52.5&lng=13.4&radius=5&fuelType=E5
GET  /api/v1/gas-stations/{id}
```

### OpenAPI
- Use Springdoc OpenAPI:
```
- Swagger UI at `/swagger-ui.html` in dev only.
- Annotate all public controller methods with `@Operation` and `@ApiResponse`.
- Disable in production:
```yaml
# application-prod.yml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

### Spring Security
This is a stateless, no-auth public API. Configure explicitly — never leave Spring Security defaults active.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts ->
                    hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
            );
        return http.build();
    }
}
```

CSRF is intentionally disabled — API is stateless, no cookies. If sessions are ever introduced, re-enable CSRF immediately.

---

## Testing

- Slice tests (`@WebMvcTest`, `@DataJpaTest`) for controllers and repositories.
- `@SpringBootTest` only for full end-to-end scenarios — use sparingly.
- Every new service method requires at least one unit test.
- Always proof functionality.
- Every task needs to be tested before finishing it.

### Test Naming Convention
Pattern: `should_doX_when_Y`

```java
@Test
void should_returnStationsWithPrices_when_validCoordinatesProvided() { ... }

@Test
void should_throwNotFoundException_when_stationIdDoesNotExist() { ... }

@Test
void should_returnEmptyList_when_noStationsInRadius() { ... }
```

- No abbreviations in test names.
- Failing test name alone must identify the problem.
- Arrange / Act / Assert inside every test body, separated by blank lines.

---

## Security

### Input Validation
- Validate all search parameters: length, allowed characters, coordinate ranges (lat −90/+90, lng −180/+180), radius bounds.
- Use `@Validated` + DTO constraints (`@Size`, `@Pattern`, `@DecimalMin`, `@DecimalMax`) on all endpoints.

### Injection Prevention
- Parameterized queries and JPA named parameters exclusively.
- Never concatenate user input into SQL or dynamic queries.

### Security Headers
Enforce via Spring Security config and/or reverse proxy:
- `Content-Security-Policy` — restrictive; own origin + known CDN sources only
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`

### Secrets
- `TANKERKOENIG_API_KEY` stored exclusively in environment variable.
- Injected via `@ConfigurationProperties` — never `@Value` inline, never committed, never logged, never returned to client.
- Enable repository secret scanning; block commits that expose secrets.

### Tankerkönig API Proxy
- The backend proxies **all** Tankerkönig API calls. The frontend never contacts Tankerkönig directly.
- Cache all responses server-side: minimum **5-minute TTL** (Spring Cache + Caffeine or Redis).
- Deduplicate concurrent in-flight requests for the same region.
- If a user spams searches, the cache absorbs it — never forward every request as a live API call.
- On API failure: return structured error DTO to client; log full error internally; never expose API details, status codes, or the key to the browser.
- Log API call counts to detect runaway consumption early.

### CORS
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigin)
            .allowedMethods("GET")
            .allowedHeaders("Content-Type")
            .allowCredentials(false)
            .maxAge(3600);
    }
}
```
- No wildcard (`*`). Exact frontend origin only.
- `app.cors.allowed-origin` set per environment in `application-prod.yml` — never hardcoded.

### Rate Limiting
- Per-IP throttling via Bucket4j on all public endpoints.
- Stricter limits on the search endpoint than on static reads.
- Enforce max request-body size and max result-set size.

### Error Handling
- Return generic, structured error DTOs to clients.
- Log full stack traces internally only.
- Never expose framework version, dependency names, or DB details in error responses.

### Management Endpoints
- Disable or IP-whitelist all Actuator endpoints.
- Expose only `health` and `info` if needed; disable all others in production.

### GDPR — Backend Rules
- Do not store user coordinates persistently. Use for Tankerkönig query, then discard.
- Do not log IP address + coordinates together. No join key between them.
- Coordinates travel: Backend → Tankerkönig as a radius only, never as raw lat/lng in logs.
- Tankerkönig responses contain station data only — never store personal data alongside them.

### Logging & Monitoring
- Centralize logs; alert on traffic spikes, error rate increases, rate-limit events, and unusual Tankerkönig API call volumes.

