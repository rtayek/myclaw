#!/bin/sh

set -eu

script_dir=$(
    CDPATH= cd -- "$(dirname -- "$0")" &&
    pwd
)

project_dir=$(
    CDPATH= cd -- "$script_dir/.." &&
    pwd
)

cd "$project_dir"

if [ "$#" -eq 0 ]; then
    exec gradle run --args='claude -'
fi

if [ "$#" -eq 1 ]; then
    case "$1" in
        claude|glm)
            exec gradle run --args="$1 -"
            ;;
        *)
            exec gradle run --args="claude \"$1\""
            ;;
    esac
fi

if [ "$#" -eq 2 ]; then
    case "$1" in
        claude|glm)
            exec gradle run --args="$1 \"$2\""
            ;;
    esac
fi

printf 'Usage: %s [claude|glm] [prompt]\n' "$0" >&2
exit 2
