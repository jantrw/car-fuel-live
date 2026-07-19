# Architecture

## Runtime

```text
Vue 3 frontend
  -> local location search and station result UI
  -> typed API clients

Spring Boot API
  -> local search endpoint
  -> live station lookup endpoint
  -> Tankerkoenig client

PostgreSQL 17
  -> Flyway-managed location dataset
```

The frontend is a Vite application. Vue components use composables for lookup, selected-country state, and station results; API calls are isolated in `src/api/`.

The backend is a stateless public Spring Boot API. It resolves manual-search results only from PostgreSQL, then calls Tankerkoenig only for a selected concrete coordinate pair. Tankerkoenig responses are validated through their `ok` flag and mapped to internal DTOs.

## Public Endpoints

| Endpoint | Responsibility |
| --- | --- |
| `GET /api/v1/locations/suggestions` | Local country, place, and German postal-code search. `q` is 2 to 80 characters; `countryCode` ranks but never filters. A result is marked for direct resolution only when one clear exact place or postal-code match exists. |
| `GET /api/v1/gas-stations` | Live nearby prices for validated `lat` and `lng`. Uses fixed MVP defaults: 5 km, all fuel types, distance order. |

Search returns typed items: `country`, `place`, or `postalCode`. Only places and postal codes include coordinates. No matches return an empty item list.

## Location Data

Flyway owns schema changes. The import script loads GeoNames-based European countries and places, aliases, and German postal codes into PostgreSQL. It also prepares normalized transliteration variants, keeping exact and prefix search index-friendly.

Core tables:

- `location_countries`: country metadata and an optional capital-place reference.
- `location_places`: GeoNames places and administrative rows with coordinates.
- `location_place_aliases`: searchable place aliases.
- `german_postal_codes`: German postal-code lookups.

The database is the only manual-search and coordinate-resolution source. There is no live geocoding provider and no long-lived fuel-price cache.

## Boundaries

- The frontend persists only the selected country; it never persists raw coordinates.
- The API key exists only in backend environment-backed configuration and is never logged or returned.
- Controllers validate public input. Responses use structured errors; internal details remain server-side.
- Public price lookups are rate-limited before upstream calls.
