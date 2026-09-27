#!/usr/bin/env bash
#
# Builds the project and runs the ship art viewer: a design tool to play with the
# generated ships (not the game). Pick a ship with N/P (or Tab), move it with the
# arrows and mirror it with M; ESC quits.
#
#   ./run-ships.sh              -> starts the viewer
#   ./run-ships.sh --lang es    -> Spanish texts (the ship model names stay English)
#
set -euo pipefail
cd "$(dirname "$0")"

mvn -q -ntp -pl BeyondSpaceTraderJava -am package -Dmaven.test.skip=true
exec java -cp BeyondSpaceTraderJava/target/beyond-space-trader-0.1.0-SNAPSHOT.jar \
    org.gts.bst.lanterna.ShipArtViewer "$@"
