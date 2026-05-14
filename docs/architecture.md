## Current Architecture

```text
Browser (Vue 3 + Vite frontend)
  -> manual search foundation in `car-fuel-live-frontend/src/components/location-lookup`
  -> lookup state in `src/composables/useLocationLookup.ts`
  -> country context persistence in `src/stores/locationCountryContext.ts`
  -> backend calls through `src/api/locationSearch.ts`
  -> explicit search form with grouped result sections

Spring Boot backend
  -> `BackendApplication`
  -> stateless public API security via `SecurityConfig`
  -> `GET /api/v1/locations/suggestions` for local manual-search results
  -> no Tankerkönig integration implemented yet

PostgreSQL 17 location dataset
  -> Flyway schema for countries, places, place aliases, German postal codes
  -> import script loads GeoNames Europe data into PostgreSQL
  -> DB-only location source for manual search
```

## Current Module State

- Frontend currently contains the manual search foundation for explicit local search. It derives the active country context from `localStorage`, browser locale, or time zone, persists only that country code, and shows it visibly above the input.
- The lookup form requests `/api/v1/locations/suggestions` only after an explicit search action. Query edits clear stale visible state immediately, and the grouped backend response renders as visible city/place, country, and postal-code result sections.
- Selecting a returned result in the current slice only fills the input and clears the visible result list. The later result-state behavior remains intentionally out of scope for this slice.
- Frontend backend access is isolated in `src/api/locationSearch.ts`; components do not call `fetch` directly.
- Frontend user-facing copy is available in English and German through `src/i18n/locationLookupMessages.ts`.
- Backend currently contains the Spring Boot entrypoint, stateless `permitAll` security configuration, structured validation error handling, and the local location search feature.
- The backend location search feature is grouped under `locations/` with controller, service, repository, DTO, and model packages.
- No geolocation flow, country persistence flow, rate limiting, or Tankerkönig client is implemented yet.

## Current Data Layer

- Flyway migration `V1__create_location_seed_schema.sql` creates `location_countries`, `location_places`, `location_place_aliases`, and `german_postal_codes`.
- `import-location-data.ps1` downloads GeoNames files, filters Europe rows, generates TSV staging files, verifies that Flyway migration `V1` completed, and loads data into PostgreSQL.
- The seeded dataset stores normalized search text plus latitude and longitude so manual search can resolve text locally from the database.
- The location search repository uses JDBC read queries against the seeded tables. It runs separate searches for countries, places, aliases, and German postal codes, then the service overfetches a bounded candidate window, de-duplicates across query families, ranks, and applies the final visible limit.
- Location search uses exact-match queries first so full city, country, alias, and postal-code inputs can use existing B-tree indexes. Prefix queries run only when exact matching returns no result.
- No live geocoding provider is part of the current architecture.

## Current API

- `GET /api/v1/locations/suggestions`
  Searches the local seeded PostgreSQL dataset only for manual-search results.
  Query parameters:
  - `q`: required non-blank text, length `2..80`.
  - `countryCode`: optional two-letter ISO country code used as a ranking preference only.
  - `limit`: optional, defaults to `8`, max `8`.
  Invalid request parameters, including malformed numeric values such as `limit=foo`, are normalized into the shared `ApiErrorResponse` validation contract.
  Response:
  - `items`: flat list of typed results.
  - `type`: `country`, `place`, or `postalCode`.
  - `id`, `label`, `countryCode`, `latitude`, `longitude`, and optional `postalCode`.
  `country` items intentionally omit `latitude` and `longitude`; `place` and `postalCode` items include them. Place results are first merged by place identity, then same-place populated/admin duplicates with the same administrative hierarchy are collapsed so distinct same-name towns remain selectable. If multiple German place results still share one visible label, the service injects a German Bundesland name from `admin1_code` into those labels without adding another database join. If the user query contains a postal code and a matching postal-code row exists, only postal-code results are returned. For textual manual search, city and place matches rank ahead of country prefix matches. Optional `countryCode` influences both candidate collection and final ranking so locally relevant results from the active country survive the bounded result window. The service still opens prefix fallback when an exact hit is only a low-confidence micro-place, so unfinished inputs like `berli` can continue to rank strong local continuations such as `Berlin` ahead of obscure zero-population exact rows.
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
