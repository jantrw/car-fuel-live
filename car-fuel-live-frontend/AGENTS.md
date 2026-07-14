# Frontend Agent Instructions
# Scope: car-fuel-live-frontend/

## Stack
- Vue 3
- TypeScript
- Vite
- Tailwind CSS 4
- Vitest

## Vue And TypeScript
- Use Composition API with `<script setup lang="ts">`.
- Apply `.codex/skills/vue-best-practices/SKILL.md` for Vue work.
- Components live in `src/components/`; composables live in `src/composables/` and start with `use`.
- Backend calls live in `src/api/` only. Components do not call `fetch` directly.
- Use typed props and emits.
- Validate external API responses at runtime.
- Prefer `unknown` over `any`; justify any escape hatch with an ESLint-disable comment.

## Styling
- Use Tailwind utilities first.
- Keep shared CSS in `src/assets/main.css`.
- Do not reintroduce shadcn-vue or generated `src/components/ui/` scaffolding unless explicitly requested.
- Do not reintroduce Pinia for single-screen state; prefer composables/module-level refs until app-wide state is real.

## Localization
- All user-facing copy must exist in German (`de`) and English (`en`).
- Keep reusable copy in `src/i18n/locationLookupMessages.ts` or the relevant shared message source.
- Do not hardcode new user-facing strings in components when a message source exists.

## Privacy And API
- On first visit, derive country fallback from browser locale and time zone only.
- Never trigger browser geolocation automatically.
- If geolocation is added, it must be user-triggered, low accuracy, timeout-bounded, and fall back to manual search.
- Persist only selected country in `localStorage`; never raw coordinates.
- Never expose Tankerkonig keys or upstream URLs in frontend code.

## Testing
- Test names use `should X when Y`.
- Cover behavior at the composable/API boundary when possible.
- Run lint, Vitest, and production build after frontend changes.
