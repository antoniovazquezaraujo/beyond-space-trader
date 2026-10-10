
[🇪🇸 Leer en Español](cheatsheet_es.md)

# Beyond Space Trader — Quick Cheat Sheet

One page with the keys and panels of the game. The details are in the
[User Manual](manual.md).

## Run the game

| | |
| --- | --- |
| Linux/macOS | `./bin/beyond-space-trader.sh` |
| Windows | `bin\beyond-space-trader.bat` |
| In Spanish | add `--lang es` |
| Mute the sound | add `--mute` (or `--no-sound`) |
| From source | build with `mvn clean package -DskipTests`, run with `./run.sh` |

## The map

| Key | Action |
| --- | --- |
| Arrows / `hjkl` | Select the next system in that direction |
| `TAB` | Short-range chart / galactic chart |
| `ENTER` / `T` | Track the selected system (again: stop tracking) |
| `/` | Find a system by name |
| `SPACE` | Warp to the selected system (or through the wormhole) |
| `G` | Portable Singularity jump |
| `F` / `R` | Buy fuel / repair hull |
| `Y` | Special event of the current system |
| `Esc` | Close the panel; quit from the map |
| `n` / `p` | Newspaper / personnel on the map; down/up in the lists |

## The panels

| Panel | Keys |
| --- | --- |
| `C` Trade | `B` buy · `S` sell · `Shift+B`/`Shift+S` the maximum |
| `B` Bank | `G` loan · `P` pay back · `I` insurance |
| `Q` Quests | `ENTER` set the target · `SPACE` close |
| `N` Newspaper | Arrows scroll · `PageUp`/`PageDown` |
| `P` Personnel | `H` hire/fire |
| `I` Commander | `SPACE` close |
| `V` Ship and cargo | `SPACE` close |
| `S` Ships for sale | `B` buy |
| `E` Equipment | `B` buy · `S` sell |
| `D` Ship designer | `←`/`→` change · `R` name · `C` construct · `V` save |
| `O` Escape pod | 2,000 cr, asks first |
| `F2` / `F5` / `F9` | New game / save / load |
| `F3` / `F8` / `F10` | High scores / options (sound on/off) / menu |
| `A` About | Origin, authors and license |
| Any panel | `Esc` closes it (`SPACE` in the read-only ones) |
| Lists | `↑`/`↓` and also `n`/`p`, `j`/`k` |

Cargo transfer (jettison/plunder): `1`-`9`/`0` pick a slot, `Shift+digit` for all,
`Esc` to close.

## Encounters

| Key | Action |
| --- | --- |
| `SPACE` | Fire |
| `↑`/`↓` (or `k`/`j`) | Dodge |
| `→` (or `l`) | Close in (accept the offer) |
| `←` (or `h`) | Flee |
| `ENTER` | Natural action of the encounter / continue |
| `A` / `F` / `S` | Attack / Flee / Surrender |
| `B` / `U` / `Y` | Bribe / Submit / Yield (police) |
| `O` / `P` | Board (Marie Celeste) / Plunder |
| `M` / `T` / `D` / `I` | Meet / Trade / Drink / Ignore |
| `X` | Interrupt the automatic rounds |
| `Esc` | Leave the scene when it waits for you |

## Basics in three lines

- **Map:** `SPACE` travels (fuel + one day), `TAB` changes chart, `G` jumps once
  to anywhere.
- **Money:** buy low in `C`, sell high in `C`; the trade panel shows the margin
  with the systems in range.
- **End:** buy the moon in Utopia (500,000 cr) and return there to retire.
