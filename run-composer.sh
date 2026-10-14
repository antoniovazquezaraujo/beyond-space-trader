#!/usr/bin/env bash
#
# Runs the ship composer: a design tool that reads the chassis and the pieces
# from ships/chassis.txt and ships/pieces.txt, so they can be edited with any
# editor and reloaded with R (the art needs no rebuild). It does not build the
# game: build it first with `mvn clean package -DskipTests`.
#
#   ./run-composer.sh              -> starts the composer
#   ./run-composer.sh --lang es    -> Spanish texts
#
set -euo pipefail
cd "$(dirname "$0")"

PACKAGE="$PWD/output/BeyondSpaceTrader"
JAR="$PACKAGE/lib/app/beyond-space-trader-0.1.0-SNAPSHOT.jar"
JAVA="$PACKAGE/lib/runtime/bin/java"
if [ ! -f "$JAR" ]; then
  JAR="$PWD/BeyondSpaceTraderJava/target/beyond-space-trader-0.1.0-SNAPSHOT.jar"
  JAVA="java"
fi
if [ ! -f "$JAR" ]; then
  echo "Nothing built yet. Build the game first:" >&2
  echo "  mvn clean package -DskipTests" >&2
  exit 1
fi

exec "$JAVA" -cp "$JAR" org.gts.bst.lanterna.ShipComposer "$@"
