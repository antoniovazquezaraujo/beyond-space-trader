# Lanterna UI design (draft)

This document collects the interface redesign that has to happen **before** the
Lanterna port (#9, #10, #11). The Swing/JWinForms front-end is retired; the model
and the presenters stay.

## Decisions already taken

- **One window, panels inside.** No floating windows: the map stays in place and the
  side panel changes with the current activity (trade, shipyard, bank, quests,
  encounter...).
- **Clean terminal.** Simple boxes and borders, a small intentional palette
  (default/green/yellow/red), no bevels, no system dialogs, no system fonts.
- **Keyboard first.** Every action has a visible key; the mouse is optional.
- **Header:** the fields in the sketch below (commander, day, credits, debt, fuel,
  hull, shields, cargo, police record and warnings).
- **Charts:** one at a time in the centre; `TAB` switches between the local and the
  galactic chart.
- **Encounters:** they replace the context panel with their own action keys; the map
  stays visible behind.
- **Options, save/load and confirmations:** panels in the same window, not a
  separate menu screen.
- **Direct menu at the bottom:** the navigation screen shows the direct keys in the
  bottom line(s): the daily ones (chart, trade, bank, quests, newspaper, jump,
  fuel/repairs, commander, ship and menu) plus the actions available in the current
  system (ships, equipment, designer, escape pod, crew). It wraps to a second line
  when it does not fit.
- **The general menu hides when a panel is open:** while an interactive panel is
  shown, the bottom line only belongs to that panel; its keys are drawn inside the
  panel so the general keys cannot be mistaken for the panel ones.
- **The navigation panel is information only:** current system data, dock, target and
  the cargo table; no action keys.
- **F10 menu, program only:** a dropdown overlay (only while it is open) with the
  program actions: high scores, options, save, load, new game and quit. Commander and
  ship status are direct keys (I and V).
- **Keys inside the panels:** every panel shows its own keys at the bottom of the
  panel (wrapped when they do not fit).
- **The map is always visible:** panels use their own width on the right and never
  take the whole screen, so the chart stays visible behind them.

## Layout

```
+--------------------------------------------------------------------------+
| Antonio · Day 12 · 12.345 cr · Debt 0                                    |
| Fuel 14/15 · Hull 25/25 · Shields 0/0 · Cargo 3/10 · Police: Clean       |
+-------------------------------------+------------------------------------+
|                                     | Acamar · T6 · Democracy            |
|              MAP                    | Pressure: Boredom                  |
|        (local / galactic)           | Water  30/ 35   buy/sell           |
|                                     | Furs  250/265   buy/sell           |
|                                     |                                    |
|                                     | (the navigation panel is only      |
|                                     |  information: no keys)             |
+-------------------------------------+------------------------------------+
| Last message (embedded in the separator line)                            |
| TAB map · C trade · B bank · Q quests · N news · J jump · F/H dock ...   |
| ... · I cmdr · V ship · F10 menu · [L] Ships · [E] Equipment ...         |
+--------------------------------------------------------------------------+
```

When a panel is open, its keys live inside the panel and the general menu hides:

```
+-------------------------------------+------------------------------------+
|              MAP                    | Trade                              |
|                                     | item          buy      sell  cargo |
|                                     | > Water      30 cr.   35 cr.   0   |
|                                     |   Furs      250 cr.  265 cr.   3   |
|                                     |                                    |
|                                     | [arrows] select [B] buy [S] sell   |
|                                     | [Shift] all [ESC] close            |
+-------------------------------------+------------------------------------+
| Last message                                                             |
+--------------------------------------------------------------------------+
```

The F10 menu is a small dropdown over the content with the program actions:

```
┌─ Menu ──────────────┐
│ > High scores (F3)  │
│   Options     (F8)  │
│   Save        (F5)  │
│   Load        (F9)  │
│   New game    (F2)  │
│   Quit              │
└─────────────────────┘
```

Any panel (trade, bank, quests, ships, equipment, commander, ship, personnel,
designer, newspaper...) opens on the right with the width it needs, with its own keys
at the bottom of the panel and the map still visible:

```
+-------------------------------------+------------------------------------+
|              MAP                    | Trade                              |
|                                     | item          buy      sell  cargo |
|                                     | > Water      30 cr.   35 cr.   0   |
|                                     |   Furs      250 cr.  265 cr.   3   |
|                                     |                                    |
|                                     | [arrows] select [B] buy [S] sell   |
|                                     | [Shift] all [ESC] close            |
+-------------------------------------+------------------------------------+
```

The F10 menu is a small dropdown over the content with the program actions:

```
┌─ Menu ──────────────┐
│ > High scores (F3)  │
│   Options     (F8)  │
│   Save        (F5)  │
│   Load        (F9)  │
│   New game    (F2)  │
│   Quit              │
└─────────────────────┘
```

A screen that is not the navigation one takes the whole width (its own keys at the
bottom, inside the panel):

```
+--------------------------------------------------------------------------+
| Antonio · Day 12 · 12.345 cr · Debt 0                                    |
+--------------------------------------------------------------------------+
| Trade                                                                    |
| item          buy      sell   cargo  here                               |
| > Water      30 cr.   35 cr.   0     12                                 |
|   Furs      250 cr.  265 cr.   3      4                                 |
|                                                                          |
| [arrows] select  [B] buy  [S] sell  [Shift] all  [ESC] close            |
+--------------------------------------------------------------------------+
| Last message / news                                                      |
+--------------------------------------------------------------------------+
```

The F10 menu is a dropdown over the content (only while it is open):

```
┌─ Menu ──────────────┐
│ > Commander    (I)  │
│   Ship         (V)  │
│   High scores (F3)  │
│   Options      (F8) │
│   Save         (F5) │
│   Load         (F9) │
│   New game     (F2) │
│   Quit              │
└─────────────────────┘
```

When a panel with a list is open (trade, ships, equipment, designer, newspaper...)
and the terminal is narrower than 120 columns, the panel uses the whole width and the
chart is hidden; the chart comes back when the panel closes.

- **Header (always visible):** commander, day, credits, debt, fuel, hull, shields,
  cargo bays, police record and any active warning (low fuel/hull, quest item on
  board, wanted...).
- **Map (center):** the axis of the screen.
  - *Local chart*: the current system, its planet/station, the own ship and the
    other ships, with a cursor. A green braille ring marks the actual jump range
    (the current fuel), so the systems inside it are green too.
  - *Galactic chart*: known systems, current position, selected destination, routes
    and wormholes, visited marks.
  - Both charts draw one character per sector; the local one is centred on the
    current system, and the galactic one keeps its viewport still while the cursor
    moves inside it, scrolling only when the cursor gets close to an edge. Discrete
    zoom levels could be added later.
  - `TAB` switches between both charts.
- **Context panel (right):** one panel at a time, no new windows.
  - *Navigation* (default): current/selected system data, prices at a glance and the
    action keys.
  - *Trade*: buy/sell offers for every item, the ship cargo and the cash.
  - *Shipyard*: repairs, fuel, ships for sale, equipment (sections of the panel).
  - *Bank*: cash, debt, loans, insurance.
  - *Personnel*: crew/mercenaries, skills, hire/dismiss.
  - *Quests & news*: active quests, latest news, newspaper.
  - *Encounter*: replaces the panel (map dimmed); actions offered with keys
    (attack, flee, surrender, bribe, submit, board...).
  - *Options* and *save/load* as panels too.
- **Log/status bar (bottom):** last events, news ticker and confirmation prompts
  (`[S/N]`), plus the key hints of the focused panel.

## Keyboard

| Key | Action |
| --- | --- |
| Arrows | Move the cursor on the map or in the panel |
| TAB | Local chart / galactic chart |
| ENTER / T | Confirm / track the selected system |
| ESC | Close the panel / go back / quit |
| C | Trade panel |
| B | Bank panel |
| Q | Quests panel |
| N | Newspaper panel |
| W | Warp to the selected system (normal trip: spends fuel and a day) |
| J | Jump with the Portable Singularity, only while it is on board |
| F / H | Buy fuel / repair the hull |
| F10 | Dropdown menu (scores, options, save/load, new game, quit) |
| I / V | Commander / ship status |
| F2 / F5 / F9 | New game / save / load |
| F3 / F8 | High scores / options |
| Digits | Quantities in the trade panels and cargo transfer |

Encounter keys are shown in the encounter panel itself. The navigation screen shows
the direct keys (daily plus the actions available in the current system) in the bottom
lines; when a panel is open, the general menu hides and the panel shows its own keys
inside it. The F10 menu holds the program actions (high scores, options, save/load,
new game and quit).

## Screens and states

1. **Main menu:** New game / Load game / Options / High scores / About / Quit, in the
   same one-window shell.
2. **New commander:** name and skill points, showing the starting ship and cash.
3. **Game:** the layout above.
4. **Encounter:** context panel replaced; the map stays visible behind it.
5. **Game over / retirement:** summary and the high score table.

## Colours (ANSI palette)

The text UI uses one ANSI palette with semantic roles, defined in `UiPalette`: white
text, **cyan** titles and accents (day, system, target, panel titles), **yellow** keys
(written as `[C]`) and amounts, **green/red** status values (fuel, hull, shields,
police record, buy/sell prices) and highlighted selected rows (black on cyan). The F10
menu uses the same colours (cyan frame, highlighted entry) and the window chrome
(encounter, dialogs) takes its colours from `LanternaTheme`. The chart keeps its own
system colours.

## Ship sprites

Every standard ship has a small schematic ASCII sprite (up to 12 columns x 5 rows) in
`src/main/resources/spacetrader/ships.txt`, one `[ShipType]` section per ship. The text
UI draws it in the ship panel (`V`), in the selected-ship card of the ship list (`L`)
and in the encounter screen, with the two ships facing each other. The drawings are
monochrome for now (colour roles come later) and the file can be edited freely: the
game loads it at startup, and a ship without art falls back to a generic sprite.

## What happens to the current forms

| Swing form | Where it goes |
| --- | --- |
| `FormViewShip`, `FormViewCommander`, `FormViewPersonnel` | Context panels (ship/commander/personnel data) |
| `FormViewBank`, `FormGetLoan`, `FormPayBackLoan`, `FormCosts` | Bank panel |
| `FormViewQuests`, newspaper | Quests & news panel |
| `FormShipyard`, `FormShipList`, `FormEquipment`, `FormBuyFuel`, `FormBuyRepairs` | Shipyard panel |
| `FormCargoBuy`, `FormCargoSell`, `FormJettison`, `FormPlunder` | Trade panel / plunder actions |
| `FormEncounter`, `FormMonster` | Encounter panel |
| `FormFind`, `FormTest` | `F` find / debug console (dev builds) |
| `FormOptions`, `FormNewCommander`, `FormAbout`, high scores | Menu screens/panels |
| `FormAlert` | Log bar + confirmation row (no floating dialog) |

The presenters already written for these screens (`MainPresenter`, `BankPresenter`,
`ShipPresenter`, `CommanderPresenter`, `PersonnelPresenter`, `QuestsPresenter`,
`ShipListPresenter`, `EquipmentPresenter`, `ShipyardPresenter`,
`CargoTransferPresenter`, `EncounterPresenter`, `HighScoresPresenter`) are reused by
the new views.

## Implementation order (after this design)

1. **#9 chart renderer:** a widget that draws the local/galactic chart with a cursor,
   tested against a fake surface.
2. **#10 window shell:** header, panel, log bar, key routing and theme; dialogs become
   panels.
3. **#11 panels:** one by one, retiring the Swing forms and JWinForms at the end.

## Quantities in trade

The player types the number (with backspace) and `ENTER` confirms; there are also
quick keys (`1`, `10`, `100` and `A` for everything). No dialogs and no separate
input mode.
