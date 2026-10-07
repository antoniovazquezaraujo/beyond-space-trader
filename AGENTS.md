# AGENTS — Beyond Space Trader

Project profile for the agent team (Dani, Alex, Bicho, Jorge). Also see
[CONTRIBUTING.md](CONTRIBUTING.md) and [README.md](README.md).

## What this is

A Java port of *Space Trader*, on its way to a terminal UI with **Lanterna**.
GPL v3 license. Repo: `github.com/antoniovazquezaraujo/beyond-space-trader`.

## Stack and versions

- **Java 17 exactly** (`maven.compiler.release=17`; CI uses Temurin 17). Do not use APIs from 18+.
- **Maven multi-module**: parent `pom.xml` + `BeyondSpaceTraderJava` module.
- **Lanterna 3.1.5** for the terminal UI.
- **JUnit 5 (Jupiter 5.11.4)**. **No Mockito**: use plain assertions and hand-written fakes.

## Commands

```sh
mvn package                   # build and produce the jar
./run.sh                      # build and start the TUI
mvn verify                    # tests
mvn -B -ntp -Pquality verify  # what CI runs (tests + SpotBugs)
```

## Tests (important rules)

- The `quality` profile runs **SpotBugs** with `failOnError=true` (effort Max, threshold Medium).
  Findings are either fixed or explicitly excluded in `config/spotbugs-exclude.xml`.
- Surefire runs tests with an **English locale** (`-Duser.language=en -Duser.country=US`)
  and **`java.awt.headless=true`**: tests **must not open windows** or depend on the
  machine's regional settings.
- Localization: tests read the English resource bundle; do not assert on text in other languages.
- Layout: `src/test/java/spacetrader/*Test.java` (model/business),
  `org.gts.bst.presenter/*PresenterTest.java` (MVP), `org.gts.bst.view/*Test.java` (UI with
  Lanterna virtual terminals).

## Architecture

- **MVP** pattern: `org.gts.bst.presenter` (presenters) + `org.gts.bst.view`
  (`*View` interfaces and `*ViewModel`s). The game engine lives in `spacetrader.*`
  (port of the original: `Game`, `Ship`, `Trade`, `UniverseGenerator`…).
- **Lanterna** UI only in `org.gts.bst.view` / `lanterna`; the model must not
  depend on the UI.
- Developer documentation in `docs/developer/`: `ui-design.md`, `ships.md`,
  `encounters.md`, the ADRs under `adr/` and the release process under
  `release/`. See `docs/developer/README.md`.
- Player documentation in `docs/user/` (manual and cheat sheet, in English and
  Spanish); it is published to GitHub Pages. If a change is visible to players,
  update it in the same PR.

## Architecture decisions (ADRs)

In `docs/developer/adr/`, numbered (`0001`, `0002`…) and **never rewritten**: when
a decision changes, a new ADR is added that supersedes the old one, which is marked
as *superseded* in its header. See `docs/developer/adr/README.md`. ADRs are written
in Spanish; follow their format.

## Process (GitHub Flow)

- `develop` is the default and protected branch: **never commit directly to it**;
  every change goes through a PR.
- One short-lived branch per change, created from `develop`, with a prefix:
  `feature/… fix/… refactor/… docs/… build/… test/…`.
- Keep PRs **small and focused** on one change; write them in English. CI
  (`mvn -B -ntp -Pquality verify`) must be green.
- Merge with **squash** and delete the branch (local and remote) once integrated.
- **Merges into `develop` wait for the maintainer**: the agents open the PR with
  CI green and stop there; only the maintainer approves or merges it. Work that
  must not reach `develop` yet lives on an integration branch (e.g.
  `refactor/game-decomposition`) and lands as a single PR when it is complete.
- `main` only receives releases (a PR from `develop` plus a tag); see
  `docs/developer/release/Release_Process.md`.
- Write commits in the imperative mood, with a Conventional Commits prefix when it helps
  (`feat:`, `fix:`, `refactor:`, `docs:`, `build:`, `test:`).
- Reference the issue being closed in the PR body (`Closes #12`).
- If a change is visible to players, update `docs/user/` in the same PR.
