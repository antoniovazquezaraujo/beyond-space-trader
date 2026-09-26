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

## Layout

```
+------------------------------------------------------------------------+
| Antonio · Day 12 · 12.345 cr · Debt 0                                  |
| Fuel 14/15 · Hull 25/25 · Shields 0/0                                  |
| Cargo 3/10 · Police: Clean · No warnings                               |
+------------------------------------+------------------------------------+
| MAP                                | Acamar · T6 · Democracy            |
| (local / galactic)                 | Pressure: Boredom                  |
|                  TAB               | Resource: Rich Fauna               |
|                                    | Water   30/ 35   buy/sell          |
|                                    | Furs   250/265   buy/sell          |
|                                    |                                    |
|                                    | [C] Trade   [A] Shipyard           |
|                                    | [B] Bank    [P] Personnel          |
|                                    | [Q] Quests  [W] Warp               |
+------------------------------------+------------------------------------+
| Last message / news / combat log            [S/N] confirm              |
+------------------------------------------------------------------------+
```

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
| Arrows / WASD | Move the cursor on the map or in the panel |
| TAB | Local chart / galactic chart |
| ENTER | Confirm / open the selected thing |
| ESC | Close the panel / go back |
| C | Trade panel |
| A | Shipyard panel |
| B | Bank panel |
| P | Personnel panel |
| Q | Quests & news panel |
| W | Warp / travel |
| O | Options |
| F1 | Help |
| Digits | Quantities (1, 10, 100, all) inside the trade panels |

Encounter keys are shown in the encounter panel itself. Every panel lists its own
shortcuts in the bottom bar, so the player never has to remember them.

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

## Open questions

- Quantities in trade: preset steps (1/10/100/all) or free number entry?
