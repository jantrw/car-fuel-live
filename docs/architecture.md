## Current Architecture

```text
Browser (Vue 3 + Vite frontend)
  -> manual location lookup MVP in `car-fuel-live-frontend/src/components/location-lookup`
  -> lookup state in `src/composables/useLocationLookup.ts`
  -> backend calls through `src/api/locationSearch.ts`
  -> shared UI base with shadcn-vue button scaffold

Spring Boot backend
  -> `BackendApplication`
  -> stateless public API security via `SecurityConfig`
  -> `GET /api/v1/locations/search` for local location lookup
  -> no Tankerkönig integration implemented yet

PostgreSQL 17 location dataset
  -> Flyway schema for countries, places, place aliases, German postal codes
  -> import script loads GeoNames Europe data into PostgreSQL
  -> DB-only location source for manual search
```

## Current Module State

- Frontend currently contains a temporary manual location lookup MVP. It lets the user enter a term, request matches, select a typed result, and view the selected label and coordinates.
- Frontend backend access is isolated in `src/api/locationSearch.ts`; components do not call `fetch` directly.
- Frontend user-facing MVP copy is available in English and German through `src/i18n/locationLookupMessages.ts`.
- Backend currently contains the Spring Boot entrypoint, stateless `permitAll` security configuration, structured validation error handling, and the local location search feature.
- The backend location search feature is grouped under `locations/` with controller, service, repository, DTO, and model packages.
- No geolocation flow, country persistence flow, rate limiting, or Tankerkönig client is implemented yet.

## Current Data Layer

- Flyway migration `V1__create_location_seed_schema.sql` creates `location_countries`, `location_places`, `location_place_aliases`, and `german_postal_codes`.
- `import-location-data.ps1` downloads GeoNames files, filters Europe rows, generates TSV staging files, verifies that Flyway migration `V1` completed, and loads data into PostgreSQL.
- The seeded dataset stores normalized search text plus latitude and longitude so manual search can resolve text locally from the database.
- The location search repository uses JDBC read queries against the seeded tables. It runs separate searches for countries, places, aliases, and German postal codes, then the service de-duplicates, ranks, and limits the combined result list.
- Location search uses exact-match queries first so full city, country, alias, and postal-code inputs can use existing B-tree indexes. Prefix queries run only when exact matching returns no result.
- No live geocoding provider is part of the current architecture.

## Current API

- `GET /api/v1/locations/search`
  Searches the local seeded PostgreSQL dataset only.
  Query parameters:
  - `query`: required non-blank text, max 80 characters.
  - `limit`: optional, defaults to `8`, max `8`.
  Response:
  - `items`: flat list of typed results.
  - `type`: `country`, `place`, or `postalCode`.
  - `id`, `label`, `countryCode`, `latitude`, `longitude`, and optional `postalCode`.
  Place results are semantically de-duplicated by type, country, and label. If the user query contains a postal code and a matching postal-code row exists, only postal-code results are returned.
  No-match responses return `200 OK` with an empty `items` list.

## PostgreSQL Schema

- `location_countries`
  Holds one row per seeded European country. Primary key is `country_code`. Columns: `geoname_id`, `name`, `normalized_name`, `iso3_code`, `numeric_code`, `capital_name`, `continent_code`, optional `latitude`/`longitude`, optional `population`, and `created_at`.
- `location_places`
  Holds GeoNames administrative and populated place rows. Primary key is `geoname_id`. Foreign key `country_code -> location_countries.country_code`. Columns: `name`, `ascii_name`, `normalized_name`, `normalized_ascii_name`, `latitude`, `longitude`, `feature_class`, `feature_code`, optional `admin1_code` to `admin4_code`, `population`, optional `timezone`, optional `source_modified_on`, optional `alternate_names`, and `created_at`.
- `location_place_aliases`
  Holds searchable aliases for places. Composite primary key is `(place_geoname_id, alias_name)`. Foreign key `place_geoname_id -> location_places.geoname_id` with `ON DELETE CASCADE`. Columns: `alias_name`, `normalized_alias_name`, and `created_at`.
- `german_postal_codes`
  Holds German postal-code lookups only. Composite primary key is `(postal_code, place_name)`. `country_code` is constrained to `DE`. Columns: `place_name`, `normalized_place_name`, optional `admin1_name` to `admin3_name`, `latitude`, `longitude`, optional `accuracy`, and `created_at`.

## Search-Relevant Indexes

- `location_countries.normalized_name` for country-name lookup.
- `location_places.country_code` for country-scoped place queries.
- `location_places.normalized_name` and `location_places.normalized_ascii_name` for primary place search.
- `location_places.feature_code` for filtering by GeoNames feature type.
- `location_place_aliases.normalized_alias_name` for alias matches.
- `german_postal_codes.normalized_place_name` for German postal-code place lookup.
