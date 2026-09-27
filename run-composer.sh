#!/usr/bin/env bash
#
# Builds the project and runs the ship composer: a design tool that reads the
# chassis and the pieces from ships/chassis.txt and ships/pieces.txt, so they can
# be edited with any editor and reloaded with R (no rebuild needed).
#
#   ./run-composer.sh              -> starts the composer
#   ./run-composer.sh --lang es    -> Spanish texts
#
set -euo pipefail
cd "$(dirname "$0")"

mvn -q -ntp -pl BeyondSpaceTraderJava -am package -Dmaven.test.skip=true
exec java -cp BeyondSpaceTraderJava/target/beyond-space-trader-0.1.0-SNAPSHOT.jar \
    org.gts.bst.lanterna.ShipComposer "$@"
