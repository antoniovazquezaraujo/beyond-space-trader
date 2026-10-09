#!/usr/bin/env bash
#
# Converts a raw download to a game sample: 16-bit mono 44.1 kHz WAV in the
# resources of the module. By default (one-shot) it trims the silence, fades the
# edges (no clicks) and normalizes with headroom; with --loop it only converts
# the format and normalizes, so the seam of the loop survives.
#
# Usage:
#
#   tools/convert-sounds.sh [--loop] [--seconds N] <raw-file> <key> [variant]
#
# Examples:
#
#   tools/convert-sounds.sh ~/Downloads/explosion.flac combat/explosion 1
#   tools/convert-sounds.sh --loop --seconds 3 ~/Downloads/engine.wav ships/gnat
#   tools/convert-sounds.sh --loop ~/Downloads/calm.ogg music/calm 1
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

usage() {
  cat >&2 <<'EOF'
usage: tools/convert-sounds.sh [--loop] [--seconds N] <raw-file> <key> [variant]

Converts a raw download to the canonical sample of the game
(16-bit mono 44.1 kHz WAV in BeyondSpaceTraderJava/src/main/resources/sounds).

  (no flag)      one-shot: trim the silence, fade the edges, normalize
  --loop         loop (engines, ambience, music): no trimming and no edge
                 fades, just the format and the normalization; check the seam
  --seconds N    with --loop: keep only the first N seconds of the source
  -h, --help     show this help

Examples:
  tools/convert-sounds.sh ~/Downloads/explosion.flac combat/explosion 1
  tools/convert-sounds.sh --loop --seconds 3 ~/Downloads/engine.wav ships/gnat
  tools/convert-sounds.sh --loop ~/Downloads/calm.ogg music/calm 1
EOF
}

loop=0
seconds=""
while [ $# -gt 0 ]; do
  case "$1" in
    --loop)
      loop=1
      shift
      ;;
    --seconds)
      if [ $# -lt 2 ]; then
        echo "missing value for --seconds" >&2
        usage
        exit 2
      fi
      seconds="$2"
      shift 2
      ;;
    --seconds=*)
      seconds="${1#--seconds=}"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    --)
      shift
      break
      ;;
    -*)
      echo "unknown option: $1" >&2
      usage
      exit 2
      ;;
    *)
      break
      ;;
  esac
done

if [ $# -lt 2 ] || [ $# -gt 3 ]; then
  usage
  exit 2
fi

if [ -n "$seconds" ]; then
  case "$seconds" in
    ''|*[!0-9.]*)
      echo "--seconds must be a number of seconds, not '$seconds'" >&2
      exit 2
      ;;
  esac
  if [ "$loop" -eq 0 ]; then
    echo "--seconds only makes sense with --loop" >&2
    exit 2
  fi
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

# The one-shot keeps the trim and the edge fades; the loop keeps its seam intact
# (the same process would cut the tail and click on every repetition). The loop
# normalizes with a linear gain, so the start and the end get the same level.
if [ "$loop" -eq 1 ]; then
  filters="loudnorm=I=-18:TP=-2:LRA=7:linear=true"
else
  filters="silenceremove=start_periods=1:start_threshold=-50dB,\
areverse,silenceremove=start_periods=1:start_threshold=-50dB,areverse,\
afade=t=in:st=0:d=0.005,areverse,afade=t=in:st=0:d=0.015,areverse,\
loudnorm=I=-18:TP=-2:LRA=7"
fi

# --seconds cuts the source before decoding it; without it the whole file goes in.
trim=()
if [ -n "$seconds" ]; then
  trim=(-t "$seconds")
fi

ffmpeg -hide_banner -loglevel error -y "${trim[@]+"${trim[@]}"}" -i "$raw" \
  -af "$filters" \
  -ac 1 -ar 44100 -c:a pcm_s16le "$out"

if [ "$loop" -eq 1 ]; then
  echo "wrote $out (16-bit mono 44.1 kHz loop; check the seam: start and end should match)"
else
  echo "wrote $out (16-bit mono 44.1 kHz); rebuild the package to hear it"
fi
