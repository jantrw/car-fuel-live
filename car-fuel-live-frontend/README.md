# car-fuel-live-frontend

Vue 3 frontend for the current Car Fuel Live manual-search plus live-price MVP.

## Current Scope

- derives the active country context from `localStorage`, browser locale, or time zone
- renders an explicit manual search flow for `country`, `place`, and German `postalCode` results
- loads nearby live fuel prices after selecting a `place` or `postalCode`
- keeps `country` selection as a context change only in the current MVP
- persists only the selected country code, never raw coordinates

## Stack

- Vue 3
- TypeScript 5
- Vite 7
- Pinia
- Tailwind CSS 4
- shadcn-vue

## Development

Install dependencies:

```sh
npm install
```

Run the dev server:

```sh
npm run dev
```

The Vite dev server proxies `/api` to the backend on `http://localhost:8080`.

## Verification

Run tests:

```sh
npm run test -- --run
```

Build the app:

```sh
npm run build
```

Lint the source tree:

```sh
npm run lint
```

## Important Files

- `src/components/location-lookup/LocationLookupView.vue`: page-level lookup and price-result composition
- `src/composables/useLocationLookup.ts`: explicit local search orchestration
- `src/composables/useGasStationResults.ts`: selected-result price lookup orchestration
- `src/api/locationSearch.ts`: backend location search client
- `src/api/gasStations.ts`: backend gas-station client
- `src/stores/locationCountryContext.ts`: active country context state
