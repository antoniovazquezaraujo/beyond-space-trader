#!/usr/bin/env bash
#
# Compiles both Maven modules and runs the game with the Lanterna text UI.
#
set -euo pipefail
cd "$(dirname "$0")"

mvn -q -ntp -am -pl BeyondSpaceTraderJava compile
exec java -cp "BeyondSpaceTraderJava/target/classes:JWinForms/target/classes" \
    org.gts.bst.lanterna.LanternaApp "$@"
