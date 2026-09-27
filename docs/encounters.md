# Encounters (reference for the redesign)

Everything that can happen in an encounter, scene by scene, so it can be drawn and
redesigned without breaking the rules. The texts are in `Strings.properties` (and the
translations next to it); the rules are in `spacetrader/Game.java`, the action sets in
`org/gts/bst/presenter/EncounterPresenter.java` and the screen in
`org/gts/bst/lanterna/LanternaEncounterView.java`.

## When an encounter happens

A trip (`Game.Travel()`) lasts **20 clicks** (`Consts.StartClicks`) and every click the
game looks for an encounter, so a trip can have several (or none):

- **Random**: `GetRandom(44 - 2 * difficulty)` (44 on Beginner ... 36 on Impossible) is
  compared against the destination's **pirate / police / trader activity** (0-7, from
  the political system). Police odds are multiplied by a factor that grows with your
  criminal record; a Flea halves the odds; pirates only once per trip (you are
  "raided"); Wild on board on the way to Kravat brings swarms of police; with the
  artifact on board 15 % of the random encounters are alien *mantis*; on Gemulon too
  late, half of them are mantis.
- **Very rare**, one-shot each and only after day 10, 0.5 % per click
  (`_chanceOfVeryRareEncounter`): the abandoned **Marie Celeste** (to loot), **Captain
  Ahab / Conrad / Huie** (they trade skill points for equipment), and a **good** or an
  **old bottle** of tonic.
- **Trade in orbit**: 10 % of the trader encounters (`_chanceOfTradeInOrbit`).
- **Scripted** (quest): the **space monster** at Acamar, the stolen **Scarab** when you
  arrive through a wormhole, the **Dragonfly** at Zalkon, the kidnappers' **Scorpion**
  at Qonos, and the police ambush right after you loot the Marie Celeste.
- Encounters where the opponent is cloaked (or the `Always ignore ...` options are on
  and they just ignore/flee) never take place.

The first line of the scene says where it happens: *"At ^1 from ^2 you encounter ^3
^4."* — `^1` is the clicks left to the destination.

## The scene screen (today)

A centred modal window (78x20) with:

```
Gnat                                  Pirate ship
    __                                    __
   /  \_                                 /  \_
  | o o >                               | o  o \
Hull 100/100   Shields 1/1            Hull 20/20   Shields 1/1

At 2 clicks from Korma you encounter a pirate gnat.

The pirate ship attacks.

[A]ttack  [F]lee  [S]urrender
```

- The two ships **face each other** (sprites from `ships.txt`), with their names and
  hull/shield values.
- `encounterText` is the **last round** ("You hit the X." / "The X hits you."), the
  action text says what is happening now ("The X attacks/flees/is disabled/wishes to
  surrender").
- The keys offered depend on the encounter (the table below).
- With the `Continuous attack` / `Continue attacking a fleeing ship` options on, a
  **1-second timer** (`LanternaEncounterView.startTimer` → `EncounterPresenter.tick`)
  keeps firing rounds until the fight ends; `Interrupt` (X) stops it.

## The opponents

| Opponent | Ship | Notes |
| --- | --- | --- |
| Pirate | random, scaled with your worth and difficulty | can attack, flee or surrender; the alien *Mantis* uses the same path |
| Police | random, scaled | can attack, flee, inspect or demand your surrender |
| Trader | random, scaled | can offer to trade, flee, or be attacked and plundered |
| Famous captain | Gargantuan, reflective shields, military lasers, gadgets | only shows up if you carry the gear they want |
| Space monster / Dragonfly / Scarab / Scorpion | fixed | quest ships with special rules |
| Marie Celeste | empty trader | to board and loot |
| Bottle | a bottle | the tonic |

## Actions offered per encounter

| Encounter | Actions (key) |
| --- | --- |
| Pirate / police / Scarab attack and police surrender demand | Attack (A), Flee (F), Surrender (S) |
| Police inspection | Attack, Flee, Submit (U), Bribe (B) |
| Police after the Marie Celeste | Attack, Flee, Yield (Y), Bribe |
| Trader attack, flee or ignore | Attack, Ignore (I) |
| Trader trade in orbit | Attack, Ignore, Trade (T) |
| Marie Celeste | Board (O), Ignore |
| Disabled or surrendering pirate / trader | Attack, Plunder (P) |
| Cloaked or ignoring opponent | Attack, Ignore |
| Famous captain | Attack, Ignore, Meet (M) |
| Tonic bottle | Drink (D), Ignore |
| Automatic rounds running | Interrupt (X), always |

## What each action does

- **Attack (A)**: checks first (`EncounterVerifyAttack`): no weapons, only disruptors
  against a non-disableable ship, or no disruptors against the *Scorpion* → alert and
  nothing happens. Attacking the police (always if your record is criminal or worse)
  drops your record to *Criminal* and costs `ScoreAttackPolice`; attacking a trader or
  a famous captain asks for confirmation and costs `ScoreAttackTrader`
  (`ScoreAttackCaptain` for a captain, who also makes the news); attacking someone who
  was ignoring you starts the fight. Then a **round** is fired.
- **Flee (F)**: at Beginner difficulty you escape unharmed; otherwise the opponent gets
  a free shot and the escape is resolved with pilot vs pilot. Fleeing from an
  inspection or from the Marie Celeste police turns them hostile and hurts your record
  (`ScoreFleePolice` / `ScoreAttackPolice`); with the Marie Celeste you confirm first
  (`EncounterPostMarieFlee`).
- **Ignore (I)**: leaves the encounter (only offered when the opponent ignores or flees
  from you, or when trading in orbit).
- **Surrender (S)**: to the *mantis* with the artifact on board → give the artifact up
  (quest lost); to the police (unless you are a psychopath, who gets no mercy) → accept
  prison: you are **arrested**; to pirates → they loot you (see Loot below).
- **Submit (U)** (police inspection): with illegal cargo/passengers they confiscate it,
  fine you and add `ScoreTrafficking`; carrying nothing illegal improves your record.
- **Yield (Y)** (police demanding surrender): like Submit, but if you carry an illegal
  special item you are arrested instead.
- **Bribe (B)**: the price grows with your worth, drops with the difficulty and depends
  on the system's `BribeLevel` (0 = they cannot be bribed; the Marie Celeste police
  never take it; no cash → alert). Paying ends the encounter.
- **Board (O)** (Marie Celeste): opens the cargo transfer to loot it; if you take
  narcotics, the police will ambush you on the next click.
- **Plunder (P)** (disabled or surrendering pirate/trader): the cargo transfer; costs
  `ScorePlunderPirate` / `ScorePlunderTrader`.
- **Meet (M)** (famous captain): trades their skill training for your gear — Ahab:
  reflective shield → piloting; Conrad: military laser → engineering; Huie: military
  laser → trading (only if the skill is under 10; +2 points on Beginner/Normal).
- **Trade (T)** (trader in orbit): opens the trader's buy/sell offers (cargo transfer
  screens with the trader's prices).
- **Drink (D)** (tonic): a good bottle raises one (or two on Beginner/Normal) random
  skills; an old bottle gives a random skill a bad tweak.
- **Interrupt (X)**: stops the automatic attack/flee rounds.

## The combat model (one round)

`Game.EncounterExecuteAttack` resolves one shot: the attacker's *Fighter* skill against
the defender's *Pilot* (a fleeing defender is harder to hit and gets no shot back).

- **Weapons**: lasers (pulse, beam, military, Morgan) damage the **hull**; disruptors
  (photon, quantum) **disable** and only scratch the hull. With the
  *Attempt to disable opponents* option, a shot with the shields down can disable the
  opponent instead of damaging it.
- **Shields absorb first**; the defender's *Engineer* reduces the damage; the hull can
  only lose a fraction per shot (at least 2 shots on Normal, 3 on Easy, 4 on Beginner,
  1 on Hard/Impossible), so fights are never one-shot.
- A **disabled** ship is destroyed by the next hit. The **Scarab's** organic hull only
  takes damage from pulse lasers (and photon disruptors). The **Scorpion** is never
  destroyed: at 0 hull it stays at 1 and is disabled (that is how the Princess is
  rescued). An **ion reactor on board** boosts the damage you take. After the Scarab
  quest, your **hardened hull** halves the damage.
- The opponent's attitude changes with the damage (`EncounterUpdateEncounterType`):
  pirates flee/surrender (and can be disabled), police flee, traders flee or surrender;
  the chance depends on their hull and yours.

## Outcomes

`EncounterResult`: `Continue`, `Normal`, `Killed`, `EscapePod`, `Arrested`.

- **You win**: destroy or disable the opponent → alerts (`EncounterYouWin`,
  `EncounterDisabledOpponent`, or a **bounty** for pirates when your record is not
  criminal), reputation and kill counters, and the news change for famous captains.
  Then the **scoop** may offer a canister from their hold (chance by difficulty; if
  your bays are full you can jettison).
- **You lose**: with an escape pod you wake up days later at a nearby port (and start
  again with a Flea after 3 days and 500 cr.); without it, the game ends (or both ships
  are destroyed).
- **Arrested**: trial and alerts: illegal goods impounded, hefty fine (or the ship is
  sold), hidden compartments found and removed, insurance lost, mercenaries leave, and
  a second-hand Flea to keep flying.
- **Pirates loot you** (surrender): hidden cargo bays hide the Princess/sculpture
  first; otherwise the sculpture is lost, cargo is taken by price order, and with
  nothing to steal they blackmail you (debt). Wild may hop onto the pirate ship (or
  chat), and they examine an ion reactor on board.

## Scripted encounters (quests)

| Trigger | Scene | Effects |
| --- | --- | --- |
| Acamar, quest active | Space monster attacks | killing it ends the quest |
| Arrival through a wormhole, quest active | Stolen **Scarab** | destroying it enables the hull upgrade |
| Zalkon, quest active | **Dragonfly** | destroying it enables the lightning shield |
| Qonos, quest active | Kidnappers' **Scorpion** | disable it (not destroy) to rescue the Princess |
| Just looted the Marie Celeste with narcotics | Police ambush | attack, flee (record hit), yield or bribe |
| Kravat with Wild on board | Swarms of police | smuggle him home (or he leaves the ship) |
| Artifact on board | Alien **mantis** | surrender it or fight; it also blocks the quest |
| Reactor quest active | Reactor warnings and meltdown on arrival | mission timer, fuel burn, warning alerts |

## Arrival events (they look like encounters but happen after landing)

Reactor warnings/meltdown, tribbles eating the cargo (or dying from the reactor,
or being bought), debt reminders, repairs, prices and quantities update, the Easter
egg at Og (a full cargo of one unit each gives a Lightning Shield), the newspaper and
the moon/retirement.

## Notes for the redesign

- The view model already has the two ship types (→ sprites), the hull/shield texts and
  the round text; to animate the round it would help to add the **flags** (you hit,
  they hit, damage taken) instead of only the formatted sentences.
- There is already a 1-second tick for the automatic rounds (`EncounterView.startTimer`
  → `EncounterPresenter.tick`), and the encounter is a modal Lanterna window: a short
  animation (3-4 frames of ~80-120 ms, driven from the view with
  `gui.getGUIThread().invokeLater`) fits without touching the rules.
- The sprites are editable ASCII art (`ships.txt`) and the colour roles are pending
  (#82). The encounter scene is the natural place to show them off.
- Scene ideas: shots drawn as beams or flashes between the ships, shields flashing when
  they absorb, hull bars, ships approaching for trade/docking and boarding, drifting
  apart while fleeing, an explosion frame when one is destroyed, and a short combat log
  of the last rounds.
- Known gap: the reaction sentences ("You hit the X.", "The X is fleeing."...) are
  hardcoded in `Game.java` and are not translated yet; they should become bundle keys
  with the redesign.
