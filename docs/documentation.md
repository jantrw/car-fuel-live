# Car Fuel Live

Car Fuel Live is a public, no-login application for finding current fuel prices near a manually selected place in Germany and nearby European countries.

## Current Product

Users select a country context and search locally for a country, place, region, or German postal code. A single clear exact place or postal-code match loads nearby live station prices directly; otherwise users select a concrete result.

- The initial country comes from a saved selection, browser locale, time zone, then Germany.
- The selected country is the only browser-persisted value. Coordinates are never persisted.
- Search starts only after an explicit user action and is resolved from the local PostgreSQL dataset.
- Country selection changes the search context only. It does not use a country centroid for a price lookup.
- Place and postal-code selection use their stored coordinates for a live lookup.
- The current station lookup uses a 5 km radius, all fuel types, and distance order.
- A station result initially shows up to 10 stations; users can explicitly reveal five more from the current response without another price request.
- The interface supports German and English.

## Privacy And Security

- Browser geolocation is not implemented. Any future use must be explicitly user-triggered, low accuracy, and temporary.
- The frontend never calls Tankerkoenig directly and never exposes its API key.
- The public backend is stateless and has no authentication or accounts.
- Public endpoints use bounded requests and rate limiting. Errors use a structured response without implementation details.

## Planned Work

- Country-based default results for major cities.
- Opt-in "Use my city" flow.
- Fuel type, distance, and price-order controls.
- Station detail pages.
- Rate limiting for the remaining public endpoints.

## Local Development

1. Copy `car-fuel-live-backend/.env.example` to `.env` and set database values.
2. Start PostgreSQL with the backend `docker-compose.yml`.
3. Start the backend with `gradlew.bat -p car-fuel-live-backend bootRun`.
4. Start the frontend with `npm --prefix car-fuel-live-frontend run dev`.

For live station data, set `TANKERKOENIG_API_KEY` in the backend process environment. Restore a checksummed database baseline to reproduce a released dataset. Use `car-fuel-live-backend/scripts/import-location-data.ps1` after Flyway has created the schema only for an intentional refresh from the current rolling GeoNames files; it does not reproduce an existing release.

## Verification

- Backend: `gradlew.bat -p car-fuel-live-backend spotlessCheck` and `gradlew.bat -p car-fuel-live-backend test`
- Frontend: `npm --prefix car-fuel-live-frontend run lint`, `npm --prefix car-fuel-live-frontend run test -- --run`, and `npm --prefix car-fuel-live-frontend run build`

Full backend tests require Docker for Testcontainers.
