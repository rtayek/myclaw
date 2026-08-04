#!/bin/sh
# chatgpt-web-submit.sh - Submits a prompt to ChatGPT web chat via Playwright CDP

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

exec java -jar "$JAR_PATH" web-submit "$@"
