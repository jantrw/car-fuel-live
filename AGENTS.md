# Project Agent Instructions
# Scope: repository root

Global agent rules live in `C:\Users\janferech\.codex\AGENTS.md`. This file adds repo-specific rules only.

## Documentation Sources
- Product spec: `docs/documentation.md`.
- Architecture: `docs/architecture.md`.
- Local ignored agent handoff, if present: `docs/agent-project-status.md`.
- Backend rules: `car-fuel-live-backend/AGENTS.md`.
- Frontend rules: `car-fuel-live-frontend/AGENTS.md`.

## Workflow
- Use a dedicated branch per issue: `<type>/<issue-id>-<short-description>`.
- Work branches merge into `dev` first. `dev` merges into `main` only after explicit approval.
- Never open a PR, merge, or push without asking first.
- Create GitHub issues with `gh` before non-trivial implementation unless the user explicitly waives it.
- Do not commit `todo.md` or `lessons.md` unless explicitly asked.
- Do not modify unrelated files.

## Product Rules
- Frontend user-facing copy must support German (`de`) and English (`en`).
- Backend-facing text, API docs, validation messages, and server-managed messages stay English unless explicitly requested.
- First-visit country fallback uses browser locale and `Intl.DateTimeFormat().resolvedOptions().timeZone`; never IP geolocation.
- If browser locale has no region, default fallback country is Germany.
- Frontend may persist only selected country in `localStorage`; never raw coordinates.
- Browser geolocation must be user-triggered, never automatic on page load.

## Security And CI
- Never commit secrets, `.env`, `secrets/`, or production configs.
- CI should run frontend high-severity audit and backend dependency checks on every push.
- TLS and HSTS belong to infrastructure/deployment. Do not downgrade runtime URLs to HTTP.

## Code And Docs
- Update `docs/documentation.md` for behavior or requirement changes.
- Update `docs/architecture.md` for architecture changes.
- Keep documentation focused on durable behavior and implemented architecture, not duplicate changelogs.
- Add comments only for non-obvious intent, constraints, invariants, or tricky behavior.
- Do not create or modify Docker files unless explicitly instructed.
- Ask before removing anything from `.gitignore`.

## Verification
- Frontend: ESLint, Prettier, Vitest, production build.
- For frontend changes, use Playwright MCP to verify behavior and capture screenshots for visual checks. Screenshots are local artifacts; never commit them.
- Backend: Spotless and Gradle tests.
- Full backend tests may need Docker/Testcontainers.
