# Project Agent Instructions
# Scope: repository root

## Documentation Sources
- Agent handoff status lives in `docs/agent-project-status.md`.
- Product spec lives in `docs/documentation.md`.
- Architecture lives in `docs/architecture.md`.
- Backend rules live in `car-fuel-live-backend/AGENTS.md`.
- Frontend rules live in `car-fuel-live-frontend/AGENTS.md`.

## Repo Workflow
- If a prompt is unclear, **always** ask questions to gather needed information.
- Enter plan mode for any non-trivial task (3+ steps or architectural decisions).
- If something goes sideways, stop and re-plan immediately.
- Use plan mode for verification steps, not just building.
- Divide every plan into sequential phases. Each phase maps to exactly one GitHub issue.
- Create all issues via the GitHub CLI before writing any code unless explicitly waived by the user.
  - Keep issues simple, concrete, and easy to understand.
- No project boards, milestones, or labels — issues only.

### Task Management
1. **Plan First**: Write plan to `todo.md` with checkable items.
2. **Verify Plan**: Check in before starting implementation.
3. **Track Progress**: Mark items complete as you go.
4. **Explain Changes**: High-level and understanding summary at each step.
5. **Document Results**: Add review section to `todo.md`.
6. **Capture Lessons**: Update `lessons.md` after corrections.

#### Todo Quality
- For any larger task, expand the parent item in `todo.md` into concrete subtodos before implementation.
- Write parent todos so a different agent can understand the goal and continue the work without prior chat context.
- Use subtodos to capture the work breakdown, key constraints, and relevant open questions when they materially affect execution.
- Do not leave large umbrella todos vague. If a task hides multiple meaningful steps, split it.
- If a task grows during implementation, update `todo.md` and add subtodos immediately.
- When a todo is finished and its result is proven, move it to a `Ready To Delete` section in `todo.md`.
- Only mark items `Ready To Delete` after implementation is complete and verification is complete.
- Keep unfinished, unverified, or partially verified items out of `Ready To Delete`.

### Self-Improvement
- Review `lessons.md` at session start for this project.
- After any correction from the user, update `lessons.md` with the pattern.
- Write rules for yourself that prevent the same mistake.

### Demand Elegance
- For non-trivial changes: pause and ask "is there a more elegant way?"
- If a fix feels hacky: "Knowing everything I know now, implement the elegant solution."
- Skip this for simple, obvious fixes — don't over-engineer.
- Challenge your own work before presenting it.

### Security Workflow
- Add a GitHub issue as a TODO to review the application with Codex Security.

## Functionality & Verification
- Always prove functionality.
- Every task needs to be tested before finishing it.
- **(IMPORTANT)** Do not add tests which simply restate the implementation. These provide zero confidence.
- Never mark a task complete without proving it works.
- Ask yourself: "Would a staff engineer approve this?"

## Cross-Module Rules
### Localization
- Frontend must support German (`de`) and English (`en`) for all user-facing text.
- Backend-facing text, API docs, and server-managed messages remain English unless a task explicitly requires localization.

### Location UX & Privacy
- First-visit country fallback must use browser locale and `Intl.DateTimeFormat().resolvedOptions().timeZone`, not IP geolocation.
- If browser locale has no region, default the fallback country to Germany.
- Frontend may persist only the selected country in `localStorage` for later visits. Never persist raw coordinates unless the user explicitly asks for a feature that requires it and the policy is updated first.
- Browser geolocation must be user-triggered through an explicit action such as `Use my city`. Never trigger geolocation automatically on page load.

### Security & CI
- Run `npm audit --audit-level=high` for frontend and `./gradlew dependencyCheckAnalyze` for backend in CI on every push.
- Never commit secrets, `.env` files, or production configs to the repository.
- TLS and HSTS are enforced at the infrastructure/deployment level. Never ship a build that downgrades to HTTP.

### Code Changes
- Always update `docs/documentation.md` after code changes affecting behavior or requirements.
- Update `docs/architecture.md` if architecture changes.
- Don't write everything into `docs/documentation.md` and `docs/architecture.md`, fill it only with needed elements and best practises for documentations and architectures.
- Add code comments only where they materially help: explain non-obvious intent, constraints, invariants, or tricky behavior. Do not narrate obvious code.
- Do not modify unrelated files.
- Always test the code after implementing new features or code changes.

#### .gitignore
- Always ask before removing something from `.gitignore`.
- Add necessary files and folders to `.gitignore` using public repository best practices.

## Out Of Scope
### Docker
- Do not create or modify Docker-related files until explicitly instructed.
- When Docker is introduced, use minimal base images, run containers as non-root, scan with Trivy, and provide local compose for the app and PostgreSQL.

### GitHub Actions
- Workflows live in `.github/workflows/`.

## Formatting Enforcement
- Frontend formatting: ESLint + Prettier.
- Backend formatting: Spotless.
- Do not introduce new formatting rules.

## Core Principles
- **Simplicity First**: Make every change as simple as possible. Impact minimal code.
- Keep READMEs, commits, issues, PRs, docs, and other project text simple, understandable to third parties, easy to follow, and free of unnecessary complexity.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.