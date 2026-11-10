#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
jar=alpha-desktop-0.15.0.jar
[ -f "$jar" ] || jar=target/alpha-desktop-0.15.0.jar
[ -f "$jar" ] || { echo 'Download and extract the desktop release, or run ./build.sh first.' >&2; exit 1; }
exec java -jar "$jar" "$@"
