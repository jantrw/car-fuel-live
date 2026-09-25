# Car Fuel Live

Find live fuel prices near a selected place without creating an account.

## Stack

- Frontend: Vue 3, TypeScript, Vite, Tailwind CSS
- Backend: Java 21, Spring Boot 4, Spring Security, Spring JDBC, Flyway
- Data: PostgreSQL 17
- External API: Tankerkoenig (backend only)

## Repository Structure

```text
.
|- car-fuel-live-frontend/   Vue frontend
|- car-fuel-live-backend/    Spring Boot backend
|- docs/                     Product and architecture docs
```

## Getting Started

Create the backend environment file:

```powershell
Copy-Item .\car-fuel-live-backend\.env.example .\car-fuel-live-backend\.env
```

Set `DB_USER`, `DB_PASSWORD`, and `DB_NAME`.

Start PostgreSQL from the repository root:

```powershell
docker compose --env-file .\car-fuel-live-backend\.env -f .\car-fuel-live-backend\docker-compose.yml up -d
```

Start the backend in a new PowerShell terminal. Set the API key to retrieve live prices:

```powershell
$env:TANKERKOENIG_API_KEY = '<your-api-key>'
.\car-fuel-live-backend\gradlew.bat -p .\car-fuel-live-backend bootRun
```

Start the frontend in another terminal:

```powershell
npm --prefix .\car-fuel-live-frontend run dev
```

After the backend starts and Flyway creates the schema, seed a new or empty database:

```powershell
.\car-fuel-live-backend\scripts\import-location-data.ps1
```

## Verification

```powershell
.\car-fuel-live-backend\gradlew.bat -p .\car-fuel-live-backend test
npm --prefix .\car-fuel-live-frontend run test -- --run
npm --prefix .\car-fuel-live-frontend run build
```

## Documentation

- Product behavior and requirements: [`docs/documentation.md`](docs/documentation.md)
- Implemented architecture: [`docs/architecture.md`](docs/architecture.md)
