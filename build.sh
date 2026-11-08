#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
command -v mvn >/dev/null 2>&1 || { echo 'Install JDK 17+ and Maven 3.8+ first.' >&2; exit 1; }
mvn clean package
