# Project Agent Instructions
# Scope: repository root

## Project Context
- At the start of each session, read `lessons.md` if it exists and apply its relevant guidance. It is local-only; never commit it unless explicitly asked.
- Read `docs/agent-project.md` first. It is the complete agent-oriented project context; do not read the public docs unless the task needs reader-facing wording.
- Read the scoped `AGENTS.md` only when changing that module.

## Workflow
- Use a dedicated branch per issue: `<type>/<issue-id>-<short-description>`.
- Work branches merge into `dev` first. `dev` merges into `main` only after explicit approval.
- Never open a PR, merge, or push without asking first.
- Create GitHub issues with `gh` before non-trivial implementation unless the user explicitly waives it.
- Do not commit `todo.md` or `lessons.md` unless explicitly asked.
- Do not modify unrelated files.

## Security And CI
- Never commit secrets, `.env`, `secrets/`, or production configs.
- CI should run frontend high-severity audit and backend dependency checks on every push.
- TLS and HSTS belong to infrastructure/deployment. Do not downgrade runtime URLs to HTTP.

## Code And Docs
- Update `docs/agent-project.md` for durable agent guidance. Update public docs only when their reader-facing product or architecture summary changes.
- Keep `docs/documentation.md` and `docs/architecture.md` current when durable behavior or architecture changes. Summarize; do not turn them into implementation logs or exhaustive requirement lists.
- Add comments only for non-obvious intent, constraints, invariants, or tricky behavior.
- Do not create or modify Docker files unless explicitly instructed.
- Ask before removing anything from `.gitignore`.

## Verification
- Frontend: ESLint, Prettier, Vitest, production build.
- For frontend changes, use Playwright MCP to verify behavior and capture screenshots for visual checks. Store screenshots in `img-screenshots/`; they are local artifacts and must never be committed.
- Backend: Spotless and Gradle tests.
- Full backend tests may need Docker/Testcontainers.

## Codex Code Review guidelines

**Use these guidelines only when using the automated PR review!**

- Write all review findings in German.
- Keep findings concise.
- Use a short imperative title.
- Limit the explanation to at most three sentences.
- Do not restate the changed code.
- Do not include praise, generic advice, or introductory text.
- State only:
    1. the concrete defect,
    2. its practical consequence,
    3. the smallest reasonable correction.
- Only report actionable issues with a realistic failure scenario.
