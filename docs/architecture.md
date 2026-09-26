# Architecture

## Runtime

```text
Vue 3 frontend
  -> typed API clients

Spring Boot API
   -> PostgreSQL 17 (location search)
   -> Tankerkoenig (live fuel prices)
```

The Vite frontend uses Vue composables for search, selected-country state, and station results. All API calls live in `src/api/`.

The stateless Spring Boot API resolves manual searches from PostgreSQL. It calls Tankerkoenig only after a user selects a result with coordinates. The client checks each response's `ok` flag and maps valid data to internal DTOs.

## Public endpoints

| Endpoint                            | Responsibility                                             |
| ----------------------------------- | ---------------------------------------------------------- |
| `GET /api/v1/locations/suggestions` | Searches local countries, places, and German postal codes. |
| `GET /api/v1/gas-stations`          | Returns live prices near validated coordinates.            |

Location queries must contain 2 to 80 characters. The optional `countryCode` affects ranking but never filters results. The API marks a result for direct resolution only for one clear, exact place or postal-code match.

Search items have one of three types: `country`, `place`, or `postalCode`. Only places and postal codes include coordinates. A search without matches returns an empty item list.

Station lookups use fixed MVP defaults: a 5 km radius, all fuel types, and distance sorting.

## Location data

Flyway owns schema changes. The import script loads GeoNames-based European countries, places, aliases, and German postal codes into PostgreSQL. It also stores normalized transliterations so indexes can support exact and prefix searches.

Core tables:

- `location_countries`: country metadata and an optional reference to the capital.
- `location_places`: GeoNames places and administrative areas with coordinates.
- `location_place_aliases`: searchable aliases for places.
- `german_postal_codes`: German postal codes and their coordinates.

The database is the only manual-search and coordinate-resolution source. There is no live geocoding provider and no long-lived fuel-price cache.

## Boundaries

- The frontend stores only the selected country, never raw coordinates.
- Backend environment configuration holds the API key. Logs and responses never expose it.
- Controllers validate public input. Structured error responses omit internal details.
- Rate limits run before price lookups call Tankerkoenig or location searches query the database.
