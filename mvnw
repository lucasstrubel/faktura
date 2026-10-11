#!/bin/sh
# ---------------------------------------------------------------------------
# Maven-Bootstrap-Skript (Unix/Git-Bash): laedt Apache Maven beim ersten
# Aufruf nach ~/.m2/wrapper und ruft es mit allen Argumenten auf.
# ---------------------------------------------------------------------------
set -e

MAVEN_VERSION=3.9.9
MAVEN_SHA512=8beac8d11ef208f1e2a8df0682b9448a9a363d2ad13ca74af43705549e72e74c9378823bf689287801cbbfc2f6ea9596201d19ccacfdfb682ee8a2ff4c4418ba
WRAPPER_DIR="$HOME/.m2/wrapper/dists"
MAVEN_HOME="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  echo "Lade Apache Maven $MAVEN_VERSION herunter (einmalig)..."
  mkdir -p "$WRAPPER_DIR"
  ZIP="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION-bin.zip"
  curl -fsSL -o "$ZIP" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"
  if command -v sha512sum >/dev/null 2>&1; then
    ACTUAL_SHA512=$(sha512sum "$ZIP" | cut -d ' ' -f 1)
  else
    ACTUAL_SHA512=$(shasum -a 512 "$ZIP" | cut -d ' ' -f 1)
  fi
  if [ "$ACTUAL_SHA512" != "$MAVEN_SHA512" ]; then
    echo "FEHLER: Maven-Prüfsumme stimmt nicht überein." >&2
    rm -f "$ZIP"
    exit 1
  fi
  unzip -q -o "$ZIP" -d "$WRAPPER_DIR"
  rm -f "$ZIP"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
