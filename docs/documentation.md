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
- Users can manually enter any city or region. The location is resolved to longitude and latitude.
- Users can filter results by fuel type: **E5**, **E10**, **Diesel**.
- Users can filter results by distance: `1km`, `2km`, `5km`.
- Users can order results by price.
- Each gas station entry is clickable and links to a site with more details.
- No login, no authentication, no user accounts.
- If the user searches the exact same location again, the app should avoid a duplicate upstream API call by using cached or stored data.
- Manual location search should use a persisted local location dataset for European countries and places plus German postal codes.
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
4. Manual search: user enters city or region, frontend sends query to backend, backend resolves coordinates and queries Tankerkönig, frontend renders station list.
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

### Caching and Freshness
- Prefer a persisted geocoding cache for repeated manual searches.
- Deduplicate identical in-flight upstream requests so concurrent searches share one fresh fetch.
- Do not rely on long-lived fuel-price result caching by default. Price freshness is more important than multi-minute caching.

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
- Frontend lint: `npm --prefix .\car-fuel-live-frontend run lint`
- Frontend build: `npm --prefix .\car-fuel-live-frontend run build`
- Frontend tests: `npm --prefix .\car-fuel-live-frontend run test -- --run`

### IntelliJ Project Structure
- Open the repository root as the IntelliJ project.
- Keep backend and frontend as separate modules inside the same IntelliJ project.
