## Application Overview

**Name:** Car Fuel Live (working title)
**Purpose:** A public, no-auth web application that displays real-time fuel prices across Germany for a user's current location or any searched region.

---

## What the App Does

- On first visit, the app requests browser geolocation and displays gas prices for the user's detected region.
- Users can manually enter any city or region. The location is resolved to longitude and latitude.
- Users can filter results by fuel type: **E5**, **E10**, **Diesel**.
- Users can filter results by distance: `1km`, `2km`, `5km`.
- Users can order results by price.
- Each gas station entry is clickable and links to a site with more details.
- No login, no authentication, no user accounts.
- If the user searches the exact same location again, the app should avoid a duplicate upstream API call by using cached or stored data.
- Manual location search should use a persisted geocoding cache for major cities in Germany and major cities in Europe.
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

1. Auto-location: user visits, browser prompts for geolocation, frontend sends coordinates to backend, backend queries Tankerkönig, frontend renders station list with prices.
2. Manual search: user enters city or region, frontend sends query to backend, backend resolves coordinates and queries Tankerkönig, frontend renders station list.
3. Filter: user selects fuel type and distance, results update in place or via a new backend query depending on implementation.

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
- Frontend: `npm --prefix .\car-fuel-live-frontend run dev`
- Swagger UI: `http://localhost:8080/swagger-ui.html` in development, no authentication required

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
