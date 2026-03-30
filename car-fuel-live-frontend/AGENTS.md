# Frontend Agent Instructions
# Scope: car-fuel-live-frontend/

---

## Stack
| Technology   | Version              |
|--------------|----------------------|
| Vue.js       | 3.5.29               |
| TypeScript   | 5.9.3                |
| Tailwind CSS | 4.0.0                |
| shadcn-vue   | 2.4.3                |
| pinia        | 3.0.4                |

## Vue.js Style

- Single-responsibility components — one concern per file.
- Filenames / component names: **PascalCase** in code (`StationCard.vue`), **kebab-case** in templates (`<station-card />`).
- Composition API for all components. Always use `<script setup lang="ts">`.
- Keep templates simple — move all logic into `setup()` or composables.
- Composables live in `src/composables/`, prefixed with `use` (e.g., `useGasPrices.ts`).
- All fetch calls to the backend live in `src/api/` only. Components and composables never call `fetch` directly.
- Explicit `props` validation on every component — no untyped props.
- Use pinia stores.

### Tooling & Enforcement
- ESLint + `eslint-plugin-vue` + `@typescript-eslint` + Prettier + Volar.
- `eslint --fix` and Prettier run in CI and on pre-commit.

---
## UI Components (shadcn-vue)

- Use shadcn-vue as the primary component library.
- Add components via CLI only: `npx shadcn-vue@latest add <component>`.
- Never modify files inside `src/components/ui/` — these are auto-generated.
- Custom logic goes into wrapper components in `src/components/`, not into ui/ files.
- Use shadcn-vue components as building blocks — style via Tailwind utility classes on the wrapper, not by editing the component source.
---

## Tailwind CSS v4.0

- Utility classes exclusively. No custom CSS unless a utility genuinely cannot cover the case.
- Design tokens (colors, spacing, typography) configured via `@theme` in the root CSS file — do not hardcode values inline.
- No inline `style=""` attributes for anything Tailwind can express.
- Class ordering enforced automatically by `prettier-plugin-tailwindcss`.

---

## TypeScript

- `tsconfig.json` must enable strict mode — no exceptions.
- Prefer `unknown` over `any`. Restrict `any` to documented exceptions with an explicit `// eslint-disable-next-line` comment explaining why.
- Use `interface` for public shapes and DTOs; use `type` for unions or complex compositions.
- Validate all external inputs at runtime (API responses, URL params) even when TypeScript types exist.
- Never use `as unknown as X` casts without a comment justifying the bypass.

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "moduleResolution": "bundler",
    "esModuleInterop": true,
    "skipLibCheck": true
  }
}
```

---

## Testing

### Test Naming Convention
Pattern: `should X when Y`

```typescript
describe('useGasPrices', () => {
  it('should return stations when valid coordinates are provided', () => { ... })
  it('should return empty array when no stations are in radius', () => { ... })
  it('should throw error when API call fails', () => { ... })
})
```

- No abbreviations in test names.
- Failing test name alone must identify the problem.
- Arrange / Act / Assert inside every test body, separated by blank lines.

---

## Security

### Geolocation — Client-Side Rules
- Request **low accuracy only** (`enableHighAccuracy: false`). City-level radius is sufficient — high accuracy is unnecessary and more invasive under GDPR.
- Always use sensible timeout and `maximumAge`:
```typescript
navigator.geolocation.getCurrentPosition(
  onSuccess,
  onError,
  { enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 }
)
```
- Handle all three error cases explicitly with user-friendly messages:
  - `PERMISSION_DENIED` → show manual search, no error state
  - `POSITION_UNAVAILABLE` → show manual search with message
  - `TIMEOUT` → show manual search with retry option
- Never store raw coordinates in component state longer than needed for the current request.
- Never send coordinates to any third party. All geo queries go to the Spring Boot backend only.
- Display a clear consent prompt **before** calling `navigator.geolocation.getCurrentPosition()`.
- Fall back to manual city search gracefully when permission is denied — no silent IP-based fallback.

### GDPR — Frontend Rules
- Do not persist coordinates in `localStorage`, `sessionStorage`, or any client-side store.
- Coordinates are used only to trigger a backend request, then discarded.
- Display a minimal privacy notice stating what location data is used for and that it is not stored.

### API Communication
- All backend calls go through `src/api/` — never call the backend directly from components.
- Never expose or log any API keys. The Tankerkönig key lives on the backend only and must never appear in frontend code, network requests, or browser DevTools.
- Handle API error responses with structured error handling — never display raw error messages from the backend to the user.

---

## Design
Always follow `skills/frontend-design/SKILL.md` when implementing new features or creating UI.
