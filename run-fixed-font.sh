#!/usr/bin/env bash
#
# Builds the shaded jar and runs the game in a terminal with a fixed monospace font,
# so the charts keep their proportions whatever the desktop's terminal font is.
# It tries kitty, xfce4-terminal and alacritty (the ones that accept a font on the
# command line); otherwise it falls back to the current terminal.
#
#   ./run-fixed-font.sh              -> starts the game in a fixed-font terminal
#
set -euo pipefail
cd "$(dirname "$0")"

mvn -q -ntp -pl BeyondSpaceTraderJava -am package -Dmaven.test.skip=true

JAR="$PWD/BeyondSpaceTraderJava/target/beyond-space-trader-0.1.0-SNAPSHOT.jar"
MAIN="org.gts.bst.lanterna.LanternaApp"
FONT="DejaVu Sans Mono"
SIZE=12

if command -v kitty >/dev/null 2>&1; then
  exec kitty -o "font_family='$FONT'" -o "font_size=$SIZE" java -cp "$JAR" "$MAIN" "$@"
fi

if command -v xfce4-terminal >/dev/null 2>&1; then
  exec xfce4-terminal --font="$FONT $SIZE" --command="java -cp '$JAR' $MAIN"
fi

if command -v alacritty >/dev/null 2>&1; then
  exec alacritty -o "font.normal.family=\"$FONT\"" -o "font.size=$SIZE" -e \
      java -cp "$JAR" "$MAIN" "$@"
fi

echo "No terminal with a command-line font found (kitty, xfce4-terminal, alacritty);" >&2
echo "running in the current terminal. If the map looks off, change the" >&2
echo "\"Galaxy chart columns per sector\" option (F8) to 1 or 3." >&2
exec java -cp "$JAR" "$MAIN" "$@"
