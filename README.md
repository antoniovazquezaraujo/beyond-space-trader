# Beyond Space Trader

A Java port of the classic Palm OS game **Space Trader**, based on the C# port
[Space Trader for Windows](https://sourceforge.net/projects/spacetraderwin/) and on the
SourceForge Java port [SpaceTrader for Java](https://sourceforge.net/projects/spacetraderjava/).
It is a terminal (TUI) application built with
[Lanterna](https://github.com/mabe02/lanterna), structured around Model-View-Presenter.
The old Swing/JWinForms front-end was removed in the Lanterna port and is preserved
under the tag `swing-final`.

This repository starts from the upstream snapshot `spacetraderjava-code-r69`
(tag `upstream-r69`).

## Status

Work in progress. The game builds and runs with the Lanterna text UI; the imported bugs
are fixed and the model is decoupled from the views through Model-View-Presenter (view
interfaces, view models and presenters with headless tests).

Roadmap:

- [x] Fix startup issues (singleton, look & feel, resource paths)
- [x] Remove the debug cheat from the `Game` constructor
- [x] Replace the NetBeans/Ant build with Maven and drop JNLP/WebStart
- [x] Refactor towards Model-View-Presenter
- [x] Port the UI to Lanterna and remove the Swing/JWinForms front-end
- [ ] ASCII-art ship sprites (standard ships and custom ship designer)

## Layout

| Module | Contents |
|---|---|
| `BeyondSpaceTraderJava/` | The game. Main class: `org.gts.bst.lanterna.LanternaApp`. |

It is a Maven project (`pom.xml` at the repository root). The old NetBeans/Ant build
files and the Swing/JWinForms front-end were removed and remain available in git history
(tags `upstream-r69` and `swing-final`).

## Building and running

Requires JDK 17 and Maven 3.9+.

```bash
./run.sh                                     # build and run the terminal UI
./run-fixed-font.sh                          # run in a terminal with a fixed font (kitty/xfce4/alacritty)
java -jar BeyondSpaceTraderJava/target/beyond-space-trader-0.1.0-SNAPSHOT.jar

mvn package                                  # build the jar
mvn -Pquality verify                         # tests and SpotBugs (the gate fails on findings)
```

The game needs a terminal of at least 60x15 (100x30 or bigger is recommended). The
maps assume the usual monospace cell (about twice as tall as wide); if the galaxy
looks stretched, change *Galaxy chart columns per sector* in Options (`F8`).

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
the placeholders. Drop the file in the resources directory and the game picks it up:
it follows the system locale automatically (for example `LANG=es_ES.UTF-8`) and the
`--lang` argument overrides it (`./run.sh --lang es`, `--lang es_ES`, `--lang=en`).
Texts without a translation fall back to English.

## Credits

- Original game: **Space Trader** (Palm OS), by Pieter Spronck, with artwork by
  Alexander Lawrence.
- Windows port: **Space Trader for Windows**, by Jay French, with additional
  coding by David Pierron; original coding by Pieter Spronck, Sam Anderson,
  Samuel Goldstein and Matt Lee.
- Java port: **SpaceTrader for Java**, the upstream snapshot this repository
  continues (see `NOTICE` for the full provenance chain).

## License

GNU General Public License v3.0 or later (`GPL-3.0-or-later`).
See [LICENSE](LICENSE) for the license text and [NOTICE](NOTICE) for the upstream
copyright and provenance chain.
