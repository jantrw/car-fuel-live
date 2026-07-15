# Backend Agent Instructions
# Scope: car-fuel-live-backend/
# Root package: io.github.jantrw.carfuellive

## Stack
- Java 21, Spring Boot 4, Gradle 9, PostgreSQL 17, Flyway.

## Java And Spring
- Spotless enforces Java formatting. Do not hand-format around it.
- Apply `.codex/skills/java-best-practices/SKILL.md` when changing Java implementation details.
- Keep the public API stateless; CSRF stays disabled while there are no sessions or cookies.
- Use `@ConfigurationProperties` for grouped configuration. Do not scatter `@Value`.
- Return structured error DTOs to clients. Log internal details server-side only.

## Database
- Flyway migrations live in `src/main/resources/db/migration/` and use `V<n>__<description>.sql`.
- Never edit an applied migration; add a new migration instead.
- Seed scripts live in `src/main/resources/db/seed/` and `scripts/`; PostgreSQL is the source of truth for manual location resolution.
- Use parameterized SQL only. Never concatenate user input into SQL.
- Keep `location_countries.capital_place_geoname_id`; copied databases should retain stable country-capital references.

## Public API
- Validate request parameters at the controller boundary.
- Enforce bounded result sizes before DB-heavy or upstream-heavy work.
- Frontend never calls Tankerkonig directly; backend owns all upstream calls.
- `TANKERKOENIG_API_KEY` comes only from environment-backed backend config. Never log or return it.
- Treat Tankerkonig `ok=false` as upstream failure even when HTTP status is successful.
- Do not add long-lived fuel-price caching by default. Freshness wins unless measured pressure says otherwise.
- Rate-limit public price lookups before calling Tankerkonig.

## Privacy
- Do not persist user coordinates.
- Do not log IP address and coordinates together.
- Do not store raw coordinates on behalf of the client.

## Testing
- Test names use `should_doX_when_Y`.
- Prefer focused controller, service, repository, and client tests.
- Use `@SpringBootTest` only for full integration scenarios.
- Full backend tests may require Docker/Testcontainers.
