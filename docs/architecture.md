## Data Flow

```text
Browser (Vue 3 SPA)
  -> Spring Boot REST API (/api/v1/...)
       -> Tankerkönig API (server-side only, API key never leaves backend)
       -> PostgreSQL 17 (response cache and optional persistence)
```

## Boundary Rules

- The Vue frontend never contacts Tankerkönig directly.
- The Spring Boot backend proxies all Tankerkönig access.
- The Tankerkönig API key must never leave the backend.
