## Application Overview

**Name:** Car Fuel Live (working title)
**Purpose:** A public, no-auth web application that displays real-time fuel prices across Germany for a user's current location or any searched region.

---

## What the App Does

- On first visit, the app requests browser geolocation and displays gas prices for the user's detected region.
- Users can manually enter any city or region (city or region will be transferred to longitude and latitude) to view gas prices there.
- Users can filter results by fuel type: **E5**, **E10**, **Diesel**.
- Users can filter results by distance: 1km, 2km, 5km and so on.
- Users can order results via price.
- Each Price/Gas station is clickable and refers to another site in which more detail is shown.
- No login, no authentication, no user accounts — ever, by design.
- App request data once per search to a location. If the user searches for the exact location again, the api won't be called again, the data would be cached or stored.

---

## Domain Model

| Concept    | Description                                                                                                            |
|------------|------------------------------------------------------------------------------------------------------------------------|
| `Station`  | A gas station with name,place, address, postal code, opening times, coordinates, list of fuels they support, and brand |
| `Price`    | A fuel price entry (E5 / E10 / Diesel) tied to a Station                                                               |
| `Region`   | A geographic bounding area (coordinates + radius) used to query                                                        |
| `FuelType` | Enum: `E5`, `E10`, `DIESEL`                                                                                            |

---

## User Flows

1. **Auto-location:** User visits → browser prompts for geolocation → coordinates sent to backend → backend queries Tankerkönig → Vue renders station list with prices.
2. **Manual search:** User types a city/region → frontend sends query to backend → backend resolves coordinates and queries Tankerkönig → Vue renders results.
3. **Filter:** User selects one or more fuel types from the filter UI/select distance → results update in-place (client-side filter on loaded data, or new backend query depending on implementation).

---

## External APIs

### Tankerkönig API
- Base URL: `https://creativecommons.tankerkoenig.de/?page=info` + `https://creativecommons.tankerkoenig.de/swagger/` + `https://creativecommons.tankerkoenig.de`
- Example URL for gas stations within 5km at the center of berlin: `https://creativecommons.tankerkoenig.de/json/list.php?lat=52.521&lng=13.438&rad=5&sort=dist&type=all&apikey=00000000-0000-0000-0000-000000000002`
- Key endpoints:
  - `/json/list.php` — stations by radius around coordinates
  - `/json/detail.php` — single station detail
- **API key:** stored exclusively in the `TANKERKOENIG_API_KEY` environment variable. Never sent to the client and is never logged.
- **Proxying:** The backend proxies all Tankerkönig calls. The Vue frontend never contacts the Tankerkönig API directly.
- **Error handling:** On API failure, return a structured error DTO to the client. Log the full error internally. Never expose API internals, keys, or raw error messages to the browser.

---

## Security & Coding Standards

### Dependency scanning
- Run npm audit --audit-level=high (frontend) and ./gradlew dependencyCheckAnalyze (backend) in CI on every push.

Rules are scoped to each module:
- Backend rules (Java, Spring Boot, Security): `car-fuel-live-backend/AGENTS.md`
- Frontend rules (Vue, TypeScript, Tailwind): `car-fuel-live-frontend/AGENTS.md`

- Never commit secrets, `.env` files, or production configs to the repository.
- TLS and HSTS are enforced at the infrastructure/deployment level —
  never ship a build that downgrades to HTTP.

---

## Architecture (Data Flow)

```
Browser (Vue 3 SPA)
  └─→ Spring Boot REST API  (/api/v1/...)
        └─→ Tankerkönig API     (server-side only, API key never leaves backend)
        └─→ PostgreSQL 17       (response cache + optional persistence)
```

## Code Changes
- Always update `docs/documentation.MD` after code changes.
- Update `docs/architecture.md` if the architecture changes.
- Do not modify unrelated files.
- Always test the code after implementing new features or code changes.

---

## Git

## Commit Scopes
`api` | `db` | `map` | `prices` | `auth` | `docker` | `config`

**Examples:**
feat(api): add Tankerkönig price polling endpoint
fix(db): correct station coordinate mapping on insert
refactor(map): extract marker logic into composable
chore(docker): pin postgres image to 17.2

## Issue Labels
In addition to global labels: `map` | `polling`

### .gitignore

- Always ask if you want to adjust the .gitignore.
- Add necessary files/folder to the .gitignore. Focus on the best-practise within the .gitignore and a public repo.

---

## Dev spinups

- Start backend, DB, frontend
- Make sure to be in root folder: `cd car-fuel-live`
  --> backend: `.\car-fuel-live-backend\gradlew -p car-fuel-live-backend bootRun`
  --> docker-db: `docker-compose -f .\car-fuel-live-backend\docker-compose.yml up -d`
  --> frontend: `npm --prefix .\car-fuel-live-frontend run dev`

---

## Docker *(not yet in scope)*
Do not create or modify any Docker-related files until explicitly instructed.

When Docker is introduced, apply the following rules:
- Use minimal base images: `eclipse-temurin:21-jre-alpine` for Java, `node:22-alpine` for Node build steps.
- All containers must run as non-root users.
- Scan images with Trivy before shipping.
- Provide a `docker-compose.yml` covering the app and PostgreSQL for local development.

---

## Github Actions *(not yet in scope)*

- Workflows live in `.github/workflows/`

---

## Formatting Enforcement
- **ESLint + Prettier** — JS / TS / Vue
- **Spotless** — Java

Do not introduce new formatting rules.
