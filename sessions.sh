#!/bin/sh
# sessions.sh - Runs myclaw.jar to list sessions for a specified LLM backend

BACKEND="${1:-claude}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ -f "$SCRIPT_DIR/.gradle-build/libs/myclaw.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/.gradle-build/libs/myclaw.jar"
elif [ -f "$SCRIPT_DIR/build/libs/myclaw.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/build/libs/myclaw.jar"
elif [ -f "$SCRIPT_DIR/myclaw.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/myclaw.jar"
else
    echo "myclaw.jar not found. Building jar..."
    "$SCRIPT_DIR/gradlew" jar || exit 1
    JAR_PATH="$SCRIPT_DIR/.gradle-build/libs/myclaw.jar"
fi

exec java -jar "$JAR_PATH" sessions "$BACKEND"
