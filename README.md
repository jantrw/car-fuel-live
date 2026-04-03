<div align="center">

# Car Fuel Live

Public web app in progress for finding live fuel prices around a user's location or searched region in Germany.

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](car-fuel-live-backend)
[![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](car-fuel-live-backend)
[![Vue 3](https://img.shields.io/badge/Vue-3-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)](car-fuel-live-frontend)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](car-fuel-live-frontend)
[![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](car-fuel-live-backend)
[![Project Status](https://img.shields.io/badge/status-foundation%20stage-C97A00?style=for-the-badge)](#project-status)

[![Open Issues](https://img.shields.io/github/issues/jantrw/car-fuel-live?style=flat-square)](https://github.com/jantrw/car-fuel-live/issues)
[![Closed Issues](https://img.shields.io/github/issues-closed/jantrw/car-fuel-live?style=flat-square)](https://github.com/jantrw/car-fuel-live/issues?q=is%3Aissue+is%3Aclosed)
[![Last Commit](https://img.shields.io/github/last-commit/jantrw/car-fuel-live/main?style=flat-square)](https://github.com/jantrw/car-fuel-live/commits/main)
[![Stars](https://img.shields.io/github/stars/jantrw/car-fuel-live?style=flat-square)](https://github.com/jantrw/car-fuel-live/stargazers)
[![Forks](https://img.shields.io/github/forks/jantrw/car-fuel-live?style=flat-square)](https://github.com/jantrw/car-fuel-live/network/members)

[Overview](#overview) •
[Stack](#stack) •
[Architecture](#architecture-direction) •
[Getting Started](#getting-started) •
[Docs](#documentation) •
[Roadmap](#roadmap)

</div>

## Overview

Car Fuel Live is being built to answer one simple question fast: where is the cheapest nearby station for the fuel I need right now?

The planned product focuses on:

- browser geolocation on first visit
- manual search for a city or region
- live fuel prices for `E5`, `E10`, and `Diesel`
- quick filtering by radius
- sorting by price
- clickable stations with more details
- backend-only Tankerkonig integration

## What Problem Does This Solve

Finding a fuel station should be simple: check nearby prices, compare options quickly, and move on.

Car Fuel Live is intended to keep that experience focused:

- live fuel prices near the user or a searched location
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

## Stack

| Area | Technology |
| --- | --- |
| Frontend | Vue 3, TypeScript, Vite, shadcn-vue |
| Backend | Spring Boot 4, Java 21, Flyway, OpenAPI |
| Database | PostgreSQL 17 |
| External data | Tankerkonig API |

## Architecture Direction

```text
Browser (Vue 3 SPA)
  -> Spring Boot REST API
       -> Tankerkonig API
       -> PostgreSQL
```

Core boundaries:

- the frontend never talks to Tankerkonig directly
- the Tankerkonig API key stays in the backend only
- fuel-price freshness is prioritized over aggressive caching
- identical searches should be deduplicated where it improves efficiency without harming freshness

## Repository Structure

```text
.
|- car-fuel-live-frontend/   Vue frontend
|- car-fuel-live-backend/    Spring Boot backend
|- docs/                     Product and architecture docs
```

## Getting Started

Local reproduction is still work in progress because the first full vertical slice is not complete yet.

Current development entry points:

```powershell
.\car-fuel-live-backend\gradlew -p .\car-fuel-live-backend bootRun
npm --prefix .\car-fuel-live-frontend run dev
```

Expected supporting services during development:

- PostgreSQL
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
