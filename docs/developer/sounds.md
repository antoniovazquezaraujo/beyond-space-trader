# Sounds: the samples inside the jar

The game plays WAV samples that travel **inside the jar**, as classpath
resources (see [ADR 0007](adr/0007-audio-por-muestras.md)). The engine reads
them at startup, converts each one to **16-bit mono 44.1 kHz** in memory and
mixes them; **everything is optional**, so the game never fails because a sound
is missing, it just stays silent for that key.

## Where the files live

Author them in the resources of the module:

```
BeyondSpaceTraderJava/src/main/resources/sounds/
├── ui/          menu blips            ui/menu-move-1.wav
├── alerts/      dialogs               alerts/alert-1.wav
├── combat/      shots, hits, blasts   combat/laser-pulse-1.wav
├── repair/      hull repairs         repair/hammer-1.wav
├── travel/      warp and escape       travel/warp-1.wav
├── ships/       engine loops          ships/flea-1.wav
├── ambient/     per-screen ambience   ambient/trade-1.wav
└── music/       music loops           music/calm-1.wav
```

Each key is a file name, and every key can have **variants**: the files ending
in `-<number>.wav` (`combat/hit-1.wav`, `combat/hit-2.wav`...). The loader probes
`-1`, `-2` and so on up to `-9` (it cannot list folders inside a jar), stops at
the first number that is missing and picks one of the loaded variants **at
random on every playback**, so repeated shots and impacts do not sound
identical. The numbered variants must have no gaps, and the plain
`<key>.wav` without a number is **not** loaded.

## The files, one by one

The practical map: what to record and where. Every example ends in `-1.wav`;
the numbered variants are explained above.

### One-shot effects (not loops)

| File (inside `sounds/`) | What it is |
| --- | --- |
| `ui/menu-move-1.wav` | The cursor moves anywhere: menus, lists, dialogs, editors |
| `ui/menu-select-1.wav` | Anything accepted: a menu entry, a toggle, a button, an amount, a cargo move |
| `ui/chart-move-1.wav` | The chart cursor moves to another system |
| `ui/chart-select-1.wav` | The chart accepts something: tracking a system, a find, a special event |
| `alerts/alert-1.wav` | A dialog with a single button |
| `alerts/warning-1.wav` | A dialog that asks a question (two buttons) |
| `alerts/police-1.wav` | The police shows up in an encounter (once per encounter) |
| `alerts/police-scan-1.wav` | The police scanner sweeps your ship |
| `combat/laser-pulse-1.wav` | A Pulse laser firing |
| `combat/laser-beam-1.wav` | A Beam laser firing |
| `combat/laser-military-1.wav` | A Military laser firing |
| `combat/laser-morgan-1.wav` | A Morgan's laser firing |
| `combat/laser-photon-1.wav` | A Photon disruptor firing |
| `combat/laser-quantum-1.wav` | A Quantum disruptor firing |
| `combat/hit-1.wav` | A shot landing on a ship (variants `-2`, `-3`... welcome) |
| `combat/explosion-1.wav` | A ship being destroyed (variants welcome) |
| `repair/hammer-1.wav` | A hammer blow on the hull while the repair scene plays (variants welcome) |
| `repair/done-1.wav` | The hull repairs are done (end of the repair scene) |
| `travel/warp-1.wav` | Warping away: it sounds as the ship sets off |
| `travel/escape-1.wav` | Getting away from an encounter |

### Loops

| File (inside `sounds/`) | What it is |
| --- | --- |
| `ships/<shiptype>-1.wav` | The engine of a ship (see the engines below) |
| `alerts/attack-1.wav` | The rival fires: the alarm loops while the fight lasts |
| `alerts/danger-1.wav` | The first hit on your ship: loops over the attack one |
| `ambient/title-1.wav` | The ambience of the title screen |
| `ambient/navigation-1.wav` | The ambience of the star map (navigation) |
| `ambient/trade-1.wav` | The ambience of the trade exchange (market) |
| `ambient/bank-1.wav` | The ambience of the bank |
| `ambient/quests-1.wav` | The ambience of the quests screen |
| `ambient/personnel-1.wav` | The ambience of the personnel screen (hiring) |
| `ambient/commander-1.wav` | The ambience of the commander profile |
| `ambient/ship-1.wav` | The ambience of your ship screen |
| `ambient/shiplist-1.wav` | The ambience of the ship list |
| `ambient/equipment-1.wav` | The ambience of the equipment screen |
| `ambient/options-1.wav` | The ambience of the options screen |
| `ambient/highscores-1.wav` | The ambience of the high scores |
| `ambient/designer-1.wav` | The ambience of the ship designer |
| `ambient/news-1.wav` | The ambience of the newspaper |
| `ambient/about-1.wav` | The ambience of the about/credits screen |
| `ambient/menu-1.wav` | The ambience of the menu |
| `music/calm-1.wav` | Calm music (traders and the rare encounters) |
| `music/tense-1.wav` | Tense music (pirates, police, the mission hunters) |

### Engines: the types and their sizes

A ship with no loop of its own borrows the loop of its size and then the
shared default, so four or five size loops are enough to start.

| Size file | Ships of that size |
| --- | --- |
| `ships/tiny-1.wav` | flea |
| `ships/small-1.wav` | gnat, firefly, mosquito, bottle, dragonfly |
| `ships/medium-1.wav` | bumblebee, beetle, mantis |
| `ships/large-1.wav` | hornet, grasshopper, scarab |
| `ships/huge-1.wav` | termite, wasp, spacemonster, custom, scorpion |
| `ships/gargantuan-1.wav` | none yet (the size exists, no ship uses it) |
| `ships/default-1.wav` | Any type without a loop of its own |

The per-type files, if you want to polish them later: `ships/flea-1.wav`,
`ships/gnat-1.wav`, `ships/firefly-1.wav`, `ships/mosquito-1.wav`,
`ships/bumblebee-1.wav`, `ships/beetle-1.wav`, `ships/hornet-1.wav`,
`ships/grasshopper-1.wav`, `ships/termite-1.wav`, `ships/wasp-1.wav`,
`ships/spacemonster-1.wav`, `ships/dragonfly-1.wav`, `ships/mantis-1.wav`,
`ships/scarab-1.wav`, `ships/bottle-1.wav`, `ships/custom-1.wav` and
`ships/scorpion-1.wav`.

## Engines, shots and music (phase B)

The encounter scene drives the loops with the life of the ships: the engine is
**constant** while the ship is in the scene (it does not follow the manoeuvres).

| Moment | What sounds |
| --- | --- |
| The ship enters the scene | Its engine loop starts with a ~0.3 s fade in |
| The ship is stopped, surrendered or disabled (but present) | The engine goes on: the ship is still there |
| The ship is destroyed | Its engine fades out in ~0.15 s, mixed with `combat/explosion` |
| The ship marches out (flees, escapes, crosses and leaves) | Its engine fades out in ~0.4 s while it goes |
| The scene closes | Both engines fade fast (~0.12 s) and the music goes away |

The engine sample falls back from the ship type to its size and then to the
shared default: `ships/<shiptype>` → `ships/tiny`, `ships/small`,
`ships/medium`, `ships/large`, `ships/huge` or `ships/gargantuan` (the sizes of
`Consts.ShipSpecs`) → `ships/default` → silence.

A shot plays the **strongest weapon on board** of the ship that fires (Morgan's >
Military > Beam > Pulse > Quantum > Photon): `combat/laser-morgan`,
`combat/laser-military`, `combat/laser-beam`, `combat/laser-pulse`,
`combat/laser-quantum` or `combat/laser-photon`.

The music follows the tension, re-evaluated on every part of the encounter:

| Mood | Encounters | Key |
| --- | --- | --- |
| Tense | Pirates, police and the mission hunters (Dragonfly, Scarab, Scorpion, space monster, famous captains attacking); a trader that turns violent | `music/tense` |
| Calm | Traders (buy, sell, ignore, flee) and the rare encounters (captains, bottles, Marie Celeste, the ignoring mission ships) | `music/calm` |

When the situation changes (the trader attacks, the police shows up) the music
crossfades to the mood that plays; when the scene closes it fades to nothing.
Getting away from an encounter plays `travel/escape` (the map warp keeps
`travel/warp`).

### Alarms (conditional loops)

Three alerts react to the encounter, all optional like the rest:

| Key | When | How |
| --- | --- | --- |
| `alerts/police` | The police shows up | One-shot, once per encounter (also when it turns police halfway, as in the Marie Celeste) |
| `alerts/attack` | The rival fires for the first time | Loop until the fight ends |
| `alerts/danger` | Your ship takes its first hit (shield or hull) | Loop, **over** the attack one |

The fight ends when the enemy is destroyed or disabled, when it flees or
crosses away, when the result screen waits for the player (a surrender, a
loot), when you get away, or when the scene closes: the alarm loops fade out
with a short crossfade. A key without a sample is silence, and the two loops
share their own channel in the mixer (`ALERTS`, 0.6 by default).

## Adding a sound

The two menu keys sound across the whole UI: the F10 menu, every list (trade,
bank, ships, equipment, personnel, quests, options), the dialogs and their
buttons, the quantity dialogs, the cargo transfer and the ship editors. The
chart (the map) has its own pair, `ui/chart-move`/`ui/chart-select`, so moving
the cursor and setting a target do not sound like a list. Nothing sounds when a
key changes nothing (an arrow against an edge, a cancel with Escape).

The engine reads any WAV that `javax.sound` can open and converts it on load to
the canonical format; a file that cannot be converted is ignored with a warning
on the console. Stereo or 22 kHz files work, but the canonical format is the one
the mixer is tuned for.

To prepare a download (trim silence, fade the edges so it does not click,
normalize with headroom and convert the format):

```bash
tools/convert-sounds.sh ~/Downloads/explosion.flac combat/explosion 1
# -> BeyondSpaceTraderJava/src/main/resources/sounds/combat/explosion-1.wav
```

The script needs `ffmpeg` and leaves the raw download where it is: nothing raw
is kept in the repository. After converting, rebuild the package
(`mvn clean package -DskipTests`) so the jar carries the new WAV.

## Loops (engines, ambience and music)

The **loop** keys need a different treatment: the one-shot conversion trims the
silence and fades the edges, which would cut the tail of the loop and click on
every repetition. Use `--loop`, which only converts the format and normalizes
with a linear gain (the start and the end keep the same level):

```bash
tools/convert-sounds.sh --loop --seconds 3 ~/Downloads/engine.flac ships/gnat
tools/convert-sounds.sh --loop ~/Downloads/trade-ambience.ogg ambient/trade 1
tools/convert-sounds.sh --loop ~/Downloads/calm.ogg music/calm 1
```

`--seconds N` keeps only the first `N` seconds of the source (optional; without
it the whole file goes in). The cut lands at an arbitrary point, so it can
break the seam even with no fades: use it to shorten a longer source and always
check the seam afterwards. The loop keys are `ships/<shiptype>` (engines),
`ambient/<screen>` and `music/calm`, `music/tense`.

- **Engines**: aim for **2 to 4 seconds**. Short enough to keep the jar small,
  long enough that the repetition does not feel like a stutter. Look for
  sources marked *loop* or *seamless* (an engine hum recorded in a loop).
- **Ambience and music**: they can be longer (tens of seconds); the same rules
  apply: no fades and a seam that matches.
- **Check the seam by ear**: play the converted WAV in a loop (`ffplay -loop 0
  file.wav`, or any player with repeat) and listen for a click or a gap where
  the end meets the start. If it clicks, try a better source or trim it at a
  zero crossing with a wave editor; the converter cannot fix a bad seam.

The engine sample falls back in this order, so a missing type borrows from its
size and then from the shared default (silence when none exists):

| Order | Key |
| --- | --- |
| 1 | `ships/<shiptype>` (`ships/gnat`, `ships/flea`, ...) |
| 2 | `ships/tiny`, `ships/small`, `ships/medium`, `ships/large`, `ships/huge`, `ships/gargantuan` (the size of `Consts.ShipSpecs`) |
| 3 | `ships/default` |
| 4 | silence |

## Credits and licences

Fill a row for every WAV you add: **audio files are usually not free**, and the
project is GPL v3. Check the licence of each download before committing it (for
example, Freesound files often require attribution).

| Key / file | Author | Source | Licence |
| --- | --- | --- | --- |
| `ui/menu-move-1.wav` | | | |
| `combat/laser-pulse-1.wav` | | | |
| ... | | | |
