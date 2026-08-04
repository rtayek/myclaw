#!/bin/sh
# start-chrome-cdp-claude.sh - Starts Chrome with remote debugging on port 9222, pointed at claude.ai

PORT="${1:-9222}"
PROFILE_DIR="${HOME}/.chrome-cdp-profile"

if [ -f "/c/Program Files/Google/Chrome/Application/chrome.exe" ]; then
    CHROME_BIN="/c/Program Files/Google/Chrome/Application/chrome.exe"
elif [ -f "/c/Program Files (x86)/Google/Chrome/Application/chrome.exe" ]; then
    CHROME_BIN="/c/Program Files (x86)/Google/Chrome/Application/chrome.exe"
else
    CHROME_BIN="google-chrome"
fi

echo "Starting Chrome with CDP debugging on port $PORT..."
echo "Profile directory: $PROFILE_DIR"

"$CHROME_BIN" --remote-debugging-port="$PORT" --user-data-dir="$PROFILE_DIR" https://claude.ai &
