#!/bin/sh

set -eu

./gradlew --stop >/dev/null 2>&1 || true

rm -rf .gradle-build

if [ -e .gradle-build ]; then
    printf '%s\n' 'Could not remove .gradle-build' >&2
    exit 1
fi

printf '%s\n' '.gradle-build removed'
