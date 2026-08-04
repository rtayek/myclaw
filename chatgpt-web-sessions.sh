#!/bin/sh
# chatgpt-web-sessions.sh - Lists ChatGPT web chats via Playwright CDP (Chrome running on port 9222)

CDP_URL="${1:-http://127.0.0.1:9222}"
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

exec java -jar "$JAR_PATH" web-sessions "$CDP_URL"
