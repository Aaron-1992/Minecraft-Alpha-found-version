#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p .test-classes
javac --release 17 -d .test-classes src/main/java/*.java tests/java/*.java
java -Djava.awt.headless=true -cp .test-classes DesktopTests
java -Djava.awt.headless=true -cp .test-classes OriginalEngineTests
