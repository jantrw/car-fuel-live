# Project Agent Instructions
# Scope: repository root

## Documentation Sources
- Product spec lives in `docs/documentation.md`.
- Architecture lives in `docs/architecture.md`.
- Backend rules live in `car-fuel-live-backend/AGENTS.md`.
- Frontend rules live in `car-fuel-live-frontend/AGENTS.md`.

## Repo Workflow
- Enter plan mode for any non-trivial task (3+ steps or architectural decisions).
- If something goes sideways, stop and re-plan immediately.
- Use plan mode for verification steps, not just building.
- Divide every plan into sequential phases. Each phase maps to exactly one GitHub issue.
- Create all issues via the GitHub CLI before writing any code unless explicitly waived by the user.
- No project boards, milestones, or labels — issues only.

### Task Management
1. **Plan First**: Write plan to `todo.md` with checkable items.
2. **Verify Plan**: Check in before starting implementation.
3. **Track Progress**: Mark items complete as you go.
4. **Explain Changes**: High-level and understanding summary at each step.
5. **Document Results**: Add review section to `todo.md`.
6. **Capture Lessons**: Update `lessons.md` after corrections.

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
- Never mark a task complete without proving it works.
- Ask yourself: "Would a staff engineer approve this?"

## Cross-Module Rules
### Security & CI
- Run `npm audit --audit-level=high` for frontend and `./gradlew dependencyCheckAnalyze` for backend in CI on every push.
- Never commit secrets, `.env` files, or production configs to the repository.
- TLS and HSTS are enforced at the infrastructure/deployment level. Never ship a build that downgrades to HTTP.

### Code Changes
- Always update `docs/documentation.md` after code changes affecting behavior or requirements.
- Update `docs/architecture.md` if architecture changes.
- Don't write everything into `docs/documentation.md` and `docs/architecture.md`, fill it only with needed elements and best practises for documentations and architectures.
- Do not modify unrelated files.
- Always test the code after implementing new features or code changes.

### Git
- Every issue or feature must live in its own dedicated branch. Do not combine unrelated changes in one branch.
- Branch names must follow `<type>/<issue-id>-<short-description>` using lowercase and hyphens only.
- All work branches must merge into `dev` first. `dev` may only merge into `main` after explicit approval.
- Never open a pull request or merge a branch without asking first.

#### Commit Scopes
`api` | `db` | `map` | `prices` | `auth` | `docker` | `config`

Examples:
`feat(api): add Tankerkönig price polling endpoint`
`fix(db): correct station coordinate mapping on insert`
`refactor(map): extract marker logic into composable`
`chore(docker): pin postgres image to 17.2`

#### Issue Labels
- In addition to global labels: `map` | `polling`

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
