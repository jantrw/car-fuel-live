# Security.MD

**Overview**

This document defines the essential, actionable security controls for this project — a public, no-auth web application.

**Stack:** Java 21 + Spring Boot 4.0.3, TypeScript + Vue 3, Gradle, GitHub, Docker (future), PostgreSQL 17.

---

## Assumptions / Scope

- No user authentication: all endpoints are publicly readable.
- Inputs are anonymous search queries and coordinates.
- App is internet-facing.
- The only secret is the **Tankerkönig API key**.

---

## MUST — Implement Immediately

### 1. TLS Everywhere
- Enforce HTTPS and HSTS in production.
- Use automated certificate management (e.g., Let's Encrypt).
- No plaintext HTTP for public traffic.

### 2. Server-Side Input Validation
- Validate all search parameters: length, allowed characters, coordinate ranges (lat −90/+90, lng −180/+180), radius bounds.
- Use `@Validated` and DTO constraints (`@Size`, `@Pattern`, `@DecimalMin`, `@DecimalMax`) on all Spring endpoints.

### 3. Prevent Injection
- Use parameterized queries and JPA named parameters exclusively.
- Never concatenate user input into SQL or dynamic queries.

### 4. Security Headers
Enforce at minimum:
- `Content-Security-Policy` — restrictive; allow only your own origin and known CDN sources
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`

### 5. No Secrets in Repository
- **Tankerkönig API key** stored exclusively in the `TANKERKOENIG_API_KEY` environment variable.
- Injected via Spring `@ConfigurationProperties` — never via `@Value` scattered inline.
- Never returned to the client, never logged (mask in log output), never committed.
- Enable repository secret scanning and block commits that expose secrets.

### 6. Tankerkönig API — Key Handling & Rate Limiting
- The backend proxies all Tankerkönig API calls. The Vue frontend must never contact the Tankerkönig API directly.
- Cache all Tankerkönig responses server-side with a minimum **5-minute TTL** (use Spring Cache with Caffeine or Redis).
- Deduplicate concurrent in-flight requests for the same region to prevent multiple parallel API calls for the same query.
- If a user spams searches, the cache absorbs repeated requests — do not forward every search as a live API call.
- On Tankerkönig API failure: return a structured error DTO to the client; log the full error internally; never expose API error details, status codes, or the key to the browser.
- Log API call counts to detect runaway consumption early.

### 7. CORS Configuration
This app has a clear two-origin architecture: Vue SPA on one origin, Spring Boot API on another.

- Allow **only** the frontend's exact origin (e.g., `https://your-domain.com`).
- No wildcard (`*`) — even for a public API, wildcards allow cross-site abuse.
- No credentials (no cookies, no auth headers) — `allowCredentials: false`.
- Allow only the HTTP methods and headers the frontend actually uses (`GET`, `Content-Type`).

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

Configure `app.cors.allowed-origin` per environment in `application-prod.yml` — never hardcode.

### 8. Dependency Scanning & Patching
- Enable Dependabot / Snyk / OWASP Dependency Check in CI.
- Patch critical CVEs promptly; treat unpatched critical CVEs as a build-blocking issue.

### 9. Rate Limiting & Request Size Limits
- Apply per-IP throttling (e.g., Bucket4j) on all public endpoints.
- Enforce max request-body size and max result-set size to reduce scraping and abuse.
- Apply stricter limits to the search endpoint than to static reads.

### 10. Do Not Leak Internals in Errors
- Return generic, structured error DTOs to clients.
- Log full stack traces and error details internally only.
- Never expose framework version, dependency names, or database details in error responses.

### 11. Protect Management Endpoints
- Disable or IP-whitelist all Spring Actuator/management endpoints.
- Expose only `health` and `info` if needed; disable all others in production.

### 12. Logging & Monitoring
- Centralize logs and alert on: abnormal traffic spikes, error rate increases, repeated rate-limit events, and unusual Tankerkönig API call volumes.

---

## GDPR Compliance (Mandatory for EU-Facing App)

This app uses the browser **Geolocation API**, which collects location data. Under GDPR, location data is personal data. The following controls are non-optional.

### Data minimization
- Request **low accuracy** geolocation only (`enableHighAccuracy: false`). A city-level radius is sufficient for gas price lookups; high accuracy is unnecessary and more invasive.
- Do not store user coordinates persistently. Use them only to perform the Tankerkönig query, then discard.
- Do not log IP address + coordinates together. Log them separately if at all, with no join key.

### Consent
- Display a clear, user-initiated geolocation consent prompt in the browser before calling `navigator.geolocation.getCurrentPosition()`.
- Handle permission denial gracefully: fall back to the manual city search flow without error.
- Do not infer or reconstruct location from IP address as a silent fallback.

### Data flow
- Coordinates travel: Browser → your Spring Boot backend → Tankerkönig (as a radius, not raw coordinates).
- Coordinates are **never** sent to any other third party.
- Responses from Tankerkönig are station data only — no personal data is stored alongside them.

### Privacy notice
- Provide a minimal privacy notice stating what location data is used for and that it is not stored.

---

## Client-Side Geolocation Security

- The Geolocation API only works over HTTPS (already enforced by the TLS rule above).
- Request only low accuracy (`enableHighAccuracy: false`).
- Set a sensible timeout and `maximumAge` in the geolocation options:
```typescript
navigator.geolocation.getCurrentPosition(
  onSuccess,
  onError,
  { enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 }
)
```
- Handle all error cases: `PERMISSION_DENIED`, `POSITION_UNAVAILABLE`, `TIMEOUT` — each should produce a user-friendly message and fall back to manual search.
- Never pass raw coordinates to the client response or store them in the Vue component state longer than needed for the current request.

---

## SHOULD — Plan for Next 1–2 Sprints

- Put a CDN/WAF in front of the app (e.g., Cloudflare) to mitigate DDoS and block common bots.
- Harden container images: minimal base images, run as non-root, scan with Trivy.
- Add DB and HTTP timeouts, circuit breakers (e.g., Resilience4j) around Tankerkönig API calls.
- Enforce pagination and strict caps on all heavy queries.

---

## NICE-TO-HAVE / Later

- API gateway for centralized rate limiting and logging.
- SBOM for releases.
- Behavioral bot detection.
- Cache warming: pre-populate prices for common German cities at startup.

---

## Minimal Checklist (Copy/Paste)

### Immediate (days)
- [ ] Enforce HTTPS + HSTS
- [ ] Store `TANKERKOENIG_API_KEY` in env var; enable secret scanning
- [ ] Enable dependency scanning in CI
- [ ] Implement rate limiting + max request size
- [ ] Server-side input validation + parameterized DB queries
- [ ] Add core security headers (CSP, nosniff, frame options, HSTS)
- [ ] Configure CORS: exact frontend origin only, no wildcard
- [ ] Backend-only Tankerkönig proxy; never expose key to client
- [ ] Cache Tankerkönig responses (5-min TTL); deduplicate concurrent requests
- [ ] Geolocation consent prompt; low-accuracy only; no coordinate persistence
- [ ] Do not log IP + coordinates together

### Next (1–2 sprints)
- [ ] WAF/CDN in front (DDoS and bot protection)
- [ ] Container image scanning + non-root execution
- [ ] CI gates: lint, tests, SCA, secret-scan
- [ ] Centralized logs + alerts for anomalies and Tankerkönig usage spikes
- [ ] Circuit breaker around Tankerkönig API calls (Resilience4j)
- [ ] Privacy notice page

---

*Last updated: 05.03.2026*
