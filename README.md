# Car Fuel Live

Public no-auth web app for finding live fuel prices near a selected place.

## Stack

- Frontend: Vue 3, TypeScript, Vite, Tailwind CSS
- Backend: Java 21, Spring Boot 4, Spring Security, Spring JDBC, Flyway
- Data: PostgreSQL 17
- External API: Tankerkonig, called by the backend only

## Repository Structure

```text
.
|- car-fuel-live-frontend/   Vue frontend
|- car-fuel-live-backend/    Spring Boot backend
|- docs/                     Product and architecture docs
```

## Getting Started

Create the backend env file once:

```powershell
Copy-Item .\car-fuel-live-backend\.env.example .\car-fuel-live-backend\.env
```

Set `DB_USER`, `DB_PASSWORD`, and `DB_NAME`. Set `TANKERKOENIG_API_KEY` only when verifying live fuel prices.

Start local services from the repository root:

```powershell
docker compose --env-file .\car-fuel-live-backend\.env -f .\car-fuel-live-backend\docker-compose.yml up -d
.\car-fuel-live-backend\gradlew.bat -p .\car-fuel-live-backend bootRun
npm --prefix .\car-fuel-live-frontend run dev
```

Seed location data for a new or empty database:

```powershell
.\car-fuel-live-backend\scripts\import-location-data.ps1
```

Run the backend once before seeding so Flyway creates and records the database schema.

## Verification

```powershell
.\car-fuel-live-backend\gradlew.bat -p .\car-fuel-live-backend test
npm --prefix .\car-fuel-live-frontend run test -- --run
npm --prefix .\car-fuel-live-frontend run build
```

## Documentation

- Product behavior and requirements: [`docs/documentation.md`](docs/documentation.md)
- Implemented architecture: [`docs/architecture.md`](docs/architecture.md)
