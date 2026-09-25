# Beyond Space Trader

A Java port of the classic Palm OS game **Space Trader**, based on the C# port
[Space Trader for Windows](https://sourceforge.net/projects/spacetraderwin/) and on the
SourceForge Java port [SpaceTrader for Java](https://sourceforge.net/projects/spacetraderjava/).
The long-term goal is to turn it into a terminal (TUI) application built with
[Lanterna](https://github.com/mabe02/lanterna), structured around Model-View-Presenter.

This repository starts from the upstream snapshot `spacetraderjava-code-r69`
(tag `upstream-r69`).

## Status

Work in progress. The imported snapshot **does not run out of the box**; these are the
known blockers:

- Starting or loading a game throws `NullPointerException` because the
  `Game.CurrentGame()` singleton is never assigned (`Game.java`).
- The main window hardcodes the Windows look & feel
  (`com.sun.java.swing.plaf.windows.WindowsLookAndFeel`), which does not exist outside Windows.
- The image-list resources are looked up under the wrong package path and the app
  aborts while building the main window.
- New games start with a leftover debug cheat (1,000,000 credits, easy encounters,
  super warp) in the `Game` constructor.

Roadmap:

- [ ] Fix startup issues (singleton, look & feel, resource paths)
- [ ] Remove the debug cheat from the `Game` constructor
- [ ] Replace the NetBeans/Ant build with Gradle and drop JNLP/WebStart
- [ ] Refactor towards Model-View-Presenter
- [ ] Port the UI to Lanterna

## Layout

| Directory | Contents |
|---|---|
| `BeyondSpaceTraderJava/` | The game (NetBeans Ant project). Main class: `org.gts.bst.ApplicationST`. |
| `JWinForms/` | A WinForms-over-Swing compatibility layer used by the game. Planned for removal. |

## Building

The original build is a NetBeans Ant project and requires JDK 8-17: the project sets
`javac.source=1.7`, which JDK 21 and later reject. `JWinForms` must be built first,
because `BeyondSpaceTraderJava` references `../JWinForms/dist/JWinForms.jar`.

Until the startup fixes land, running the game requires workarounds (a launcher with a
portable look & feel and a copy of the image-list properties under `org/gts/bst/`).

## License

GNU General Public License v3.0 or later (`GPL-3.0-or-later`).
See [LICENSE](LICENSE) for the license text and [NOTICE](NOTICE) for the upstream
copyright and provenance chain.
