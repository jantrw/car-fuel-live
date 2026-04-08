## Data Flow

```text
Browser (Vue 3 SPA)
  -> Locale/time zone bootstrap (browser locale + Intl time zone -> country fallback, default Germany)
  -> localStorage (selected country only)
  -> Spring Boot REST API (/api/v1/...)
       -> Tankerkönig API (server-side only, API key never leaves backend)
       -> PostgreSQL 17 (response cache and optional persistence)
```

## Boundary Rules

- The frontend derives the first-visit country fallback from browser locale and time zone only. Do not use IP geolocation.
- If browser locale has no region, default the first-visit country to Germany.
- The frontend may persist only the selected country in `localStorage`. Never persist raw coordinates.
- Browser geolocation is opt-in only through an explicit `Use my city` action. Never trigger geolocation automatically on page load.
- The Vue frontend never contacts Tankerkönig directly.
- The Spring Boot backend proxies all Tankerkönig access.
- The Tankerkönig API key must never leave the backend.
