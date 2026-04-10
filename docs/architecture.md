## Architecture Direction

```text
Browser (Vue 3 SPA)
  -> Locale/time zone bootstrap (browser locale + Intl time zone -> country fallback, default Germany)
  -> localStorage (selected country only)
  -> Location search UI (manual query + autocomplete suggestions for cities, regions, countries)
  -> Spring Boot REST API (/api/v1/...)
       -> PostgreSQL 17 (seeded location dataset, persisted geocoding cache, suggestion lookup)
       -> Live geocoding provider (fallback only for locations missing from PostgreSQL)
       -> Tankerkönig API (server-side only, API key never leaves backend)
```

## Boundary Rules

- The backend is a public, stateless, no-auth API.
- The frontend derives the first-visit country fallback from browser locale and time zone only. Do not use IP geolocation.
- If browser locale has no region, default the first-visit country to Germany.
- The frontend may persist only the selected country in `localStorage`. Never persist raw coordinates.
- Browser geolocation is opt-in only through an explicit `Use my city` action. Never trigger geolocation automatically on page load.
- Browser geolocation should use low accuracy only. City-level precision is sufficient.
- The frontend should show a minimal privacy notice explaining that location is used for the current request and not stored.
- Manual search should use a suggestion flow that returns matching cities, regions, and countries for partial inputs such as `Be`, including results such as `Berlin`, `Bern`, and `Belgium`.
- The Vue frontend never contacts Tankerkönig directly.
- The Spring Boot backend proxies all Tankerkönig access.
- The Tankerkönig API key must never leave the backend.
- Prefer a persisted geocoding cache for repeated manual searches.
- Reuse the geocoding cache to support frequent location suggestion lookups before falling back to a live geocoding source.
- Deduplicate identical in-flight upstream requests so concurrent requests share one fresh upstream fetch.
- Do not rely on long-lived fuel-price result caching by default. Freshness is more important than multi-minute caching.
- Frontend user-facing text must support German (`de`) and English (`en`).
- Backend/API text, OpenAPI content, validation messages, and server-managed messages stay English unless a task explicitly requires backend localization.

## Location Resolution Strategy

1. The backend keeps PostgreSQL as the primary location lookup store for manual search.
2. PostgreSQL is seeded from GeoNames with European countries, European administrative/place rows, and German postal codes so the normal search path stays local and fast.
3. Stored location rows must contain normalized search text, aliases, country code, optional admin codes, coordinates, source metadata, and ranking fields.
4. Manual search suggestions query PostgreSQL first and return ranked matches for short partial inputs. Use normalized prefix-friendly search and suitable indexes so autocomplete remains low latency.
5. Selecting a cached city or region returns stored coordinates directly for the next Tankerkönig request.
6. Selecting a cached country changes the frontend country context and loads that country's default major-city result set instead of querying Tankerkönig with a country centroid.
7. If a submitted location is missing from PostgreSQL, the backend performs one live geocoding lookup, stores the normalized result, and reuses it for later searches.
8. Live geocoding is fallback only. It must use short timeouts and in-flight deduplication so cache misses do not create long waits or duplicate upstream calls.
