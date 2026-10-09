#!/usr/bin/env bash
#
# Converts a raw download to a game sample: 16-bit mono 44.1 kHz WAV, silence
# trimmed, edges faded (no clicks) and normalized with headroom.
#
# Usage:
#
#   tools/convert-sounds.sh <raw-file> <key> [variant]
#
# Examples:
#
#   tools/convert-sounds.sh ~/Downloads/explosion.flac combat/explosion 1
#   tools/convert-sounds.sh ~/Downloads/trader.mp3 ambient/trade
#   tools/convert-sounds.sh ~/Downloads/laser.wav combat/laser-pulse 2
#
# The result lands in the resources of the game:
#
#   BeyondSpaceTraderJava/src/main/resources/sounds/<key>-<variant>.wav
#
# (variant 1 by default). The samples travel inside the jar: after converting,
# rebuild the package. The raw download can live anywhere; nothing raw is kept
# in the repository. The keys and the credits of each WAV are documented in
# docs/developer/sounds.md.
set -euo pipefail

if [ $# -lt 2 ] || [ $# -gt 3 ]; then
  echo "usage: $0 <raw-file> <key> [variant]" >&2
  exit 2
fi

if ! command -v ffmpeg >/dev/null 2>&1; then
  echo "ffmpeg is required: install it with your package manager" >&2
  exit 1
fi

raw="$1"
key="$2"
variant="${3:-1}"
out="BeyondSpaceTraderJava/src/main/resources/sounds/${key}-${variant}.wav"

if [ ! -f "$raw" ]; then
  echo "no such file: $raw" >&2
  exit 1
fi

mkdir -p "$(dirname "$out")"

# Trim the silence of both ends (the tail after reversing), fade the edges
# (5 ms in, 15 ms out) so the sample never clicks, normalize with headroom and
# write the canonical format.
ffmpeg -hide_banner -loglevel error -y -i "$raw" \
  -af "silenceremove=start_periods=1:start_threshold=-50dB,\
areverse,silenceremove=start_periods=1:start_threshold=-50dB,areverse,\
afade=t=in:st=0:d=0.005,areverse,afade=t=in:st=0:d=0.015,areverse,\
loudnorm=I=-18:TP=-2:LRA=7" \
  -ac 1 -ar 44100 -c:a pcm_s16le "$out"

echo "wrote $out (16-bit mono 44.1 kHz); rebuild the package to hear it"
