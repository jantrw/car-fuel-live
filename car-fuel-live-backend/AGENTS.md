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
- Always apply `.codex/skills/java-best-practices/SKILL.md` when working on Java implementation details.

### Static Analysis
- Checkstyle + SpotBugs run in CI. Both must pass before merge.

---

## DB

- Use Flyway for DB migrations. Migration scripts live in src/main/resources/db/migration/, named V<n>__<description>.sql. Never edit an existing migration — always add a new one.
- Apply `.codex/skills/spring-data-jpa/SKILL.md` for repository design, projections, query patterns, relationships, and persistence performance work.
- Persist a geocoding cache for major cities in Germany and major cities in Europe so repeated manual searches avoid redundant geocoding lookups.
- Keep the geocoding cache normalized and refreshable. Store canonical search name, country, coordinates, source, and cache metadata.

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
- Public API text stays English only. Keep OpenAPI summaries, descriptions, validation messages, and error payload text in English unless a task explicitly requires localized backend responses.

```
GET  /api/v1/gas-stations?lat=52.5&lng=13.4&radius=5&fuelType=E5
GET  /api/v1/gas-stations/{id}
```

### OpenAPI
- Use Springdoc OpenAPI.
- Swagger UI at `/swagger-ui.html` in development only.
- Annotate all public controller methods with `@Operation` and `@ApiResponse`.
- Disable it in production:
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
- Validate outbound `list.php` requests against Tankerkönig limits: `rad <= 25`, `type in {e5,e10,diesel,all}`, `sort in {price,dist}`.
- When `type=all`, treat upstream sorting as distance-based and do not rely on `price` sorting.
- Use `prices.php` for price refreshes of already-known stations. One upstream call can refresh at most 10 station IDs.
- Use `detail.php` only for selected station details such as opening times. Never use it as a price-polling method.
- Always inspect the upstream `ok` flag and `message`. Do not treat HTTP success alone as a valid Tankerkönig response.
- Model upstream response quirks explicitly:
  - `list.php` returns `price` for single-fuel queries but `e5`, `e10`, and `diesel` for `type=all`.
  - Fuel values can be `false` when a station does not offer that fuel.
  - `prices.php` can return station statuses `open`, `closed`, and `no prices`.
  - `detail.php` fields such as `openingTimes`, `overrides`, `wholeDay`, and `state` are optional.
- Persist a geocoding cache so repeated manual searches avoid redundant geocoding lookups.
- Deduplicate in-flight identical upstream requests so concurrent callers share one fresh Tankerkönig request.
- Do not rely on long-lived fuel-price result caching by default. Freshness is more important than multi-minute response caching.
- If a user spams searches, the cache absorbs it — never forward every request as a live API call.
- Only perform Tankerkönig requests on demand from user-driven flows. Avoid periodic background polling against the free API.
- Never implement bulk or mass-data style live fetching through the free API; blocked requests and disabled keys are a stated upstream risk.
- On API failure: return structured error DTO to client; log full error internally; never expose API details, status codes, or the key to the browser.
- Log API call counts to detect runaway consumption early.

### CORS
- No wildcard (`*`). Exact frontend origin only.
- Configure allowed origins via `@ConfigurationProperties`, not scattered `@Value`.
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
- Coordinates travel from backend to Tankerkönig as a radius only, never as raw lat/lng in logs.
- Client-side persistence is limited to the selected country only. Never persist raw coordinates on behalf of the client.
- Tankerkönig responses contain station data only — never store personal data alongside them.

### Logging & Monitoring
- Centralize logs; alert on traffic spikes, error rate increases, rate-limit events, and unusual Tankerkönig API call volumes.

