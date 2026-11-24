#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
javac --release 17 -encoding UTF-8 -d build/classes backend/src/*.java
exec java -Djava.awt.headless=true -cp build/classes WebServer "$@"
