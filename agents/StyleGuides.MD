# Style Guides

This file defines the mandatory, team-enforced style rules for the project. Do not deviate without team agreement.

---

## Stack Versions (canonical reference)

| Technology    | Version          |
|---------------|------------------|
| Java          | 21               |
| Spring Boot   | 4.0.3            |
| Vue.js        | 3.5.29           |
| TypeScript    | 5.x (latest stable) |
| Tailwind CSS  | 4.0              |
| Gradle        | 8.x (latest stable wrapper) |
| PostgreSQL    | 17-alpine (Docker image) |

---

## 1) Vue.js

**Goals:** small components, explicit contracts, strong typing, fast lint/format enforcement, comments.

### Key Rules
- Follow the official Vue Style Guide: single-responsibility components, explicit `props` validation, avoid documented anti-patterns.
- Filenames / component names: **PascalCase** in code (`MyComponent.vue`), **kebab-case** in templates (`<my-component />`).
- Prefer Composition API for all components; use `<script setup lang="ts">`.
- Keep templates simple — move all logic into `setup()` or composables.
- Composables live in `src/composables/`, prefixed with `use` (e.g., `useGasPrices.ts`).

### Tooling & Enforcement
- ESLint + `eslint-plugin-vue` + `@typescript-eslint` + Prettier + Volar.
- `eslint --fix` and Prettier run in CI and pre-commit hooks.


---

## 2) Tailwind CSS v4.0

**Goals:** utility-first styling, no custom CSS unless unavoidable, consistent design tokens, comments.

### Key Rules
- Use Tailwind utility classes exclusively. Do not write custom CSS unless a utility cannot cover the case.
- Configure design tokens (colors, spacing, typography) in `tailwind.config.ts` — do not hardcode values inline.
- Tailwind v4.0 uses a CSS-first config via `@theme` in your root CSS file; follow the v4.0 migration guide if upgrading.
- No inline `style=""` attributes for anything Tailwind can express.
- Class ordering: follow the Prettier `prettier-plugin-tailwindcss` sort order (enforced automatically).

### Tooling
- `prettier-plugin-tailwindcss` for automatic class sorting.
- Add to `.prettierrc`:
```json
{
  "plugins": ["prettier-plugin-tailwindcss"]
}
```

---

## 3) TypeScript

**Goals:** catch errors at compile time, keep types meaningful, validate all runtime input, comments.

### Key Rules
- `tsconfig.json` must enable strict mode.
- Prefer `unknown` over `any`; restrict `any` to documented exceptions with a `// eslint-disable-next-line` comment explaining why.
- Use `interface` for public shapes and DTOs; use `type` for unions or complex compositions.
- Validate all external inputs at runtime (API responses, URL params) even when TypeScript types exist.
- Never use `as unknown as X` casts without an explicit comment justifying the bypass.

### Minimal `tsconfig.json`
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "moduleResolution": "bundler",
    "esModuleInterop": true,
    "skipLibCheck": true
  }
}
```

---

## 4) Java

**Goals:** consistent formatting, short methods, clear names, automated checks, comments.

### Key Rules
- Follow Google Java Style, enforced by an automatic formatter — no manual formatting debates.
- Prefer `final` for fields that do not change after construction.
- Keep methods short and focused (single responsibility).
- Use meaningful class names (nouns) and method names (verbs).

### Tooling & Enforcement
- Formatter: **Spotless** with `google-java-format`.
- Static analysis: **Checkstyle** + **SpotBugs** in CI.

> Verify against https://plugins.gradle.org/plugin/com.diffplug.spotless before starting.

---

## 5) Spring Boot

**Goals:** predictable structure, constructor injection, thin controllers, clear config, comments.

### Key Rules
- Group by **feature/domain**, not by layer: `stations/controller`, `stations/service`, `stations/repository`.
- Use **constructor injection** for all beans. Never use field injection (`@Autowired` on fields).
- Controllers are thin — no business logic. Business logic belongs in the service layer.
- Place `@Transactional` boundaries on service methods.
- Use `@ConfigurationProperties` for grouped config — avoid scattered `@Value`.
- Centralize REST error handling with `@ControllerAdvice` and structured error DTOs.

### REST API Naming Conventions
- Base path: `/api/v1/`
- Resource names: **plural nouns** in kebab-case (e.g., `/api/v1/gas-stations`, `/api/v1/fuel-prices`)
- HTTP verbs map to actions: `GET` retrieve, `POST` create, `PUT` full update, `PATCH` partial update, `DELETE` remove
- Query parameters: camelCase (e.g., `?fuelType=E5&radius=5`)
- JSON field names: **camelCase** throughout (Jackson default; enforce via `spring.jackson.property-naming-strategy=LOWER_CAMEL_CASE`)
- Never expose database IDs in URLs if a stable natural key exists

#### Example endpoint structure
```
GET  /api/v1/gas-stations?lat=52.5&lng=13.4&radius=5&fuelType=E5
GET  /api/v1/gas-stations/{id}
```

### OpenAPI / API Documentation
- Use **Springdoc OpenAPI** for automatic API documentation.
- Dependency:
- Docs available at `/swagger-ui.html` and `/v3/api-docs` in development.
- Annotate all public controller methods with `@Operation` and `@ApiResponse`.
- Disable Swagger UI in production via:
```yaml
# application-prod.yml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

### Spring Security (No-Auth Configuration)
This is a public, stateless REST API with no authentication. Spring Security must still be configured explicitly — leaving defaults active will block requests or introduce unnecessary session overhead.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // All endpoints are public
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            // Stateless REST — no CSRF protection needed
            .csrf(csrf -> csrf.disable())
            // No sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Security headers (complement application-level headers)
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

> Do not remove or weaken this configuration. CSRF is intentionally disabled because the API is stateless and uses no cookies for auth. If cookies or sessions are ever introduced, re-enable CSRF.

### Testing
- Use slice tests (`@WebMvcTest`, `@DataJpaTest`) for controllers and repositories.
- Use `@SpringBootTest` sparingly — only for full end-to-end scenarios.
- Every new service method requires at least one unit test.

---

## 6) Test Naming Convention

Use the `should_doX_when_Y` pattern for all test method names across Java and TypeScript.

### Java (JUnit 5)
```java
@Test
void should_returnStationsWithPrices_when_validCoordinatesProvided() { ... }

@Test
void should_throwNotFoundException_when_stationIdDoesNotExist() { ... }

@Test
void should_returnEmptyList_when_noStationsInRadius() { ... }
```

### TypeScript / Vue (Vitest)
```typescript
describe('useGasPrices', () => {
  it('should return stations when valid coordinates are provided', () => { ... })
  it('should return empty array when no stations are in radius', () => { ... })
  it('should throw error when API call fails', () => { ... })
})
```

**Rules:**
- No abbreviations in test names.
- Test names must be self-documenting — a failing test name alone should identify the problem.
- Arrange / Act / Assert structure inside every test body, separated by blank lines.

## CI & Repo-Wide Rules (Mandatory)

- **Formatter-first:** Prettier (JS/TS/Vue) and Spotless (Java) run on commit and in CI. Formatting failures block merges.
- **Lint gate:** ESLint and Checkstyle must pass on all PRs.
- **Tests:** Unit tests required for all new logic. Services require at least one integration or slice test.
- **Docs:** Update `documentation.MD` and `architecture.md` on any feature or API change.
- **Change process:** Any change to these rules requires explicit team approval.

### Typical CI Commands
```bash
# JS / TS / Vue
npm run format
npm run lint
npm test
-> type in the tested file

# Java (Gradle)
./gradlew spotlessApply
./gradlew check
./gradlew test
```

---

*Last updated: 05.03.2026*
