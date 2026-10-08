#!/usr/bin/env bash
#
# Runs the game. It does not build: build it first with
#
#   mvn clean package -DskipTests
#
# and this script runs the package in output/BeyondSpaceTrader (its own Java
# runtime and ship art) or, when only the jar was built, the jar itself.
#
#   ./run.sh              -> starts the game
#   ./run.sh --lang es    -> starts it in Spanish (also de, fr...; English by default)
#
set -euo pipefail
cd "$(dirname "$0")"

PACKAGE="output/BeyondSpaceTrader"
if [ -x "$PACKAGE/bin/beyond-space-trader.sh" ]; then
  exec "$PACKAGE/bin/beyond-space-trader.sh" "$@"
fi

JAR="BeyondSpaceTraderJava/target/beyond-space-trader-0.1.0-SNAPSHOT.jar"
if [ -f "$JAR" ]; then
  echo "output/BeyondSpaceTrader is missing: running the jar with the system Java." >&2
  echo "For the self-contained package: mvn clean package -DskipTests" >&2
  exec java -jar "$JAR" "$@"
fi

echo "Nothing built yet. Build the game first:" >&2
echo "  mvn clean package -DskipTests" >&2
exit 1
