
[🇪🇸 Leer en Español](manual_es.md)

# Beyond Space Trader — User Manual

Welcome to the official manual of **Beyond Space Trader**. Here you will find
everything you need to install the game, fly between the stars, trade, fight,
finish the quests and retire as a rich commander.

## 1. What is Beyond Space Trader

Beyond Space Trader is a terminal remake of the classic **Space Trader** (Palm
OS, 2002), built with [Lanterna](https://github.com/mabe02/lanterna). You start
with a small ship and 1,000 credits and make your way up as a trader, a bounty
hunter, a pirate or whatever gets you to the moon in Utopia.

The game is played entirely with the keyboard: one window, the star chart in the
middle, a context panel on the right and a line of keys at the bottom. Every
panel is described in this manual, and the [cheat sheet](cheatsheet.md) has the
keys at a glance.

## 2. Installation and running

### 2.1 From a release (no Java needed)

1. Open the
   [latest release](https://github.com/antoniovazquezaraujo/beyond-space-trader/releases/latest)
   and download the package for your system:

   | System | File |
   | --- | --- |
   | Linux/macOS | `BeyondSpaceTrader-Linux.zip` |
   | Windows | `BeyondSpaceTrader-Windows.zip` |

2. Unzip it anywhere. The package carries its **own trimmed Java runtime**
   (`lib/runtime`), so **you do not need Java installed**.
3. Launch the game from the unzipped folder:

   - **Linux/macOS:** `./bin/beyond-space-trader.sh`
   - **Windows:** `bin\beyond-space-trader.bat`

The launchers change to the package folder before starting, because the game
reads the ship artwork (`ships/`) from the working directory. The package is:

```
BeyondSpaceTrader/
├── bin/      the launchers
├── lib/      the game jar and the private JRE
├── ships/    the editable ship art (chassis, pieces and ships)
├── LICENSE, NOTICE, README.txt
```

The game creates its own folders on the first run, next to the launchers:
`save/` (saved games and autosaves), `data/` (high scores and defaults) and
`custom/` (ship templates).

To play in Spanish, the only translation bundled today, pass the `--lang`
argument:

```bash
./bin/beyond-space-trader.sh --lang es          # Linux/macOS
bin\beyond-space-trader.bat --lang es           # Windows
```

`--lang` also accepts a region (`--lang es_ES`, `--lang es-ES`) and the
`--lang=es` form. The game uses the system language by default; texts without a
translation fall back to English.

### Other install options

- **Snap Store (Linux):** the game is also published as a snap:

  ```bash
  sudo snap install beyond-space-trader-tui
  ```

  Snap installs update themselves. The snap runs the game from its own data
  folder (`~/snap/beyond-space-trader-tui/current/game`), where `save/`, `data/`
  and an editable copy of `ships/` live.

- **itch.io:** the same Linux and Windows packages are available at
  <https://avaraujo.itch.io/beyond-space-trader-tui>.

### 2.2 From source

Requires **JDK 17** and **Maven 3.9+**:

```bash
mvn clean package -DskipTests  # build the self-contained package
./run.sh                       # run what is built (it does not build)
./run.sh --lang es             # ... in Spanish
mvn -Pquality verify           # tests and static analysis
```

`run-fixed-font.sh` is a **development helper**: it starts the game you built in
kitty, xfce4-terminal or alacritty with a fixed monospace font (DejaVu Sans
Mono 12), so the charts keep their proportions whatever the desktop font is. It
is not needed to play the released packages.

## 3. First steps

### 3.1 The title screen

The game opens on the title screen, with the game splash (or the text logo when
the terminal is small). Any key enters the program; from there:

| Key | Action |
| --- | --- |
| `F2` | New game |
| `F9` | Load a saved game |
| `F3` | High scores |
| `F8` | Options |
| `A` | About (origin, authors, license) |
| `F10` | Menu |
| `Esc` | Quit |

### 3.2 A new commander

`F2` asks for four things:

1. **Name** (Antonio by default; press ENTER to keep it).
2. **Difficulty**: Beginner, Easy, Normal, Hard or Impossible. It changes the
   encounter odds, the combat fraction and the starting conditions; Beginner is
   the only one where fleeing is always unharmed.
3. **Skill points**: you distribute **16 extra points** among **Pilot**
   (dodging and initiative), **Fighter** (aim and damage), **Trader** (prices)
   and **Engineer** (repairs and damage reduction). Every skill starts at 1 and
   no skill can go above 10, so type how many extra points each one gets.
4. The game starts: a **Gnat** with a **pulse laser** and **1,000 credits**, in a
   random mid-tech system with at least three systems within fuel range.

You can abandon a game and start another one at any time with `F2` (the game
asks for confirmation when a game is in progress). Save with `F5`, load with
`F9`; the panel `F5`/`F9` use is a file browser over the `save/` folder.

### 3.3 The screen

```
+--------------------------------------------------------------------------+
| Antonio · Day 12 · 12.345 cr · Debt 0                                    |
| Fuel 14/15 · Hull 25/25 · Shields 0/0 · Cargo 3/10 · Police: Clean       |
+-------------------------------------+------------------------------------+
|                                     | Acamar · T6 · Democracy            |
|              MAP                    | Water  30/ 35   buy/sell           |
|        (local / galactic)           | Furs  250/265   buy/sell           |
|                                     |                                    |
+-------------------------------------+------------------------------------+
| Last message                                                             |
| [TAB] map · [C] trade · ... · [F10] menu                                 |
+--------------------------------------------------------------------------+
```

- **Header (top):** your name, the day, your cash, your debt, and the fuel,
  hull, shields, cargo bays and police record of your ship. It takes one line
  when everything fits and two otherwise.
- **Chart (centre):** the short-range chart (with system names) or the galactic
  chart (the whole galaxy, no names). `TAB` switches between them.
- **Panel (right):** the context panel: navigation data in the current system,
  the trade table, the bank, the shipyard, the quests... `Esc` (and `Space` in
  the read-only panels) closes it.
- **Footer:** the last messages and the keys available right now. The navigation
  footer lists the daily keys plus the actions available in the current system.

The context panel uses its own width and never hides the chart on a wide
terminal; below 120 columns a list panel takes the whole width until it closes.

### 3.4 Terminal size

The game needs a terminal of **at least 60×15** (below that it says *Window too
small*). **100×30 or more is recommended**, and **120×30** makes the encounter
scene fit comfortably. If the galaxy looks stretched, change the
*Galaxy chart columns per sector* option (`F8`) between 1, 2 and 3 to match your
font.

## 4. Flying

### 4.1 The chart and the target

The chart is the axis of the game. Use the **arrow keys** (or `h`, `j`, `k`, `l`)
to select the next system in that direction; `TAB` switches between the
short-range chart (1:1, with names) and the galactic chart (the whole galaxy,
with markers and the current fuel circle). The chart shows states at a glance:

- A **green braille ring** around your system marks your actual jump range (your
  current fuel); systems inside it are green.
- The **selected** system carries parentheses `(•)`, the **tracked** system
  brackets `[•]` (nested `[(•)]` when both apply) and the current system is drawn
  inverted. A system with a **wormhole** is a circled dot `◉`, and the link
  between a wormhole and its pair is drawn as a magenta L.
- Unvisited systems are bright; visited ones are dim.

| Key | Action |
| --- | --- |
| Arrows / `hjkl` | Select the next system in that direction |
| `TAB` | Short-range chart / galactic chart |
| `ENTER` or `T` | Track the selected system (`T` again stops tracking it) |
| `/` | Find a system by name and select it (asks which one when several match) |
| `SPACE` | Warp to the selected system (or through the wormhole; see below) |
| `G` | Jump with the Portable Singularity |

When you select a system, the navigation panel shows its data (technology,
politics, resource, police and pirates), its distance and its standard prices.
Tracking a system shows its distance in the footer and draws its range on the
chart while the *Show range to tracked system* option is on.

### 4.2 Travelling

`SPACE` warps to the selected system. A trip:

- **costs fuel** equal to the distance in parsecs (the navigation panel warns
  `out of range` when you cannot reach it), and
- **takes one day**, during which the game resolves the trip in 20 clicks and
  **an encounter may happen** (see section 6).

A **wormhole** is a shortcut between two distant systems. When the cursor is on
the **current** system and it has a wormhole (`Wormhole to <system>` in the
navigation panel), `SPACE` crosses to its pair: it **spends no fuel**, but still
takes one day. Wormholes are drawn on the chart and announced in the news.

The **Portable Singularity** (`G`) is a one-shot quest reward: it teleports you
to **any selected system** instantaneously, without spending fuel or days. It
works only while it is on board and it is consumed after the jump.

Fuel and repairs:

| Key | Action |
| --- | --- |
| `F` | Buy fuel (asks how much; limited by your cash and the tank) |
| `R` | Repair the hull (asks how much; limited by your cash) |
| `F8` | Options; *Get full fuel tanks on arrival* and *Get full hull repairs on arrival* do it automatically |

### 4.3 The daily keys

The bottom line of the navigation screen shows the direct keys. Besides the ones
covered in this section: `I` commander, `V` ship and cargo, `C` trade, `B` bank,
`Q` quests, `N` newspaper, `P` personnel, `S` ships for sale, `E` equipment,
`D` ship designer, `O` escape pod, `A` about, `/` find, `F10` menu. The actions
that only make sense in the current system appear in the same line: `[Y] event`,
`[G] jump`, `[S] ships`, `[E] equipment`, `[D] design`, `[O] pod`, `[P] crew`.

The **special event** (`Y`) is described in section 7. The **escape pod** (`O`)
costs 2,000 credits; with one on board, losing a fight is not the end.

## 5. Trade, the ship and the panels

### 5.1 Trade (`C`)

The trade panel shows, for every item, its **buy** and **sell** price in the
current system, your cargo and your cash. When a system within range is selected
in the chart, the panel also shows its prices and the profit margin, so you can
plan the trip before undocking.

| Key | Action |
| --- | --- |
| `↑` `↓` (or `n` / `p`, `j` / `k`) | Select an item |
| `B` | Buy (asks for the amount: type it and press ENTER, `Esc` cancels) |
| `S` | Sell (asks for the amount) |
| `Shift+B` / `Shift+S` | Buy/sell the maximum without asking |
| `Esc` | Close the panel |

The panels of the game always use the same list keys: `↑`/`↓`, the arrows, and
`n`/`p` (with `j`/`k` as well) to move down and up.

### 5.2 Bank (`B`)

Ask for a **loan** and **pay it back** — the debt grows with interest every day,
so watch the header. The bank also sells **insurance**: with it on, losing the
ship pays you part of its worth (it is lost if you are arrested).

| Key | Action |
| --- | --- |
| `G` | Get a loan (asks the amount) |
| `P` | Pay back debt (asks the amount) |
| `I` | Toggle insurance |

### 5.3 Ships for sale (`S`) and equipment (`E`)

Both panels need the system's tech level to be high enough; the footer shows
them only when they are available.

**Ships for sale** lists the ships you can buy with their price and specs, the
hull/shield/weapon/gadget slots and the trade-in value of your current ship.
Ship size fixes what you can install: a bigger ship has more slots, more cargo
bays and more fuel tanks. `B` buys the selected ship (it asks for confirmation):
the trade-in value of your old ship is deducted, the crew moves to the new one
and installed equipment can be transferred for a fee when there is a slot for
it. Special quest equipment (Morgan's laser, the quantum disruptor, the
lightning shield, the fuel compactor, the hidden compartments) has to be
transferred too, and the escape pod as well.

**Equipment** sells weapons (lasers and disruptors), shields and gadgets; it also
buys your old equipment back.

| Key | Action |
| --- | --- |
| `↑` `↓` (or `n`/`p`) | Select an item |
| `B` | Buy |
| `S` | Sell |
| `Esc` | Close |

Weapons cost a slot each; lasers damage the hull, while **disruptors disable**
the opponent (photon and quantum) and only scratch the hull. Shields absorb the
first hits; gadgets add special abilities.

### 5.4 The ship designer (`D`)

At systems with a **shipyard** you can design and construct a custom ship: pick
the size and a template, name it, and trade cargo bays, fuel tanks, hull
strength, weapon/shield/gadget slots and crew quarters. The panel shows the
price of every change, the fee, the penalty and the trade-in of your current
ship, with the final total.

| Key | Action |
| --- | --- |
| `↑` `↓` | Select a field |
| `←` `→` | Change the value |
| `R` | Rename the design |
| `C` | Construct the ship (pays the total) |
| `V` | Save the design as a template |
| `Esc` | Close |

### 5.5 Commander (`I`), ship and cargo (`V`)

The **commander** panel shows your name, skills, reputation and police record.
The **ship** panel shows the ship picture, its specs, the installed equipment
and the **cargo hold**, item by item. Both are read-only and close with `Space`
or `Esc`.

### 5.6 Personnel (`P`), quests (`Q`) and newspaper (`N`)

- **Personnel (`P`)**: hire or fire **mercenaries**; each one has four skills
  and a salary. The crew complements your own skills in the fights (a second
  pilot can dodge, a second engineer can repair) and the salary is paid on every
  departure.
- **Quests (`Q`)**: the open quests, one paragraph each, with their destination
  marked under the text. Use the arrows to pick one and `ENTER` to set its
  system as the map target and close the panel; `Space` also closes it.
- **Newspaper (`N`)**: the newspaper of the day, with the news that point at the
  special events and the systems where something is happening. Scroll with the
  arrows (or `PageUp`/`PageDown`).

### 5.7 Options (`F8`) and menu (`F10`)

The options panel is a list you toggle with `ENTER`; `S` saves the current values
as defaults and `L` loads them back:

| Option | What it does |
| --- | --- |
| Autosave before and after each jump | Saves `autosave_departure.sav` and `autosave_arrival.sav` around every trip |
| Get full fuel tanks on arrival | Refuels to the top on landing (it costs money) |
| Get full hull repairs on arrival | Repairs the hull on landing (it costs money) |
| Always pay for newspaper / Show newspaper on arrival | Newspaper handling |
| Remind about loans | Warns when the debt is not being paid |
| Show range to tracked system | Draws the tracked system's range on the chart |
| Stop tracking on arrival | Clears the track when the trip ends |
| Reserve money for warp costs | Keeps the departure costs untouched when buying |
| Cargo bays to leave empty when buying in-system | Avoids filling the hold before a departure |
| Always ignore pirates / police / traders | Skips those encounters when they have nothing to offer |
| Ignore dealing traders | Skips traders that only want to trade |
| Continuous attack and flight | Keeps attacking/fleeing round after round automatically (`X` interrupts) |
| Continue attacking fleeing ship | Keeps firing at a ship that flees |
| Attempt to disable opponents when possible | Uses the shot to disable instead of destroy |
| Galaxy chart columns per sector | 1, 2 or 3 columns per sector, to fit your font |

The **menu (`F10`)** holds the program actions: high scores (`F3`), options
(`F8`), save (`F5`), load (`F9`), new game (`F2`), about and quit. `Quit` (and
`Esc` on the map) asks for confirmation when a game is loaded, so an accidental
exit does not lose unsaved progress.

## 6. Encounters

During a trip the game rolls for encounters: **pirates**, **police**, **traders**
(including those that trade in orbit), the very rare **Marie Celeste**, the
**famous captains** and **tonic bottles**, plus the quest encounters. When one
happens, the scene takes the whole screen: the two ships facing each other (yours
on the left), the header with the live values of the fight, the log at the bottom
and the available actions with their keys in the last row.

### 6.1 Flying and shooting

| Key | Action |
| --- | --- |
| `SPACE` | **Fire** a round; the opponent answers before you can fire again |
| `↑` / `↓` (or `k` / `j`) | **Dodge** up or down |
| `→` (or `l`) | **Close in**: accepting the offer (trading, the police scan, the captain, boarding a derelict) |
| `←` (or `h`) | **Flee**: turn around and pull away; reaching the edge asks the game for the escape |
| `ENTER` | The **natural action** of the encounter (trade, submit to an inspection, meet the captain, drink, board, surrender...) |
| `X` | **Interrupt** the automatic attack/flee rounds |

The duel is round by round, exactly like the classic game: your **Fighter**
skill against the opponent's **Pilot**, the shields absorb first, the
**Engineer** reduces the damage and the hull can only lose a fraction per shot,
so fights are never decided by a single hit. A **disabled** ship is destroyed by
the next hit; the **Scarab's** organic hull only takes damage from pulse lasers,
and the **Scorpion** cannot be destroyed (disable it to rescue the Princess).

### 6.2 The actions

The scene only offers the actions that make sense in that encounter; the table
below is the complete list and its keys:

| Key | Action | Notes |
| --- | --- | --- |
| `A` | **Attack** | Confirms before attacking a trader or a famous captain; attacking the police ruins your record |
| `F` | **Flee** | At Beginner you escape unharmed; otherwise the opponent gets a free shot and the pilot decides |
| `S` | **Surrender** | Pirates loot you, the police arrest you, the mantis takes the artifact |
| `B` | **Bribe** | The police inspection; the price grows with your worth and some officers cannot be bribed |
| `U` | **Submit** | Let the police inspect you: illegal cargo is confiscated and fined, clean cargo improves your record |
| `Y` | **Yield** | Answer a police surrender demand (an illegal special item means arrest) |
| `O` | **Board** | The Marie Celeste: loot its hold (narcotics bring a police ambush) |
| `P` | **Plunder** | A disabled or surrendering pirate/trader: transfer its cargo to your hold |
| `M` | **Meet** | Trade training for equipment with a famous captain |
| `T` | **Trade** | Deal with a trader in orbit: buy and sell at its prices |
| `D` | **Drink** | The tonic bottle: a good bottle raises skills, an old one spoils one |
| `I` | **Ignore** | Leave the encounter (only when the other ship has nothing to offer) |
| `X` | **Interrupt** | Stop the automatic rounds |

When the scene is waiting for you to read a result (an inspection, a looting, a
trade), a bubble under the rival or a last row tells you to press `ENTER`; `Esc`
also leaves the scene.

### 6.3 Outcomes

- **You win:** the bounty for pirates (when your record is clean), reputation
  and kill counters, and the scoop may offer a canister from their hold — if
  your bays are full you can jettison cargo.
- **You lose:** with an **escape pod** you wake up days later in a nearby port
  (and continue in a Flea after three days and 500 cr.); without it, the game
  ends.
- **You are arrested:** trial, illegal goods impounded, a heavy fine (or the
  ship sold), insurance lost and a second-hand Flea to keep flying.
- **You surrender to pirates:** they loot your hold; hidden compartments hide a
  special item first, and with nothing to steal they blackmail you.

An **ion reactor** on board makes you take more damage; after the Scarab quest,
your **hardened hull** halves it.

## 7. Special events and quests

When the current system has something to offer, the footer shows `[Y] event`.
`Y` opens the offer and applies it if you confirm (some events only need an OK).
The newspaper (`N`) announces where things are happening: the moon for sale, the
shipyards and the mission offers.

### 7.1 Quest lines

| Quest | How it starts | What to do | Reward |
| --- | --- | --- | --- |
| **Alien artifact** | An offer to deliver an artifact | Take it to professor Berger at a hi-tech system; the aliens (mantis) will try to get it back | 20,000 cr |
| **Dragonfly** | Colonel Jackson asks you to hunt a stolen experimental ship | Follow the trail and destroy it | An experimental (lightning) shield for your ship |
| **Experiment** | Dr. Lowenstam's warning | Reach Daled **within ten days** | The **Portable Singularity** (one jump to anywhere, `G`) |
| **Gemulon invasion** | A message to deliver | Reach Gemulon **within six days** | A **fuel compactor** (+3 parsecs of range) |
| **Japori antidote** | A call for help with a disease | Take ten canisters to Japori (10 bays occupied) | Two random skill points |
| **Ambassador Jarek** | The ambassador needs a ride | Take him to Devidia | A **haggling computer** (better prices) |
| **Reactor** | Henry Morgan's dangerous mission | Take the unstable reactor to Nix (15 bays) | **Morgan's laser** |
| **Scarab** | Captain Renwick's stolen ship | Destroy the Scarab at a wormhole exit | A **hull upgrade** |
| **Princess** | The royal kidnapping | Follow the hints to Qonos; **disable** (do not destroy) the Scorpion with disruptors, then take her home | A **quantum disruptor** |
| **Jonathan Wild** | Smuggle a fugitive | Take him to Kravat, dodging the police | Your **record cleaned** and a free mercenary |
| **Sculpture** | A strange delivery | Take it to Endor | **Hidden compartments** |
| **Space monster** | Acamar is under attack | Destroy the monster | 15,000 cr |
| **Moon** | The news announce it | Buy the moon in Utopia (500,000 cr) and go there to claim it | Retirement (see section 8) |

### 7.2 Small events

- Three sealed canisters for 1,000 credits (they can be robots... or water).
- A merchant prince offers a special item for 1,000 credits.
- A hacker cleans your police record for 5,000 credits.
- A fast-learning machine raises one random skill for 3,000 credits.
- The lottery may pay you 1,000 credits when you dock.
- An eccentric billionaire buys your tribbles.
- The very rare encounters: the **Marie Celeste** (loot it before the police
  notice), the famous captains **Ahab**, **Conrad** and **Huie** (training for
  equipment) and the **tonic bottles** (good and old).

## 8. Ending the game

There are three ways to finish:

- **Retire with the moon:** buy it in Utopia (500,000 cr, announced in the news)
  and return there to claim it. You get the game's final screen and your score
  goes to the high score table (`F3`).
- **Die in combat:** if your ship is destroyed and you have no escape pod, the
  game ends there.
- **Not an ending:** being arrested leaves you flying a second-hand Flea, and an
  escape pod leaves you in a nearby port. The game goes on.

The final score combines your **worth** (ship, cash and debt), the difficulty
and the days taken — retiring quickly pays better — and the score table is
stored in `data/HighScores.bin` and can be seen from `F3`.

## 9. Complete key reference

### 9.1 The map and the program

| Key | Action |
| --- | --- |
| Arrows / `hjkl` | Move the chart selection |
| `TAB` | Short-range chart / galactic chart |
| `ENTER` / `T` | Track the selected system (again: stop tracking) |
| `/` | Find a system by name |
| `SPACE` | Warp to the selected system (or through the wormhole of the current one) |
| `G` | Portable Singularity jump |
| `F` / `R` | Buy fuel / repair hull |
| `Y` | Special event of the current system |
| `C` | Trade |
| `B` | Bank |
| `Q` | Quests |
| `N` | Newspaper |
| `P` | Personnel |
| `I` / `V` | Commander / ship and cargo |
| `S` / `E` / `D` | Ships for sale / equipment / ship designer |
| `O` | Buy an escape pod (2,000 cr) |
| `A` | About |
| `F2` / `F5` / `F9` | New game / save / load |
| `F3` / `F8` / `F10` | High scores / options / menu |
| `Esc` | Close the panel; on the map, quit (asks first) |
| `n` / `p` | In the map: newspaper / personnel; in lists: move down/up |

### 9.2 The panels

| Panel | Keys |
| --- | --- |
| Any panel | `Esc` closes it (`Space` in the read-only ones) |
| Lists | `↑`/`↓` (also `n`/`p`, `j`/`k`) |
| Trade (`C`) | `B` buy · `S` sell · `Shift+B`/`Shift+S` the maximum |
| Bank (`B`) | `G` loan · `P` pay back · `I` insurance |
| Quests (`Q`) | `ENTER` set the target · `SPACE` close |
| Personnel (`P`) | `H` hire/fire |
| Ships for sale (`S`) | `B` buy |
| Equipment (`E`) | `B` buy · `S` sell |
| Designer (`D`) | `←`/`→` change · `R` name · `C` construct · `V` save template |
| Options (`F8`) | `ENTER` toggle · `S` save defaults · `L` load defaults |
| Newspaper (`N`) | `↑`/`↓` scroll (also `PageUp`/`PageDown`) |
| Cargo transfer (jettison/plunder) | `1`-`9`/`0` select · `Shift+digit` all |

### 9.3 Encounters

| Key | Action |
| --- | --- |
| `SPACE` | Fire |
| `↑`/`↓` (or `k`/`j`) | Dodge |
| `→` (or `l`) | Close in |
| `←` (or `h`) | Flee |
| `ENTER` | Natural action of the encounter / continue |
| `A` `F` `S` `B` `U` `Y` `O` `P` `M` `T` `D` `I` | Attack, Flee, Surrender, Bribe, Submit, Yield, Board, Plunder, Meet, Trade, Drink, Ignore |
| `X` | Interrupt the automatic rounds |
| `Esc` | Leave the scene when it waits for you |

## 10. Tips and troubleshooting

- **Buy low, sell high:** the trade panel shows the margin with the systems in
  range; a longer trip pays more, but also risks more encounters.
- **Watch the fuel:** the green braille ring on the chart is your real range.
  Running dry away from a port is the classic way to get stuck.
- **Upgrade the hull and shields before the weapons.** A pulse laser is enough
  for a while; a bigger cargo hold pays for everything else.
- **Disable, do not destroy:** an intact disabled ship can be plundered, and the
  Princess needs the Scorpion disabled.
- **The police record matters:** a clean record gives better prices and lets you
  keep the pirate bounties, but traders and police treat you differently.
- **Save before a dangerous trip** (`F5`) or turn on the autosaves (`F8`).

Troubleshooting:

- **“Window too small”:** the terminal is below 60×15. Resize it; 100×30 is
  comfortable and 120×30 fits the encounter scene.
- **The chart looks stretched or squashed:** set *Galaxy chart columns per
  sector* (`F8`) to 1, 2 or 3; the maps assume a cell about twice as tall as
  wide.
- **Boxes or strange glyphs:** use a monospace font with good Unicode coverage
  for the terminal; the game falls back to the text banner when the splash does
  not fit.
- **The game cannot find `ships/`:** launch it with the package launchers (they
  set the working directory), or run the jar from the folder that contains
  `ships/`.
- **Saves:** in `save/` next to the launchers (the autosaves are
  `autosave_departure.sav` and `autosave_arrival.sav`). High scores and defaults
  live in `data/`, and custom designs in `custom/templates`.

## 11. Credits and license

Beyond Space Trader is a Java remake that continues the *SpaceTrader for Java*
port of **Space Trader** (Palm OS, 2002) by **Pieter Spronck**, with artwork by
**Alexander Lawrence**. The Windows port is by **Jay French** with **David
Pierron**; the Java port is by **Aviv Eyal** and contributors. See the `NOTICE`
file for the full provenance chain.

The game is distributed under the **GNU General Public License v3.0 or later**;
see the `LICENSE` file.
