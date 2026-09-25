#!/usr/bin/env bash
#
# Compiles both Maven modules and runs the game from sources.
#
#   ./run.sh              -> starts the game
#   ./run.sh partida.sav  -> starts the game and loads that save file
#
set -euo pipefail
cd "$(dirname "$0")"

mvn -q -ntp -am -pl BeyondSpaceTraderJava compile
exec java -cp "BeyondSpaceTraderJava/target/classes:JWinForms/target/classes" \
    org.gts.bst.ApplicationST "$@"
