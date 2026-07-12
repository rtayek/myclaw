#!/bin/sh

set -eu

script_dir=$(
    CDPATH= cd -- "$(dirname -- "$0")" &&
    pwd
)

jar="$script_dir/.gradle-build/libs/myclaw.jar"

if [ ! -f "$jar" ]; then
    printf 'myclaw: jar not found: %s\n' "$jar" >&2
    printf 'Run: gradle build\n' >&2
    exit 1
fi

exec java -jar "$jar" "$@"