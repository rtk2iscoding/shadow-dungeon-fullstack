#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
javac --release 17 -encoding UTF-8 -d build/classes backend/src/*.java
classpath=build/classes
if [ -n "${DB_URL:-}" ]; then
    driver_version=42.7.14
    driver="build/lib/postgresql-$driver_version.jar"
    if [ ! -f "$driver" ]; then
        mkdir -p build/lib
        download=$(mktemp build/lib/pgjdbc.XXXXXX)
        trap 'rm -f "$download"' EXIT
        curl -fsSL --max-time 60 "https://repo.maven.apache.org/maven2/org/postgresql/postgresql/$driver_version/postgresql-$driver_version.jar" -o "$download"
        printf '%s  %s\n' '73914527305a40cce504b0d3d90b23caf565136912d607ae8ae7c5895512332c' "$download" | shasum -a 256 -c -
        mv "$download" "$driver"
    fi
    classpath="$classpath:$driver"
fi
exec java -Djava.awt.headless=true -cp "$classpath" WebServer "$@"
