## Current Architecture

```text
Browser (Vue 3 + Vite frontend)
  -> manual search and station result UI in `car-fuel-live-frontend/src/components/location-lookup`
  -> lookup state in `src/composables/useLocationLookup.ts`
  -> selected-result gas-station state in `src/composables/useGasStationResults.ts`
  -> country context persistence in `src/composables/useLocationCountryContext.ts`
  -> backend calls through `src/api/locationSearch.ts` and `src/api/gasStations.ts`

Spring Boot backend
  -> stateless public API security via `SecurityConfig`
  -> `GET /api/v1/locations/suggestions` for local manual-search results
  -> `GET /api/v1/gas-stations` for live price lookup by coordinates
  -> `stations/` feature for public price lookup response shaping
  -> `tankerkoenig/` feature for upstream client, parsing, and config

PostgreSQL 17 location dataset
  -> Flyway schema for countries, places, place aliases, German postal codes
  -> import script loads GeoNames Europe data into PostgreSQL
  -> DB-only location source for manual search
```

## Current Module State

- Frontend contains the manual search plus live price MVP. It derives the active country context from `localStorage`, browser locale, or time zone, persists only that country code, and shows it above the input.
- The lookup form requests `/api/v1/locations/suggestions` only after an explicit search action. Query edits clear stale visible state and loaded station results.
- Selecting a `place` or `postalCode` result updates the input, persists the result country context, and triggers `/api/v1/gas-stations`.
- Selecting a `country` result updates only the country context and visible input. Country-driven default price results are still out of scope.
- Frontend backend access is isolated in `src/api/locationSearch.ts` and `src/api/gasStations.ts`; components do not call `fetch` directly.
- Frontend user-facing copy is available in English and German through `src/i18n/locationLookupMessages.ts`.
- Backend contains the Spring Boot entrypoint, stateless `permitAll` security configuration, structured validation error handling, local location search, live gas-station lookup, and a proxy-aware rate-limit filter for `GET /api/v1/gas-stations`.
- The Tankerkonig client uses fixed MVP defaults (`rad=5`, `type=all`, `sort=dist`), validates the upstream `ok` flag, maps station fields into internal DTOs, and treats unsupported fuel values encoded as `false` as unavailable prices.
- No geolocation flow or country-driven default result flow is implemented yet. Public rate limiting currently exists for `GET /api/v1/gas-stations`; equivalent protection for the remaining public endpoints is still a follow-up.

## Current Data Layer

- Flyway migration `V1__create_location_seed_schema.sql` creates `location_countries`, `location_places`, `location_place_aliases`, and `german_postal_codes`. Migration `V2__add_country_capital_place_mapping.sql` adds `location_countries.capital_place_geoname_id`.
- `import-location-data.ps1` downloads GeoNames files, filters Europe rows, generates TSV staging files, verifies that the required Flyway-managed location schema exists, and loads data into PostgreSQL.
- The seeded dataset stores normalized search text plus latitude and longitude so manual search can resolve text locally from the database. The seed also materializes transliteration variants such as expanded and folded umlaut forms into alias search rows instead of relying on runtime SQL string functions.
- The seed transform resolves one stable capital-place reference per imported country. This keeps copied databases self-contained for future country-default flows, even though no current runtime endpoint reads that mapping.
- The location search repository uses JDBC read queries against the seeded tables. It runs separate searches for countries, places, aliases, and German postal codes, then the service overfetches a bounded candidate window, de-duplicates across query families, ranks, and applies the final visible limit.
- Location search uses exact-match queries first so full city, country, alias, and postal-code inputs can use existing B-tree indexes. Prefix queries run only when exact matching returns no result.
- No live geocoding provider is part of the current architecture.
- No long-lived fuel-price cache is part of the current architecture.

## Current API

- `GET /api/v1/locations/suggestions`
  Searches the local seeded PostgreSQL dataset only for manual-search results.
  Query parameters:
  - `q`: required non-blank text, length `2..80`.
  - `countryCode`: optional two-letter ISO country code used as a ranking preference only.
  - `limit`: optional, defaults to `8`, max `8`.
  Invalid request parameters return the shared `ApiErrorResponse` validation contract.
  Response:
  - `items`: flat list of typed results.
  - `type`: `country`, `place`, or `postalCode`.
  - `id`, `label`, `countryCode`, `latitude`, `longitude`, and optional `postalCode`.
  `country` items omit `latitude` and `longitude`; `place` and `postalCode` items include them. No-match responses return `200 OK` with an empty `items` list.

- `GET /api/v1/gas-stations`
  Resolves nearby live gas-station prices for a concrete coordinate pair through the backend Tankerkonig integration.
  Query parameters:
  - `lat`: required decimal latitude, range `-90..90`.
  - `lng`: required decimal longitude, range `-180..180`.
  Current fixed backend defaults:
  - `rad=5`
  - `type=all`
  - `sort=dist`
  Invalid coordinates return the shared `ApiErrorResponse` validation contract.
  Rate-limited requests return `429 Too Many Requests` with code `RATE_LIMITED`.
  Upstream or configuration failures return `502 Bad Gateway` with code `UPSTREAM_ERROR`.

## PostgreSQL Schema

- `location_countries`
  Holds one row per seeded European country with a current `PCLI` source row. Primary key is `country_code`. Columns include `geoname_id`, names/codes, optional `capital_place_geoname_id`, reference coordinates, population, and `created_at`.
- `location_places`
  Holds GeoNames administrative and populated place rows with coordinates, feature metadata, administrative codes, population, optional timezone/source fields, and `created_at`.
- `location_place_aliases`
  Holds searchable aliases for places. Composite primary key is `(place_geoname_id, alias_name)`.
- `german_postal_codes`
  Holds German postal-code lookups only. Composite primary key is `(postal_code, place_name)`.

## Search-Relevant Indexes

- `location_countries.normalized_name` for exact and prefix-range country-name lookup.
- `location_countries.capital_place_geoname_id` for copied-database completeness and future direct country-to-capital lookup.
- `location_places.country_code` for country-scoped place queries.
- `location_places.normalized_name` and `location_places.normalized_ascii_name` for exact and prefix-range primary place search.
- Composite `location_places.country_code` plus normalized-name indexes for country-scoped prefix-range place search.
- `location_places.feature_code` for filtering by GeoNames feature type.
- `location_place_aliases.normalized_alias_name` for exact alias matches.
- `location_place_aliases.normalized_alias_name, place_geoname_id` for alias prefix-range search and joins.
- `german_postal_codes.normalized_place_name` for German postal-code place lookup and indexed place-name prefixes.
