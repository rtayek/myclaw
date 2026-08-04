#!/bin/sh
# claude-web-latest-server.sh - Serves the latest claude.ai web chat as Markdown
# on http://127.0.0.1:<port>/latest-chat, reading the logged-in browser session
# via Playwright CDP (Chrome running on port 9222). Point ChatMap's
# CHATMAP_PROVIDER_URL at that endpoint.
#
# Usage: ./claude-web-latest-server.sh [--port <port>] [cdpUrl]

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

exec java -jar "$JAR_PATH" claude-web-server "$@"
