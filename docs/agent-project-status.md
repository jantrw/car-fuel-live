# Agent Project Status

**Audience:** agents only
**Purpose:** fast handoff document for the implemented product slice, recent delivered work, and the next likely follow-ups.

## Current Product Slice

- Public no-auth web app for fuel-price lookup by location.
- No live fuel-price integration yet. Tankerkönig contract and constraints are documented, but no client or result flow is implemented.
- Current implemented frontend slice is a manual local-search foundation:
  - active country context derived from `localStorage`, browser locale, or time zone
  - explicit search only, no live autocomplete while typing
  - grouped local search results from PostgreSQL
  - selecting a result currently only fills the input and clears the list
- Current implemented backend slice is a PostgreSQL-only location lookup:
  - `GET /api/v1/locations/suggestions`
  - result types: `country`, `place`, `postalCode`
  - country context affects ranking, not filtering
  - no external geocoding provider in runtime

## Implemented Data Foundation

- Flyway-managed location schema exists for:
  - `location_countries`
  - `location_places`
  - `location_place_aliases`
  - `german_postal_codes`
- Seed pipeline imports GeoNames-based Europe data into PostgreSQL.
- Search supports normalized transliteration variants without runtime fuzzy SQL.
- `location_countries.capital_place_geoname_id` now provides a stable country-to-capital reference for later country-driven flows.

## Delivered Work By Closed Issues

### Core Location Dataset

- `#23` implemented the geocoding persistence schema.
- `#24` seeded the local PostgreSQL location dataset.
- `#30` added PostgreSQL integration coverage for the seed pipeline.

### Manual Search API And UI

- `#34` introduced the backend local-search endpoint `GET /api/v1/locations/suggestions`.
- `#35` built the frontend manual-search foundation on top of that endpoint.
- `#45` replaced live autocomplete with explicit context-aware manual search.
- `#46` refined ranking for unfinished place inputs and added transliterated search variants such as `koeln`, `koln`, `muenchen`, `munchen`, `zuerich`, and `zurich`.

### Search Correctness Fixes

- `#40` preserved distinct same-name places instead of collapsing them incorrectly.
- `#41` applied the visible result limit after semantic deduplication instead of before.

### Deliberately Closed Without Implementation

- `#36` was closed as `not planned` after the product direction changed from live autocomplete to explicit manual search.

## Current Search Behavior

- Queries start only after an explicit submit.
- Query length contract is `2..80`.
- Country context is strongest for short or ambiguous prefixes.
- Clear exact place intent must outrank country preference.
- Visible prefix results currently mix:
  - up to `5` context-preferred local continuations
  - then up to `3` strong global direct or near-direct alternatives
- German postal-code queries return postal-code-only results when a matching postal row exists.

## Current Architecture Snapshot

- Frontend:
  - Vue 3 + TypeScript + Vite
  - lookup UI in `car-fuel-live-frontend/src/components/location-lookup`
  - lookup orchestration in `src/composables/useLocationLookup.ts`
  - country context state in `src/stores/locationCountryContext.ts`
  - backend access in `src/api/locationSearch.ts`
- Backend:
  - Spring Boot
  - public stateless API
  - location feature grouped under `car-fuel-live-backend/src/main/java/io/github/jantrw/carfuellive/locations`
- Database:
  - PostgreSQL 17
  - Flyway owns schema
  - import script owns data loading only

## Important Constraints

- Keep PostgreSQL as the only runtime source for manual location search.
- Do not reintroduce live geocoding unless explicitly decided.
- Do not turn country context into a hard filter.
- Prefer exact, deterministic data preparation during seed over expensive runtime fuzzy matching.
- `todo.md` and `lessons.md` are local scratch files. Never commit or push them unless explicitly requested.

## Open Follow-Ups

- Location-to-price MVP is still missing and should be treated as the next core product step:
  the app currently stops at local location search, but the core product value is still not implemented. 
  The next MVP should accept the same manual place/city/country-style search flow, let the user choose the intended result, send that result's latitude and longitude to the Tankerkönig backend integration, and render fuel prices in the frontend. The implementation should prioritize strong UX and fast perceived performance.
- `#48` is still open:
  alias/ascii prefix ranking in the backend needs a structural improvement so matches found via `normalized_ascii_name` or `normalized_alias_name` are treated as strong continuations during ranking.
- Naming cleanup remains open:
  internal names such as `suggest`, `locationSuggestions`, and `suggestionGroupTitle` still reflect the older autocomplete terminology.
- Country-driven default result flow is still not implemented:
  the stable capital mapping exists, but no user-facing flow consumes it yet.

## Recommended Starting Points For The Next Agent

1. Read `docs/documentation.md` for intended product behavior.
2. Read `docs/architecture.md` for the currently implemented architecture only.
3. Check `todo.md` for local scratch tasks and `lessons.md` for session rules.
4. If touching location search, inspect:
   - `LocationSearchController`
   - `LocationSearchService`
   - `LocationSearchRepository`
   - `useLocationLookup.ts`
5. If touching country-driven flows, inspect:
   - `V2__add_country_capital_place_mapping.sql`
   - seed SQL in `src/main/resources/db/seed/`
   - `CountryCapitalPlace`
