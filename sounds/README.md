# sounds/ — the samples of Beyond Space Trader

The game plays WAV files from this folder (see
[ADR 0007](../docs/developer/adr/0007-audio-por-muestras.md)). **Everything here
is optional**: the game never fails because a file is missing, it just stays
silent for that key. Drop new WAVs in, restart, and they play; no compilation is
needed.

```
sounds/
├── ui/          menu blips            ui/menu-move-1.wav
├── alerts/      dialogs               alerts/alert-1.wav
├── combat/      shots, hits, blasts   combat/laser-pulse-1.wav
├── travel/      warp and escape       travel/warp-1.wav
├── ships/       engine loops          ships/flea-1.wav
├── ambient/     per-screen ambience   ambient/trade-1.wav
├── music/       music loops           music/calm-1.wav
└── raw/         your raw downloads (ignored by git)
```

## Keys

Each key is a file name, and every key can have **variants**: the files ending
in `-<number>.wav` (`combat/hit-1.wav`, `combat/hit-2.wav`...). The game groups
them by prefix and picks one at random every time it plays the key, so repeated
shots and impacts do not sound identical. A key can also be a single file
without the number (`ambient/trade.wav` works too).

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
| `travel/escape` | Getting away from an encounter (reserved for phase B) |
| `ships/<shiptype>` | The engine loop of a ship (`ships/flea`, `ships/gnat`...), a loop |
| `ambient/<screen>` | The ambience of a screen, a loop: `ambient/title`, `ambient/navigation`, `ambient/trade`, `ambient/bank`, `ambient/quests`, `ambient/personnel`, `ambient/commander`, `ambient/ship`, `ambient/shiplist`, `ambient/equipment`, `ambient/options`, `ambient/highscores`, `ambient/designer`, `ambient/news`, `ambient/about`, `ambient/menu` |
| `music/calm`, `music/tense` | The music moods (reserved for phase B), loops |

The ship types are the names of `ShipType` in lowercase (`flea`, `gnat`,
`firefly`, `mosquito`, `bumblebee`, `beetle`, `hornet`, `grasshopper`,
`termite`, `wasp`, `dragonfly`, `mantis`, `scarab`, `scorpion`, ...).

## Format

The engine reads any WAV that `javax.sound` can open and converts it on load to
**16-bit mono 44.1 kHz**; a file that cannot be converted is ignored with a
warning on the console. Stereo or 22 kHz files work, but the canonical format is
the one the mixer is tuned for.

To prepare a download (trim silence, fade the edges so it does not click,
normalize with headroom and convert the format):

```bash
tools/convert-sounds.sh sounds/raw/my-download.flac combat/explosion 1
# -> sounds/combat/explosion-1.wav
```

The script needs `ffmpeg`. Keep the original downloads in `sounds/raw/` (it is
ignored by git) and commit only the converted WAVs.

## Credits and licences

Fill a row for every WAV you add: **audio files are usually not free**, and the
project is GPL v3. Check the licence of each download before committing it (for
example, Freesound files often require attribution).

| Key / file | Author | Source | Licence |
| --- | --- | --- | --- |
| `ui/menu-move-1.wav` | | | |
| `combat/laser-pulse-1.wav` | | | |
| ... | | | |
