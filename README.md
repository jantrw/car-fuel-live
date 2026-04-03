# Car Fuel Live

Car Fuel Live is a planned web application for finding live fuel prices around a user's current location or a searched region in Germany.

The project is currently in the foundation stage. The repository already contains early frontend and backend scaffolding, but the first end-to-end user flow is still being built.

## Why This Project

Fuel price tools often hide the simplest question behind clutter: where is the cheapest nearby station for the fuel I need right now?

Car Fuel Live aims to stay focused:

- live fuel prices near the user or a searched location
- fast filtering by fuel type and radius
- clear sorting by price
- no account, no login, no unnecessary friction

## Planned Product Scope

The initial product direction is:

- browser geolocation on first visit
- manual search for a city or region
- results for `E5`, `E10`, and `Diesel`
- radius filters such as `1 km`, `2 km`, and `5 km`
- sorting by price
- clickable station entries with more details
- backend-only integration with the Tankerkonig API

## Current Status

Current repository state:

- frontend foundation exists with Vue 3, TypeScript, Vite, and `shadcn-vue`
- backend foundation exists with Spring Boot 4, Java 21, Flyway, PostgreSQL, and OpenAPI
- architecture and product documentation exist
- production-ready feature flows do not exist yet

What is actively being worked on:

- first usable vertical slice from search input to station results
- backend integration and response handling for live fuel data
- persistence for cached geocoding and related app data
- frontend views for search, filters, and result rendering

## Architecture Direction

The intended high-level flow is:

```text
Browser (Vue 3 SPA)
  -> Spring Boot REST API
       -> Tankerkonig API
       -> PostgreSQL
```

Key constraints:

- the frontend never talks to Tankerkonig directly
- the API key stays in the backend only
- freshness matters, so long-lived fuel-price caching is intentionally avoided
- duplicate location searches should be deduplicated where sensible

## Tech Stack

- Frontend: Vue 3, TypeScript, Vite, shadcn-vue
- Backend: Spring Boot 4, Java 21, Flyway
- Database: PostgreSQL 17
- API documentation: OpenAPI / Swagger UI

## Repository Structure

```text
.
|- car-fuel-live-frontend/   Vue frontend
|- car-fuel-live-backend/    Spring Boot backend
|- docs/                     Product and architecture docs
```

## Getting Started

Repository setup is still being stabilized. Until the first vertical slice exists, local reproduction is best treated as work in progress.

Current planned local commands:

```powershell
.\car-fuel-live-backend\gradlew -p .\car-fuel-live-backend bootRun
npm --prefix .\car-fuel-live-frontend run dev
```

Planned local services and tooling:

- PostgreSQL for backend persistence
- Swagger UI for backend API inspection during development

## Documentation

- Product overview: [`docs/documentation.md`](docs/documentation.md)
- Architecture notes: [`docs/architecture.md`](docs/architecture.md)

## Roadmap

Near-term priorities:

1. implement the first search-to-results flow
2. connect the backend to live fuel data
3. store and reuse geocoding results safely
4. add verification, security checks, and CI hardening

## Status Note

If you are visiting this repository early: this is not a finished application yet. It is an actively shaped project with the product direction, stack, and boundaries already defined.
