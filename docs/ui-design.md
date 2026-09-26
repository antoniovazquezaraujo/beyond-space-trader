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
- **Menu:** hybrid. Direct shortcuts for the daily actions (chart, track, trade,
  bank, quests, newspaper, jump, fuel and repairs) plus a **dropdown menu** (F10)
  with the system screens and actions (commander, ship, high scores, options,
  save/load, new game and quit). The menu is an overlay that only exists while it is
  open: there is no permanent menu bar.
- **Keys inside the panels:** every screen shows its own keys at the bottom of the
  panel, and the navigation panel shows the actions available in the current system
  (ships, equipment, designer, crew...), so the player never has to look at the other
  side of the screen to choose something.
- **Full screens:** every screen that is not the navigation one (trade, bank, quests,
  ships, equipment, commander, personnel, designer, newspaper...) takes the whole
  width under the header; the chart comes back when it closes.

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
|                                     | [C] Trade  [B] Bank  [Q] Quests    |
|                                     | [N] News  [L] Ships  [E] Equipment |
|                                     | [J] Jump  [F] Fuel  [H] Repairs    |
+-------------------------------------+------------------------------------+
| Last message / news                                                      |
+--------------------------------------------------------------------------+
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
    other ships, with a cursor.
  - *Galactic chart*: known systems, current position, selected destination, routes
    and wormholes, visited marks.
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
| J | Jump (travel) to the selected system |
| F / H | Buy fuel / repair the hull |
| F10 | Dropdown menu (commander, ship, scores, options, save/load/new game, quit) |
| F2 / F5 / F9 | New game / save / load |
| F3 / F8 | High scores / options |
| Digits | Quantities in the trade panels and cargo transfer |

Encounter keys are shown in the encounter panel itself. Every screen lists its own
keys at the bottom of the panel; the navigation panel shows the actions available in
the current system, and the F10 menu holds the system screens (commander, ship, high
scores, options, save/load, new game and quit).

## Screens and states

1. **Main menu:** New game / Load game / Options / High scores / About / Quit, in the
   same one-window shell.
2. **New commander:** name and skill points, showing the starting ship and cash.
3. **Game:** the layout above.
4. **Encounter:** context panel replaced; the map stays visible behind it.
5. **Game over / retirement:** summary and the high score table.

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
