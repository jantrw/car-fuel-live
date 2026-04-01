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
  - `/json/detail.php` for single station detail
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

### IntelliJ Project Structure
- Open the repository root as the IntelliJ project.
- Keep backend and frontend as separate modules inside the same IntelliJ project.
