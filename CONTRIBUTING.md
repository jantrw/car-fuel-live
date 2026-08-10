# Contributing

## Branches and merges

Create work branches from `dev` using:

```text
<type>/<issue-id>-<short-description>
```

Open work pull requests against `dev` and merge them using Squash Merge. One pull request therefore produces one functional commit in `dev`.

Release pull requests merge `dev` into `main` using a Merge Commit. Do not use Squash Merge or Rebase Merge for this step.

After the release merge, fast-forward `dev` to the resulting `main` commit:

```sh
git fetch origin
git switch dev
git merge --ff-only origin/main
git push origin dev
```

`main` accepts changes only through pull requests. The required `backend` and `frontend` CI checks must pass. Force pushes and branch deletion are prohibited.

## Versioning

Car Fuel Live follows Semantic Versioning using `MAJOR.MINOR.PATCH`. The first release is `0.1.0`.

- `MAJOR`: incompatible public API, deployment, or data-contract changes after `1.0.0`.
- `MINOR`: backward-compatible functionality. Before `1.0.0`, intentional incompatible changes also increment `MINOR` and must be documented in the release notes.
- `PATCH`: backward-compatible fixes and security updates.

The release tag is the single source of truth for the source code, backend, frontend, container images, deployment bundle, and GitHub Release. Release tags use `vMAJOR.MINOR.PATCH`, for example `v0.1.0`.

## Releasing

A release requires a reviewed `dev` to `main` pull request and successful required CI checks.

After merging:

1. Verify that the merge commit is contained in `main`.
2. Create the release tag on that exact merge commit.
3. Push the tag explicitly.

```sh
git tag -a v0.1.0 <merge-commit> -m "Release 0.1.0"
git push origin v0.1.0
```

Creating or merging a pull request does not publish a release. Pushing the tag is the deliberate release approval.

Published version tags are immutable. Never move, replace, or reuse a release tag.
