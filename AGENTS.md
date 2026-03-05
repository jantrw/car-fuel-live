# Project_Agents.MD

## Application Overview

**Name:** Gas Price Viewer (working title)
**Purpose:** A public, no-auth web application that displays real-time fuel prices across Germany for a user's current location or any searched region.

---

## What the App Does

- On first visit, the app requests browser geolocation and displays gas prices for the user's detected region.
- Users can manually enter any city or region to view gas prices there.
- Users can filter results by fuel type: **E5**, **E10**, **Diesel**.
- No login, no authentication, no user accounts — ever, by design.

---

## Domain Model

| Concept    | Description                                                       |
|------------|-------------------------------------------------------------------|
| `Station`  | A gas station with name, address, coordinates, and brand          |
| `Price`    | A fuel price entry (E5 / E10 / Diesel) tied to a Station         |
| `Region`   | A geographic bounding area (coordinates + radius) used to query  |
| `FuelType` | Enum: `E5`, `E10`, `DIESEL`                                       |

---

## User Flows

1. **Auto-location:** User visits → browser prompts for geolocation → coordinates sent to backend → backend queries Tankerkönig → Vue renders station list with prices.
2. **Manual search:** User types a city/region → frontend sends query to backend → backend resolves coordinates and queries Tankerkönig → Vue renders results.
3. **Filter:** User selects one or more fuel types from the filter UI → results update in-place (client-side filter on loaded data, or new backend query depending on implementation).

---

## External APIs

### Tankerkönig API
- Base URL: `https://creativecommons.tankerkoenig.de/`
- Key endpoints:
  - `/json/list.php` — stations by radius around coordinates
  - `/json/detail.php` — single station detail
- **API key:** stored exclusively in the `TANKERKOENIG_API_KEY` environment variable. Never sent to the client, never logged, never committed to the repository.
- **Proxying:** The backend proxies all Tankerkönig calls. The Vue frontend never contacts the Tankerkönig API directly.
- **Rate limiting / caching:** Cache all Tankerkönig responses server-side for a minimum of 5 minutes. Deduplicate concurrent in-flight requests for the same region to avoid hammering the API when multiple users query the same area simultaneously.
- **Error handling:** On API failure, return a structured error DTO to the client. Log the full error internally. Never expose API internals, keys, or raw error messages to the browser.

---

## Architecture (Data Flow)

```
Browser (Vue 3 SPA)
  └─→ Spring Boot REST API  (/api/v1/...)
        └─→ Tankerkönig API     (server-side only, API key never leaves backend)
        └─→ PostgreSQL 17       (response cache + optional persistence)
```

---

## Security & Coding Standards
Rules are scoped to each module:
- Backend rules (Java, Spring Boot, Security): `car-fuel-live-backend/AGENTS.md`
- Frontend rules (Vue, TypeScript, Tailwind): `car-fuel-live-frontend/AGENTS.md`
- Never commit secrets, `.env` files, or production configs to the repository.
- TLS and HSTS are enforced at the infrastructure/deployment level —
  never ship a build that downgrades to HTTP.

---

## Code Changes
- Always update `documentation.MD` after code changes.
- Update `architecture.md` if the architecture changes.
- Do not modify unrelated files.

---

## Docker *(not yet in scope)*
Do not create or modify any Docker-related files until explicitly instructed.

When Docker is introduced, apply the following rules:
- Use minimal base images: `eclipse-temurin:21-jre-alpine` for Java, `node:22-alpine` for Node build steps.
- All containers must run as non-root users.
- Scan images with Trivy before shipping.
- Provide a `docker-compose.yml` covering the app and PostgreSQL for local development.

---

## Formatting Enforcement
- **ESLint + Prettier** — JS / TS / Vue
- **Spotless** — Java

Do not introduce new formatting rules.
