## Current Architecture

```text
Browser (Vue 3 + Vite starter frontend)
  -> minimal SPA foundation in `car-fuel-live-frontend/src`
  -> shared UI base with shadcn-vue button scaffold

Spring Boot backend
  -> `BackendApplication`
  -> stateless public API security via `SecurityConfig`
  -> no domain endpoints or Tankerkönig integration implemented yet

PostgreSQL 17 location dataset
  -> Flyway schema for countries, places, place aliases, German postal codes
  -> import script loads GeoNames Europe data into PostgreSQL
  -> intended as the only location resolution source for manual search
```

## Current Module State

- Frontend currently contains the default Vue app foundation plus base CSS and a small `shadcn-vue` button setup.
- Backend currently contains the Spring Boot entrypoint and a stateless `permitAll` security configuration.
- No backend controller, service, repository, or Tankerkönig client is implemented yet.
- No frontend location search flow, geolocation flow, localization flow, or country persistence flow is implemented yet.
- The current repo already contains the PostgreSQL location schema and the import path for Europe location seed data.

## Current Data Layer

- `V1__create_location_seed_schema.sql` creates `location_countries`, `location_places`, `location_place_aliases`, and `german_postal_codes`.
- `import-location-data.ps1` downloads GeoNames source files, filters Europe rows, generates TSV staging files, and loads them into PostgreSQL.
- The seeded dataset stores normalized search text plus latitude and longitude so manual search can resolve text locally from the database.
- No live geocoding provider is part of the current architecture.

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
