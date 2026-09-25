# Car Fuel Live

Car Fuel Live finds current fuel prices near a manually selected place in Germany and nearby European countries. It is public and requires no account.

## Current Product

Users choose a country context, then search for a country, place, region, or German postal code. One clear, exact place or postal-code match loads nearby prices immediately. Ambiguous searches require the user to select a result.

- The app chooses the initial country in this order: saved selection, browser locale, time zone, Germany.
- The browser stores only the selected country, never coordinates.
- Search runs only after an explicit user action and uses the local PostgreSQL dataset.
- Selecting a country changes only the search context. It does not trigger a lookup from the country's centroid.
- Selecting a place or postal code uses its stored coordinates for a live lookup.
- Station lookups use a 5 km radius, all fuel types, and distance sorting.
- Results show up to 10 stations at first. Users can reveal five more from the same response without another price request.
- The interface supports German and English.

## Privacy And Security

- Browser geolocation is not implemented. Any future use must require explicit user action, request low accuracy, and remain temporary.
- The frontend never calls Tankerkoenig directly and never exposes its API key.
- The public backend is stateless and has no authentication or accounts.
- Public endpoints bound and rate-limit requests. Structured error responses omit implementation details.

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

Set `TANKERKOENIG_API_KEY` in the backend process environment to retrieve live station data. After Flyway creates the schema, seed a new database with `car-fuel-live-backend/scripts/import-location-data.ps1`.

## Verification

- Backend: `gradlew.bat -p car-fuel-live-backend spotlessCheck` and `gradlew.bat -p car-fuel-live-backend test`
- Frontend: `npm --prefix car-fuel-live-frontend run lint`, `npm --prefix car-fuel-live-frontend run test -- --run`, and `npm --prefix car-fuel-live-frontend run build`

Full backend tests require Docker for Testcontainers.
