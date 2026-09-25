# Contributing

This project follows **GitHub Flow**:

- `main` is always buildable; do not commit to it directly.
- Create a short-lived branch per change, with a prefix:
  `feature/…`, `fix/…`, `refactor/…`, `docs/…`, `build/…`, `test/…`.
- Open a pull request. Keep it small and focused on one change.
- CI (`mvn -B verify`) must pass.
- Merge with **squash** and let the branch be deleted.
- Reference the issue being closed in the PR body (`Closes #12`).

## Commit messages

Use the imperative mood and, when it helps, a Conventional Commits style
prefix: `feat:`, `fix:`, `refactor:`, `docs:`, `build:`, `test:`.

## Issues

The issue tracker is the backlog. The roadmap is tracked with milestones
(`MVP`, `Lanterna`) and labels (`bug`, `build`, `refactor`, `ui`, `lanterna`,
`license`).

## Building

Requires JDK 17 and Maven. `mvn package` builds both modules and creates a
runnable jar; `./run.sh` compiles and runs the game from sources. See the
[README](README.md) for details.
