# Beyond Space Trader

A Java port of the classic Palm OS game **Space Trader**, based on the C# port
[Space Trader for Windows](https://sourceforge.net/projects/spacetraderwin/) and on the
SourceForge Java port [SpaceTrader for Java](https://sourceforge.net/projects/spacetraderjava/).
The long-term goal is to turn it into a terminal (TUI) application built with
[Lanterna](https://github.com/mabe02/lanterna), structured around Model-View-Presenter.

This repository starts from the upstream snapshot `spacetraderjava-code-r69`
(tag `upstream-r69`).

## Status

Work in progress. The game builds and runs, the imported bugs are fixed and the model is
decoupled from the Swing front-end through Model-View-Presenter (view interfaces, view
models and presenters with headless tests). The UI port to Lanterna is the next milestone.

Roadmap:

- [x] Fix startup issues (singleton, look & feel, resource paths)
- [x] Remove the debug cheat from the `Game` constructor
- [x] Replace the NetBeans/Ant build with Maven and drop JNLP/WebStart
- [x] Refactor towards Model-View-Presenter
- [ ] Port the UI to Lanterna

## Layout

| Module | Contents |
|---|---|
| `BeyondSpaceTraderJava/` | The game. Main class: `org.gts.bst.ApplicationST`. |
| `JWinForms/` | A WinForms-over-Swing compatibility layer used by the game. Planned for removal. |

Both are Maven modules of a single reactor (`pom.xml` at the repository root). The old
NetBeans/Ant build files were removed during the Maven migration and remain available
in git history (tag `upstream-r69`).

## Building and running

Requires JDK 17 and Maven 3.9+.

```bash
./run.sh                                     # compile and run from sources (Swing)
./run-text.sh                                # run with the Lanterna text UI (work in progress)
java -jar BeyondSpaceTraderJava/target/beyond-space-trader-0.1.0-SNAPSHOT.jar

mvn package                                  # build both modules and the jar
mvn -Pquality verify                         # also run SpotBugs (the gate fails on findings)
```

On non-Windows platforms the default UI fonts are shrunk to 10 pt, because the JWinForms
layouts were tuned for Microsoft Sans Serif 8.25 and fixed-size labels clip otherwise.
Override the size with `-Dbst.uiFontSize=<points>` (`0` disables the adjustment):

```bash
java -Dbst.uiFontSize=11 -jar BeyondSpaceTraderJava/target/beyond-space-trader-0.1.0-SNAPSHOT.jar
```

## Design

The Lanterna interface design (one window, panels, keyboard first) is described in
[docs/ui-design.md](docs/ui-design.md).

## Translations

The game texts live in
[`BeyondSpaceTraderJava/src/main/resources/spacetrader/Strings.properties`](BeyondSpaceTraderJava/src/main/resources/spacetrader/Strings.properties).
`^1`, `^2`... are placeholders filled at runtime and the table entries use indexed
keys (`Name.0`, `Name.1`..., `Name.row.col` for the two-dimensional ones). To
translate the game, copy the file to `Strings_<language>.properties` next to it
(for example `Strings_es.properties`) and translate the values, keeping the keys and
the placeholders. Drop the file in the resources directory and run the game with that
locale (`java -Duser.language=es ...`).

## License

GNU General Public License v3.0 or later (`GPL-3.0-or-later`).
See [LICENSE](LICENSE) for the license text and [NOTICE](NOTICE) for the upstream
copyright and provenance chain.
