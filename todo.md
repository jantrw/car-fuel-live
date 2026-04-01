# todo

## Global AGENTS scope cleanup
- [x] Back up `C:\Users\janferech\.codex\AGENTS.md`
- [x] Remove project-specific rules from global `AGENTS.md`
- [x] Move relocated rules into the correct project `AGENTS.md`
- [x] Verify final rule placement across global + repo files

## Repo + module AGENTS scope cleanup
- [x] Move product spec from repo root `AGENTS.md` into docs
- [x] Keep repo root `AGENTS.md` to repo workflow and cross-module rules only
- [x] Remove repo-level duplication from backend `AGENTS.md` where possible
- [x] Remove repo-level duplication from frontend `AGENTS.md` where possible
- [x] Verify final placement across repo root, backend, frontend, and docs

## Review
- Global `AGENTS.md` now contains only cross-project behavior, verification, GitHub, security, and core principles.
- Project-specific workflow rules were moved into the repo root `AGENTS.md`.
- Backup created at `C:\Users\janferech\.codex\AGENTS.backup-2026-04-01.md`.
- Product spec and local spinup moved from repo root `AGENTS.md` into `docs/documentation.md`.
- Architecture data flow moved from repo root `AGENTS.md` into `docs/architecture.md`.
- Backend `AGENTS.md` no longer duplicates generic verification rules and no longer contradicts its own `@ConfigurationProperties` guidance.
- Frontend responsive/scalable UI rule now lives in the frontend design section.
- Repo, backend, frontend, and docs wording was tightened for consistency, grammar, and formatting clarity.
- Global `AGENTS.md` wording was tightened for clarity without changing scope.
- Backend `AGENTS.md` now explicitly references `.codex/skills/java-best-practices/SKILL.md` and `.codex/skills/spring-data-jpa/SKILL.md`.
- Frontend `AGENTS.md` now explicitly references `.codex/skills/vue-best-practices/SKILL.md` and `.codex/skills/frontend-design/SKILL.md`.
