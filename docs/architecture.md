## Architecture Direction

```text
Browser (Vue 3 SPA)
  -> Locale/time zone bootstrap (browser locale + Intl time zone -> country fallback, default Germany)
  -> localStorage (selected country only)
  -> Spring Boot REST API (/api/v1/...)
       -> Tankerkönig API (server-side only, API key never leaves backend)
       -> PostgreSQL 17 (seeded Europe place dataset, German postal codes, persisted geocoding cache)
```

## Boundary Rules

- The backend is a public, stateless, no-auth API.
- The frontend derives the first-visit country fallback from browser locale and time zone only. Do not use IP geolocation.
- If browser locale has no region, default the first-visit country to Germany.
- The frontend may persist only the selected country in `localStorage`. Never persist raw coordinates.
- Browser geolocation is opt-in only through an explicit `Use my city` action. Never trigger geolocation automatically on page load.
- Browser geolocation should use low accuracy only. City-level precision is sufficient.
- The frontend should show a minimal privacy notice explaining that location is used for the current request and not stored.
- The Vue frontend never contacts Tankerkönig directly.
- The Spring Boot backend proxies all Tankerkönig access.
- The Tankerkönig API key must never leave the backend.
- Prefer a persisted geocoding cache for repeated manual searches.
- Deduplicate identical in-flight upstream requests so concurrent requests share one fresh upstream fetch.
- Do not rely on long-lived fuel-price result caching by default. Freshness is more important than multi-minute caching.
- Frontend user-facing text must support German (`de`) and English (`en`).
- Backend/API text, OpenAPI content, validation messages, and server-managed messages stay English unless a task explicitly requires backend localization.
