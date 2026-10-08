# Contributing

This project follows **GitHub Flow** around the `develop` branch:

- `develop` is the **default and protected branch**: no direct pushes; every
  change goes through a pull request.
- `main` only receives releases. Publishing a release is a PR from `develop` to
  `main` followed by a version tag; the full process is in
  [docs/developer/release/Release_Process.md](docs/developer/release/Release_Process.md).
- Create a short-lived branch **from `develop`** for each change, with a prefix:
  `feature/…`, `fix/…`, `refactor/…`, `docs/…`, `build/…`, `test/…`.
- Open a **pull request in English**. Keep it small and focused on one change.
- CI (`mvn -B -ntp -Pquality verify`) must pass.
- Merge with **squash** and delete the branch (local and remote) once
  integrated.
- **Merging into `develop` is the maintainer's call**: open the PR, leave CI
  green and wait for the review; agents and bots never merge into `develop` on
  their own. Big refactors can live on an **integration branch** (for example
  `refactor/game-decomposition`): its pieces merge there and the whole branch
  lands on `develop` as one reviewed PR.
- Reference the issue being closed in the PR body (`Closes #12`).

## Player-visible changes

If a change is visible to players, update the public documentation in
**`docs/user/`** (the [manual](docs/user/manual.md) and its
[Spanish version](docs/user/manual_es.md), plus the cheat sheets) in the same
pull request. The Pages workflow republishes `docs/user/` on every push to
`develop`.

## Commit messages

Use the imperative mood and, when it helps, a Conventional Commits style
prefix: `feat:`, `fix:`, `refactor:`, `docs:`, `build:`, `test:`.

## Issues

The issue tracker is the backlog. The roadmap is tracked with milestones
(`MVP`, `Lanterna`) and labels (`bug`, `build`, `refactor`, `ui`, `lanterna`,
`license`).

## Building

Requires JDK 17 and Maven. `mvn clean package -DskipTests` builds the
self-contained package in `output/BeyondSpaceTrader`; `./run.sh` runs what is
built (it does not build). See the [README](README.md) for details and the
[developer documentation](docs/developer/README.md) for the architecture.
