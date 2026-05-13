## Application Overview

**Name:** Car Fuel Live (working title)
**Purpose:** Public, no-auth web app for real-time fuel prices across Germany and nearby European countries for a selected city or country.
**Document role:** Agent-facing product and planning reference. Captures intended behavior and durable requirements.

---

## Planned Product Behavior

- Current issue #38 MVP: users can type a search term, request local matches, select one typed result, and see its stored label and coordinates. This is a temporary proof slice before the later autocomplete and country-context flows.
- On first visit, derive the initial country from browser locale and time zone.
- If browser locale has no region, default to Germany.
- If the user does not share a city, show prices for major cities in the selected country/area.
- Store the selected country in `localStorage`.
- Users can choose `Use my city`. Only then request browser geolocation and show nearby gas prices.
- Users can manually enter a country, city, region, place, or German postal code. The backend resolves matching text to longitude and latitude from PostgreSQL before any Tankerkönig lookup.
- Manual search must provide autocomplete suggestions for countries, cities, regions, places, and German postal codes.
- Suggestions should appear after short inputs such as `Be` and return matches such as `Berlin`, `Bern`, and `Belgium`.
- Selecting a city or region should use stored coordinates directly. Selecting a country should switch country context and load that country's default major-city results instead of querying Tankerkönig with a country centroid.
- Users can filter results by fuel type: **E5**, **E10**, **Diesel**.
- Users can filter results by distance: `1km`, `2km`, `5km`.
- Users can order results by price.
- Each gas station entry should link to more details.
- No login, no authentication, no user accounts.
- The backend should deduplicate identical in-flight location lookups and identical in-flight Tankerkönig requests so concurrent users share one active computation per application node.
- The backend may keep a small in-memory coordinate cache for the largest European cities and the most common German cities so repeated known-city searches can skip unnecessary PostgreSQL lookups.
- Do not rely on long-lived fuel-price result caching by default. Price freshness matters more.
- Public search endpoints should enforce throttling and request limits so abusive traffic is rejected before it can spam the database or Tankerkönig.

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

1. First visit fallback: frontend reads browser locale and time zone, derives a country if possible, falls back to Germany when locale has no region, and renders major-city prices for that country.
2. Remembered country: frontend reads the selected country from `localStorage` and renders that country's default results without asking for geolocation.
3. Use my city: user chooses `Use my city`, browser asks for permission, frontend sends temporary coordinates to backend, backend queries Tankerkönig, frontend renders nearby prices.
4. Manual search: user enters part of a country, city, region, place, or German postal code, frontend shows suggestions, backend resolves matching PostgreSQL entries to coordinates, then queries Tankerkönig when needed, frontend renders the station list.
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
- Browser geolocation is opt-in only through an explicit action such as `Use my city`.
- Geolocation requests should use low accuracy only. City-level precision is enough.
- The frontend may persist only the selected country in `localStorage` for later visits.
- Raw coordinates must not be persisted in `localStorage`, `sessionStorage`, Pinia, or backend storage.
- The UI should include a minimal privacy notice stating that location is used for the current request and not stored.
- Manual search should provide an accessible autocomplete dropdown for partial queries and support matching countries, cities, regions, places, and German postal codes.

### Location Search and Geocoding
- PostgreSQL should hold seeded GeoNames data for European countries, administrative/place rows, place aliases, and German postal codes.
- The seeded dataset is the only source for manual search suggestions and coordinate resolution.
- The backend now exposes `GET /api/v1/locations/suggestions?q=...&countryCode=...&limit=...` for autocomplete suggestions from the seeded PostgreSQL dataset. It returns a flat `items` list with explicit result types: `country`, `place`, and `postalCode`.
- The suggestions endpoint requires `q` length `2..80` characters, treats optional `countryCode` as a ranking boost only, and limits `limit` to `8`. Invalid request parameters return the shared structured validation error payload.
- `place` and `postalCode` suggestion items include coordinates directly in the response. `country` suggestion items omit coordinates so country selection remains a context change, not a centroid lookup.
- The earlier `GET /api/v1/locations/search?query=...&limit=...` MVP endpoint remains available for the temporary manual lookup screen and still returns the same typed `items` list.
- The backend may fetch a bounded internal candidate window larger than the requested `limit` so the final user-visible limit is applied only after cross-query ranking and semantic deduplication.
- If a query contains a German postal code and PostgreSQL has a matching postal-code row, the MVP returns only postal-code results for that query.
- Text-only place searches suppress postal-code-by-place-name matches and collapse only same-place place/admin duplicates that share the same administrative hierarchy, preferring populated places over administrative rows while keeping distinct same-name towns selectable. When multiple German place results would otherwise share the same visible label, the backend appends the Bundesland name from `admin1_code` to those labels so users can distinguish them.
- MVP no-match behavior is `200 OK` with `items: []`; the frontend shows an empty state instead of an error.
- Do not add a broad second geocoding source. If a cache exists, keep it limited to canonical coordinates for the largest European cities and the most common German cities and warm it from PostgreSQL.
- `location_countries` should store `country_code`, `geoname_id`, `name`, `normalized_name`, `iso3_code`, `numeric_code`, `capital_name`, `continent_code`, optional `latitude`/`longitude`, optional `population`, and `created_at`.
- `location_places` should store `geoname_id`, `country_code`, `name`, `ascii_name`, `normalized_name`, `normalized_ascii_name`, `latitude`, `longitude`, `feature_class`, `feature_code`, optional `admin1_code` to `admin4_code`, `population`, optional `timezone`, optional `source_modified_on`, optional `alternate_names`, and `created_at`.
- `location_place_aliases` should store `place_geoname_id`, `alias_name`, `normalized_alias_name`, and `created_at`.
- `german_postal_codes` should store `country_code`, `postal_code`, `place_name`, `normalized_place_name`, optional `admin1_name` to `admin3_name`, `latitude`, `longitude`, optional `accuracy`, and `created_at`.
- Manual search should start after a short partial input and query PostgreSQL for ranked matches first.
- Suggestion ranking should favor city and place matches over country prefix matches for textual autocomplete, then popularity, with optional `countryCode` applied only as a boost.
- For Germany, support local resolution of postal codes, cities, places, and the country itself from the seeded dataset.
- When a selected or submitted city or region already exists in PostgreSQL, the backend should use the stored coordinates immediately and continue to Tankerkönig.
- When a selected or submitted country already exists in PostgreSQL, the frontend should switch to that country context and render the major-city defaults for that country.
- If a submitted location is missing, return no local match.
- Autocomplete must depend only on PostgreSQL matches and must not wait on any external geocoding provider.

### Public API Boundary
- The backend is a public, stateless, no-auth API.
- The frontend never calls Tankerkönig directly.
- `TANKERKOENIG_API_KEY` lives only in the backend environment.
- Public search and detail endpoints must enforce rate limits and bounded result sizes.

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
- On upstream failure, return a structured error DTO and log full details internally.

---

## Development

### Local Spinup
- Start from repo root.
- Create the backend env file once: `Copy-Item .\car-fuel-live-backend\.env.example .\car-fuel-live-backend\.env`
- Set at least `DB_USER`, `DB_PASSWORD`, and `DB_NAME` in `car-fuel-live-backend/.env`.
- Keep `TANKERKOENIG_API_KEY` empty until live fuel-price integration is being worked on.
- Database: `docker compose --env-file .\car-fuel-live-backend\.env -f .\car-fuel-live-backend\docker-compose.yml up -d`
- Backend: `.\car-fuel-live-backend\gradlew.bat -p .\car-fuel-live-backend bootRun`
- The bundled PostgreSQL container binds to `127.0.0.1:3307` only for local development.
- Initial location seed for a new or empty local database: `.\car-fuel-live-backend\scripts\import-location-data.ps1`
- Frontend: `npm --prefix .\car-fuel-live-frontend run dev`
- Swagger UI: `http://localhost:8080/swagger-ui.html` in development, no authentication required

### Location Dataset Setup
- `import-location-data.ps1` is a setup and maintenance script, not part of the normal application runtime.
- The script resolves the running PostgreSQL container from the docker compose `postgres` service instead of depending on a hard-coded container name.
- Start the backend once before seeding so Flyway creates and records the location schema.
- Run it once after provisioning a new or empty PostgreSQL database.
- Run it again only after a database reset or a deliberate dataset refresh.
- The script downloads the source data, prepares staging files, and loads the target PostgreSQL tables for countries, places, aliases, and German postal codes.
- The script does not create target schema tables. It fails if Flyway migration `V1` has not completed successfully.

### Frontend Foundation
- The frontend uses Vue 3 with TypeScript enabled.
- `shadcn-vue` is installed for UI component scaffolding.
- The current frontend screen is a temporary manual location lookup MVP for issue #38. It sends search requests through `src/api/`, validates the response shape at runtime, and stores selected coordinates only in component memory for the current view.
- Vite proxies `/api` to `http://localhost:8080` during local development.

### Verification Commands
- Backend tests: `.\car-fuel-live-backend\gradlew.bat -p .\car-fuel-live-backend test`
- Backend seed verification includes a PostgreSQL integration test via Testcontainers, so Docker must be available for the full backend test suite.
- Frontend lint: `npm --prefix .\car-fuel-live-frontend run lint`
- Frontend build: `npm --prefix .\car-fuel-live-frontend run build`
- Frontend tests: `npm --prefix .\car-fuel-live-frontend run test -- --run`

### IntelliJ Project Structure
- Open the repository root as the IntelliJ project.
- Keep backend and frontend as separate modules inside the same IntelliJ project.
