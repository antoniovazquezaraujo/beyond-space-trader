
[🇪🇸 Leer en Español](index_es.md)

<div style="display: flex; justify-content: center; width: 100%; overflow: hidden; margin-top: 20px; margin-bottom: 20px;">
<pre style="font-size: 1.2em; line-height: 1.2; font-weight: bold; background: transparent; border: none; overflow: hidden; padding: 0; text-align: left;">
 ___ ___ _____
| _ ) __|_   _|
| _ \__ \ | |
|___/___/ |_|
    Beyond
 Space Trader
</pre>
</div>

<p align="center">
  <a href="https://github.com/antoniovazquezaraujo/beyond-space-trader/releases/latest">
    <img src="https://img.shields.io/badge/🎮_Download_Latest_Release-0078D4?style=for-the-badge&logo=github&logoColor=white" alt="Download Latest Release">
  </a>
  <a href="https://antoniovazquezaraujo.github.io/beyond-space-trader/">
    <img src="https://img.shields.io/badge/📖_Read_the_Documentation-2EA043?style=for-the-badge&logo=markdown&logoColor=white" alt="Read the Documentation">
  </a>
</p>

**Beyond Space Trader** is a Java remake of the classic Palm OS game **Space
Trader**, played entirely in a terminal. Trade between star systems, upgrade your
ship, take on quests, dodge pirates and police, and try to retire as a rich
commander.

The whole interface is keyboard driven and drawn with
[Lanterna](https://github.com/mabe02/lanterna): one window, panels on the right
and the star chart always in the middle.

## ✨ Features

- **Classic Space Trader gameplay:** buy low, sell high, upgrade your ship and
  survive the trip, with the rules of the original game.
- **A terminal interface:** the local and galactic charts, the panels and the
  encounters are drawn as text, in a palette of a few intentional colours.
- **Animated encounters:** the fight is a scene with the two ships facing each
  other; you fly, dodge and shoot, and the game still arbitrates every round.
- **Quests and special events:** alien artifacts, the Dragonfly, the Scarab, the
  Princess, the moon for sale in Utopia...
- **English and Spanish:** the game picks the system language and accepts
  `--lang es`; the documentation is bilingual too.

## 🚀 Quick start

1. Download the zip for your system from the
   [latest release](https://github.com/antoniovazquezaraujo/beyond-space-trader/releases/latest):
   `BeyondSpaceTrader-Linux.zip` or `BeyondSpaceTrader-Windows.zip`.
2. Unzip it anywhere. **You do not need Java installed**: the package includes
   its own runtime.
3. Launch the game:
   - **Linux/macOS:** `./bin/beyond-space-trader.sh`
   - **Windows:** `bin\beyond-space-trader.bat`

```bash
# ... and to play in Spanish:
./bin/beyond-space-trader.sh --lang es
```

The full instructions, the controls and the troubleshooting notes are in the
**[User Manual](manual.md)** ([Español](manual_es.md)).

## 📖 Documentation

- [User Manual](manual.md) — installation, how to play, controls, quests and tips.
- [Quick cheat sheet](cheatsheet.md) — keys and panels at a glance.
- [Developer documentation](../developer/README.md) — the architecture and the
  technical guides (*in English*, with the ADRs in Spanish).

## ⚖️ License

Beyond Space Trader is distributed under the **GNU General Public License v3.0 or
later**; see the `LICENSE` file for the full text and the `NOTICE` file for the
provenance chain. The original game, **Space Trader** (Palm OS), was created by
Pieter Spronck with artwork by Alexander Lawrence; this project continues the
*SpaceTrader for Java* port by Aviv Eyal and contributors.
