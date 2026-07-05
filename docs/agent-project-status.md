# Agent Project Status

**Audience:** agents only
**Purpose:** fast handoff for the current implemented slice and next likely follow-ups.

## Current Product Slice

- Public no-auth web app for manual location search and live fuel-price lookup.
- Frontend derives country context from `localStorage`, browser locale, or time zone.
- Search is explicit submit only; no live autocomplete while typing.
- PostgreSQL is the only runtime source for manual location search.
- Selecting `place` or `postalCode` loads nearby Tankerkönig prices through the backend.
- Selecting `country` updates only country context for now.

## Current Search Behavior

- Queries start only after an explicit submit.
- Query length contract is `2..80`.
- Country context is strongest for short or ambiguous prefixes.
- Clear exact place intent must outrank country preference.
- German postal-code queries return postal-code-only results when a matching postal row exists.

## Entry Points

- Frontend:
  - `car-fuel-live-frontend/src/components/location-lookup/`
  - `car-fuel-live-frontend/src/composables/useLocationLookup.ts`
  - `car-fuel-live-frontend/src/composables/useGasStationResults.ts`
  - `car-fuel-live-frontend/src/api/locationSearch.ts`
  - `car-fuel-live-frontend/src/api/gasStations.ts`
- Backend:
  - `car-fuel-live-backend/src/main/java/io/github/jantrw/carfuellive/locations/`
  - `car-fuel-live-backend/src/main/java/io/github/jantrw/carfuellive/stations/`
  - `car-fuel-live-backend/src/main/java/io/github/jantrw/carfuellive/tankerkoenig/`
  - `car-fuel-live-backend/src/main/resources/db/migration/`
  - `car-fuel-live-backend/src/main/resources/db/seed/`

## Important Constraints

- Keep PostgreSQL as the only runtime source for manual location search.
- Do not reintroduce live geocoding unless explicitly decided.
- Do not turn country context into a hard filter.
- `todo.md` and `lessons.md` are local scratch files. Never commit or push them unless explicitly requested.

## Open Follow-Ups

- `#48`: alias/ascii prefix ranking needs structural backend improvement.
- Naming cleanup: replace old autocomplete terms such as `suggest`, `locationSuggestions`, and `suggestionGroupTitle`.
- `#51`: station detail slice.
- `#53`: filter UI for fuel type, radius, and sorting.
- `#54`: country-driven default price flow.
- `#55`: broader public rate limiting and Tankerkönig consumption hardening.
- `#56`: opt-in `Use my city` geolocation flow.

## Handoff

1. Read `docs/documentation.md` for intended behavior.
2. Read `docs/architecture.md` for implemented architecture only.
3. Check private local `todo.md` and `lessons.md` before editing.
