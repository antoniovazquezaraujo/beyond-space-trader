#!/usr/bin/env bash
#
# Builds the shaded jar (Swing + Lanterna) and runs the game with the Lanterna
# text UI.
#
set -euo pipefail
cd "$(dirname "$0")"

mvn -q -ntp -am -pl BeyondSpaceTraderJava package -Dmaven.test.skip=true
exec java -cp BeyondSpaceTraderJava/target/beyond-space-trader-0.1.0-SNAPSHOT.jar \
    org.gts.bst.lanterna.LanternaApp "$@"
