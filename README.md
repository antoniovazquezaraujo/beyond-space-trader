```
 ___ ___ _____
| _ ) __|_   _|
| _ \__ \ | |
|___/___/ |_|
    Beyond
 Space Trader
```

# Beyond Space Trader

<p align="left">
  <a href="https://github.com/antoniovazquezaraujo/beyond-space-trader/releases/latest"><img src="https://img.shields.io/github/v/release/antoniovazquezaraujo/beyond-space-trader?include_prereleases&style=flat-square" alt="Latest release"></a>
  <a href="https://snapcraft.io/beyond-space-trader-tui"><img src="https://img.shields.io/badge/Snap%20Store-install-E95420?style=flat-square&logo=snapcraft&logoColor=white" alt="Snap Store"></a>
  <a href="https://avaraujo.itch.io/beyond-space-trader-tui"><img src="https://img.shields.io/badge/itch.io-download-FA5C5C?style=flat-square&logo=itch.io&logoColor=white" alt="itch.io"></a>
  <a href="https://antoniovazquezaraujo.github.io/beyond-space-trader/"><img src="https://img.shields.io/badge/docs-GitHub%20Pages-2EA043?style=flat-square&logo=github" alt="Documentation"></a>
  <a href="https://github.com/antoniovazquezaraujo/beyond-space-trader/actions/workflows/build.yml"><img src="https://github.com/antoniovazquezaraujo/beyond-space-trader/actions/workflows/build.yml/badge.svg?branch=develop" alt="Build status (develop)"></a>
  <a href="https://www.gnu.org/licenses/gpl-3.0.html"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square" alt="License: GPL v3"></a>
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17">
</p>

A Java port of the classic Palm OS game **Space Trader**, played entirely in a
terminal with a [Lanterna](https://github.com/mabe02/lanterna) UI and structured
around Model-View-Presenter. Trade between star systems, upgrade your ship, take
on quests, dodge pirates and police, and try to retire to your own moon in
Utopia.

## 🎮 Download and play (no Java needed)

1. Open the [latest release](https://github.com/antoniovazquezaraujo/beyond-space-trader/releases/latest)
   and download the zip for your system:
   `BeyondSpaceTrader-Linux.zip` or `BeyondSpaceTrader-Windows.zip`.
2. Unzip it anywhere: the package carries its own trimmed runtime.
3. Run it:
   - **Linux/macOS:** `./bin/beyond-space-trader.sh`
   - **Windows:** `bin\beyond-space-trader.bat`

```bash
# To play in Spanish (English is the fallback):
./bin/beyond-space-trader.sh --lang es
```

### Other ways to install

- **Snap Store (Linux):** `sudo snap install beyond-space-trader-tui`. The snap
  updates itself and keeps its saves and the editable ship art in
  `~/snap/beyond-space-trader-tui/current/game`.
- **itch.io:** the same Linux and Windows packages are available at
  <https://avaraujo.itch.io/beyond-space-trader-tui>.

The game needs a terminal of at least 60×15 (100×30 or more is recommended;
120×30 fits the encounter scene comfortably).

## 📖 Documentation

- **User Manual:** [English](docs/user/manual.md) · [Español](docs/user/manual_es.md)
- **Cheat sheet:** [English](docs/user/cheatsheet.md) · [Español](docs/user/cheatsheet_es.md)
- **Online documentation:** <https://antoniovazquezaraujo.github.io/beyond-space-trader/>
- **Developer documentation:** [docs/developer/README.md](docs/developer/README.md)
  (architecture, ship art, encounters, design and release process; the technical
  guides are in English and the ADRs in Spanish).

## 🛠️ Build from source

Requires **JDK 17** and **Maven 3.9+**.

```bash
mvn clean package -DskipTests # build the self-contained package in output/BeyondSpaceTrader
./run.sh                      # run what is built (it does not build)
./run.sh --lang es            # ... and start it in Spanish
mvn -B -ntp -Pquality verify  # tests + SpotBugs (what CI runs)
```

`run.sh` only runs the game: it prefers the package in `output/BeyondSpaceTrader`
(its own Java runtime and ship art) and falls back to the jar. `run-fixed-font.sh`
and `run-composer.sh` work the same way; the first one starts the game in a
terminal with a fixed monospace font (kitty, xfce4-terminal or alacritty), so
the charts keep their proportions whatever the desktop font is.

## 🧭 Status

Work in progress, and playable: the whole UI runs on Lanterna, the model is
decoupled from the views through presenters and the releases ship standalone
packages for Linux and Windows. The roadmap lives in the
[developer documentation](docs/developer/README.md).

## 🧾 Provenance

This repository starts from the upstream snapshot `spacetraderjava-code-r69`
(tag `upstream-r69`). The old Swing/JWinForms front-end was removed in the
Lanterna port and is preserved under the tag `swing-final`. See the
[NOTICE](NOTICE) file for the full provenance chain:

- Original game: **Space Trader** (Palm OS), by Pieter Spronck, with artwork by
  Alexander Lawrence.
- Windows port: **Space Trader for Windows**, by Jay French, with additional
  coding by David Pierron; original coding by Pieter Spronck, Sam Anderson,
  Samuel Goldstein and Matt Lee.
- Java port: **SpaceTrader for Java**, by Aviv Eyal and contributors (the
  upstream snapshot this repository continues).

## ⚖️ License

GNU General Public License v3.0 or later (`GPL-3.0-or-later`). See
[LICENSE](LICENSE) for the license text and [NOTICE](NOTICE) for the upstream
copyright and provenance chain.
