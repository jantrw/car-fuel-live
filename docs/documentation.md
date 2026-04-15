## Application Overview

**Name:** Car Fuel Live (working title)
**Purpose:** A public, no-auth web application that displays real-time fuel prices across Germany and nearby European countries for a user's chosen city or country context.
**Document role:** Agent-facing planning and product reference. This file captures intended behavior and durable requirements before implementation starts.

---

## Planned Product Behavior

- On first visit, the app derives an initial country fallback from the browser locale and time zone.
- If the browser locale does not include a region, the default country is Germany.
- If the user does not share their city, the app displays gas prices for major cities in the selected country.
- The selected country is stored in `localStorage` so later visits can immediately load country-based results.
- Users can explicitly choose `Use my city`. Only then does the app request browser geolocation and display gas prices near the detected city.
- Users can manually enter a country, city, region, place, or German postal code. The backend resolves matching text to longitude and latitude from PostgreSQL before any Tankerkönig lookup for coordinate-based searches.
- Manual search must provide autocomplete suggestions while the user types and show matching countries, cities, regions, places, and German postal codes.
- The manual search suggestion list should appear after short partial inputs such as `Be` and show matching results such as `Berlin`, `Bern`, and `Belgium`, then update as the user continues typing.
- Selecting a city or region from manual search should use stored coordinates directly. Selecting a country should switch the country context and load that country's default major-city results instead of querying Tankerkönig with a country centroid.
- Users can filter results by fuel type: **E5**, **E10**, **Diesel**.
- Users can filter results by distance: `1km`, `2km`, `5km`.
- Users can order results by price.
- Each gas station entry is clickable and links to a site with more details.
- No login, no authentication, no user accounts.
- If the user searches the exact same location again, the app should avoid a duplicate upstream API call by reusing stored coordinates and deduplicating identical in-flight upstream requests.
- Manual location search should use a seeded local PostgreSQL dataset for European countries, administrative/place rows, place aliases, and German postal codes.
- The backend should deduplicate identical in-flight search requests so concurrent users share one fresh upstream fetch.
- The app should not rely on long-lived fuel-price result caching by default because price freshness matters.

---

## Domain Model

| Concept | Description |
| --- | --- |
| `Station` | A gas station with name, place, address, postal code, opening times, coordinates, supported fuels, and brand |
| `Price` | A fuel price entry (`E5`, `E10`, `DIESEL`) tied to a `Station` |
| `Region` | A geographic area defined by coordinates and radius |
| `FuelType` | Enum: `E5`, `E10`, `DIESEL` |

---

## User Flows

1. First visit fallback: frontend reads browser locale and time zone, derives a country if possible, falls back to Germany when locale has no region, and renders prices for major cities in that country.
2. Remembered country: on later visits, frontend reads the previously selected country from `localStorage` and renders that country's default results without asking for geolocation.
3. Use my city: user explicitly chooses `Use my city`, browser shows the geolocation permission prompt, frontend sends temporary coordinates to backend, backend queries Tankerkönig, frontend renders nearby station prices.
4. Manual search: user enters part of a country, city, region, place, or German postal code, frontend shows matching suggestions, frontend sends the query to backend, backend resolves matching PostgreSQL entries to coordinates first, then queries Tankerkönig when a coordinate-based result is needed, frontend renders the station list.
5. Filter: user selects fuel type and distance, results update in place or via a new backend query depending on implementation.

---

## Cross-Module Requirements

### Localization
- Frontend user-facing text must support German (`de`) and English (`en`).
- Backend-facing text, API docs, validation messages, and server-managed messages stay English unless a task explicitly requires backend localization.

### Location, Privacy, and UX
- First-visit country fallback must use browser locale plus `Intl.DateTimeFormat().resolvedOptions().timeZone`.
- If browser locale has no region, default the fallback country to Germany.
- Never use IP geolocation for the first-visit fallback.
- Browser geolocation is opt-in only through an explicit user action such as `Use my city`.
- Geolocation requests should use low accuracy only. City-level precision is enough.
- The frontend may persist only the selected country in `localStorage` for later visits.
- Raw coordinates must not be persisted in `localStorage`, `sessionStorage`, Pinia, or backend storage.
- The UI should include a minimal privacy notice that explains location is used for the current request and not stored.
- Manual search should provide an accessible autocomplete dropdown for partial queries and support matching countries, cities, regions, places, and German postal codes.

### Location Search and Geocoding
- PostgreSQL should hold a seeded GeoNames-based location dataset for European countries, administrative/place rows, place aliases, and German postal codes.
- The seeded dataset is the only source for manual search suggestions and coordinate resolution.
- `location_countries` should store `country_code`, `geoname_id`, `name`, `normalized_name`, `iso3_code`, `numeric_code`, `capital_name`, `continent_code`, optional `latitude`/`longitude`, optional `population`, and `created_at`.
- `location_places` should store `geoname_id`, `country_code`, `name`, `ascii_name`, `normalized_name`, `normalized_ascii_name`, `latitude`, `longitude`, `feature_class`, `feature_code`, optional `admin1_code` to `admin4_code`, `population`, optional `timezone`, optional `source_modified_on`, optional `alternate_names`, and `created_at`.
- `location_place_aliases` should store `place_geoname_id`, `alias_name`, `normalized_alias_name`, and `created_at`.
- `german_postal_codes` should store `country_code`, `postal_code`, `place_name`, `normalized_place_name`, optional `admin1_name` to `admin3_name`, `latitude`, `longitude`, optional `accuracy`, and `created_at`.
- Manual search should start suggestions after a short partial input and query the backend for ranked matches from PostgreSQL first.
- Suggestion ranking should favor exact matches, then prefix matches, then alias matches, then popularity and country relevance.
- For Germany, support local resolution of postal codes, cities, places, and the country itself from the seeded dataset.
- When a selected or submitted city or region already exists in PostgreSQL, the backend should use the stored coordinates immediately and continue to Tankerkönig.
- When a selected or submitted country already exists in PostgreSQL, the frontend should switch to that country context and render the major-city defaults for that country.
- If a submitted location is not already stored, the request should return no local match and the missing location must be added to the PostgreSQL dataset through the import/update workflow instead of a live geocoding fallback.
- Autocomplete must depend only on PostgreSQL matches and must not wait on any external geocoding provider.

### Public API Boundary
- The backend is a public, stateless, no-auth API.
- The frontend never calls Tankerkönig directly.
- `TANKERKOENIG_API_KEY` lives only in the backend environment.

---

## External API

### Tankerkönig API
- Base URLs:
  - `https://creativecommons.tankerkoenig.de/?page=info`
  - `https://creativecommons.tankerkoenig.de/swagger/`
  - `https://creativecommons.tankerkoenig.de`
- Example list request:
  - `https://creativecommons.tankerkoenig.de/json/list.php?lat=52.521&lng=13.438&rad=5&sort=dist&type=all&apikey=00000000-0000-0000-0000-000000000002`
- Key endpoints:
  - `/json/list.php` for stations by radius around coordinates
  - `/json/prices.php` for price refreshes of up to 10 known station IDs
  - `/json/detail.php` for single station detail
- Request constraints:
  - `list.php` requires `lat`, `lng`, `rad`, `type`, and `apikey`.
  - `rad` must not exceed `25` km.
  - `type` is `e5`, `e10`, `diesel`, or `all`.
  - `sort` is `price` or `dist`.
  - When `type=all`, Tankerkönig sorts by distance and `sort` is optional.
- Response handling rules:
  - Every Tankerkönig response must be checked via the upstream `ok` flag before reading payload fields.
  - Upstream failures return `ok=false` and an error `message`; the backend must map this to a structured internal error DTO.
  - `list.php` returns `e5`, `e10`, and `diesel` when `type=all`, but returns a single `price` field when only one fuel type is requested.
  - Price fields are not guaranteed to be numeric. Missing fuel support can be encoded as `false`.
  - `prices.php` station results can report `open`, `closed`, or `no prices`.
- Detail behavior:
  - `detail.php` is for selected station details, not regular price polling.
  - Detail responses can include `openingTimes`, `overrides`, `wholeDay`, and `state`.
  - `state` is often absent or `null`; detail fields must be treated as optional.
- Usage and product constraints:
  - Tankerkönig's free API is best-effort only; no SLA should be assumed.
  - Price requests should be triggered on demand from user actions. Regular background polling should be avoided.
  - Bulk or mass-data style live usage can lead to blocked requests or disabled API keys.
  - Tankerkönig data is delivered under `CC BY 4.0`; the product must include attribution.
  - MTS-K usage conditions apply and must be respected by the product.
- `TANKERKOENIG_API_KEY` lives only in the backend environment.
- The frontend never calls Tankerkönig directly.
- On upstream failure, return a structured error DTO to the client and log full details internally.

---

## Development

### Local Spinup
- Start from repo root.
- Backend: `.\car-fuel-live-backend\gradlew -p .\car-fuel-live-backend bootRun`
- Database: `docker-compose -f .\car-fuel-live-backend\docker-compose.yml up -d`
- Initial location seed: `.\car-fuel-live-backend\scripts\import-location-data.ps1`
- Frontend: `npm --prefix .\car-fuel-live-frontend run dev`
- Swagger UI: `http://localhost:8080/swagger-ui.html` in development, no authentication required

### Location Dataset Setup
- `import-location-data.ps1` is a setup and maintenance script, not part of the normal application runtime.
- Run it once after provisioning a new or empty PostgreSQL database so the local location dataset is available.
- Run it again only when the database was reset or when the location dataset should be refreshed deliberately.
- The script downloads the source data, prepares staging files, and loads the target PostgreSQL tables for countries, places, aliases, and German postal codes.

### Frontend Foundation
- The frontend foundation uses Vue 3 with TypeScript enabled.
- `shadcn-vue` is installed for UI component scaffolding.

### Verification Commands
- Backend tests: `.\car-fuel-live-backend\gradlew -p .\car-fuel-live-backend test`
- Backend seed verification includes a PostgreSQL integration test via Testcontainers, so Docker must be available for the full backend test suite.
- Frontend lint: `npm --prefix .\car-fuel-live-frontend run lint`
- Frontend build: `npm --prefix .\car-fuel-live-frontend run build`
- Frontend tests: `npm --prefix .\car-fuel-live-frontend run test -- --run`

### IntelliJ Project Structure
- Open the repository root as the IntelliJ project.
- Keep backend and frontend as separate modules inside the same IntelliJ project.
