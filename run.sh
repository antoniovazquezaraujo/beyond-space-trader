#!/usr/bin/env bash
#
# Builds the shaded jar and runs the game with the Lanterna text UI.
#
#   ./run.sh              -> starts the game
#   ./run.sh --lang es    -> starts it in Spanish (also de, fr...; English by default)
#
set -euo pipefail
cd "$(dirname "$0")"

mvn -q -ntp -pl BeyondSpaceTraderJava -am package -Dmaven.test.skip=true
exec java -cp BeyondSpaceTraderJava/target/beyond-space-trader-0.1.0-SNAPSHOT.jar \
    org.gts.bst.lanterna.LanternaApp "$@"
