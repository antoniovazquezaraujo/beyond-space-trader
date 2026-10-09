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

## Keys

| Key | When it plays |
| --- | --- |
| `ui/menu-move` | Moving the cursor in a menu or a list |
| `ui/menu-select` | Accepting an entry (the F10 menu, a toggle) |
| `alerts/alert` | A dialog with a single button |
| `alerts/warning` | A dialog that asks a question (two buttons) |
| `combat/laser-pulse`, `-beam`, `-military`, `-morgan`, `-photon`, `-quantum` | Each weapon type firing |
| `combat/hit` | A shot landing on a ship |
| `combat/explosion` | A ship being destroyed |
| `travel/warp` | Warping to another system |
| `travel/escape` | Getting away from an encounter |
| `ships/<shiptype>` | The engine loop of a ship (`ships/flea`, `ships/gnat`...), a loop |
| `ambient/<screen>` | The ambience of a screen, a loop: `ambient/title`, `ambient/navigation`, `ambient/trade`, `ambient/bank`, `ambient/quests`, `ambient/personnel`, `ambient/commander`, `ambient/ship`, `ambient/shiplist`, `ambient/equipment`, `ambient/options`, `ambient/highscores`, `ambient/designer`, `ambient/news`, `ambient/about`, `ambient/menu` |
| `music/calm`, `music/tense` | The music mood of the encounter, a loop |

The ship types are the names of `ShipType` in lowercase (`flea`, `gnat`,
`firefly`, `mosquito`, `bumblebee`, `beetle`, `hornet`, `grasshopper`,
`termite`, `wasp`, `dragonfly`, `mantis`, `scarab`, `scorpion`, ...).

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

## Adding a sound

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
it the whole file goes in). The loop keys are `ships/<shiptype>` (engines),
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
