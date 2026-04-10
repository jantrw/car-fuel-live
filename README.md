<div align="center">

# Car Fuel Live

Public web app in progress for finding live fuel prices around a user's city in Germany and nearby Europe.

<sub><strong>Tech Stack</strong></sub>

[![Frontend: Vue 3, TypeScript 5, Vite 7](https://img.shields.io/badge/Frontend-Vue%203%20%7C%20TypeScript%205%20%7C%20Vite%207-42B883?style=for-the-badge&logo=vuedotjs&logoColor=white)](car-fuel-live-frontend)
[![UI: Tailwind CSS 4, shadcn-vue](https://img.shields.io/badge/UI-Tailwind%20CSS%204%20%7C%20shadcn--vue-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)](car-fuel-live-frontend)
[![Backend: Java 21, Spring Boot 4](https://img.shields.io/badge/Backend-Java%2021%20%7C%20Spring%20Boot%204-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](car-fuel-live-backend)
[![Data: PostgreSQL 17, Flyway](https://img.shields.io/badge/Data-PostgreSQL%2017%20%7C%20Flyway-336791?style=for-the-badge&logo=postgresql&logoColor=white)](car-fuel-live-backend)
[![API Spec: OpenAPI](https://img.shields.io/badge/API%20Spec-OpenAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](car-fuel-live-backend)
[![Project Status](https://img.shields.io/badge/status-foundation%20stage-C97A00?style=for-the-badge)](#project-status)

[![Open Issues](https://img.shields.io/github/issues/jantrw/car-fuel-live?style=flat-square)](https://github.com/jantrw/car-fuel-live/issues)
[![Closed Issues](https://img.shields.io/github/issues-closed/jantrw/car-fuel-live?style=flat-square)](https://github.com/jantrw/car-fuel-live/issues?q=is%3Aissue+is%3Aclosed)
[![Last Commit](https://img.shields.io/github/last-commit/jantrw/car-fuel-live/main?style=flat-square)](https://github.com/jantrw/car-fuel-live/commits/main)
[![Stars](https://img.shields.io/github/stars/jantrw/car-fuel-live?style=flat-square)](https://github.com/jantrw/car-fuel-live/stargazers)
[![Forks](https://img.shields.io/github/forks/jantrw/car-fuel-live?style=flat-square)](https://github.com/jantrw/car-fuel-live/network/members)

[Overview](#overview) •
[Stack](#stack) •
[Getting Started](#getting-started) •
[Docs](#documentation) •
[Roadmap](#roadmap)

</div>

## Overview

Car Fuel Live is being built to answer one simple question fast: where is the cheapest nearby station for the fuel I need right now?

The planned product focuses on:

- country fallback from browser locale and time zone on first visit
- major-city price view when the user has not shared a city yet
- explicit `Use my city` geolocation
- manual search for a city or region
- remembered country context across visits
- live fuel prices for `E5`, `E10`, and `Diesel`
- quick filtering by radius
- sorting by price
- clickable stations with more details
- backend-only Tankerkonig integration

## What Problem Does This Solve

Finding a fuel station should be simple: check nearby prices, compare options quickly, and move on.

Car Fuel Live is intended to keep that experience focused:

- live fuel prices for the current country context or a searched location
- fast filtering by fuel type and radius
- clear sorting by price
- no account, no login, no unnecessary friction

## Project Status

This repository is not a finished application yet.

What already exists:

- Vue 3 + TypeScript frontend foundation
- Spring Boot 4 backend foundation
- PostgreSQL + Flyway backend setup
- product and architecture documentation

What is still in progress:

- first end-to-end search-to-results flow
- live Tankerkonig integration
- geocoding persistence and request deduplication
- frontend result views and filtering UX

## Location Data Model

The backend stores a local PostgreSQL location dataset that turns country, place, and German postal-code searches into coordinates the app can use.

| Table | What it stores | Why the app needs it |
| --- | --- | --- |
| `location_countries` | Supported European countries with reference coordinates | Lets the app resolve country searches and switch country context cleanly |
| `location_places` | Seeded European cities, towns, districts, and administrative places with coordinates | Lets the app resolve manual place searches to coordinates for later Tankerkonig requests |
| `location_place_aliases` | Alternate names, spellings, and normalized search variants for places | Lets the app find places even when users type localized names or alternate spellings |
| `german_postal_codes` | German postal codes mapped to place names and coordinates | Lets the app resolve German postal-code searches directly to coordinates |

This is an application-level overview, not the full SQL schema. The detailed implementation lives in the backend migrations and docs.

## Repository Structure

```text
.
|- car-fuel-live-frontend/   Vue frontend
|- car-fuel-live-backend/    Spring Boot backend
|- docs/                     Product and architecture docs
```

## Getting Started

Current development entry points:

```powershell
\.\car-fuel-live-backend\scripts\import-location-data.ps1
.\car-fuel-live-backend\gradlew -p .\car-fuel-live-backend bootRun
npm --prefix .\car-fuel-live-frontend run dev
```

Expected supporting services during development:

- PostgreSQL
- initial location seed import for a new or empty database
- Swagger UI for backend API inspection

## Documentation

- Product overview: [`docs/documentation.md`](docs/documentation.md)
- Architecture notes: [`docs/architecture.md`](docs/architecture.md)

## Roadmap

Near-term priorities:

1. implement the first search-to-results flow
2. integrate live fuel data in the backend
3. persist and reuse geocoding results safely
4. harden verification, security checks, and CI

## Notes

- This README intentionally describes the actual current state, not the intended finished state.
- Setup instructions will be expanded once the first usable end-to-end flow exists.
