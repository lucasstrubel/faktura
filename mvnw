#!/bin/sh
# ---------------------------------------------------------------------------
# Maven-Bootstrap-Skript (Unix/Git-Bash): laedt Apache Maven beim ersten
# Aufruf nach ~/.m2/wrapper und ruft es mit allen Argumenten auf.
# ---------------------------------------------------------------------------
set -e

MAVEN_VERSION=3.9.9
WRAPPER_DIR="$HOME/.m2/wrapper/dists"
MAVEN_HOME="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  echo "Lade Apache Maven $MAVEN_VERSION herunter (einmalig)..."
  mkdir -p "$WRAPPER_DIR"
  ZIP="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION-bin.zip"
  curl -fsSL -o "$ZIP" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"
  unzip -q -o "$ZIP" -d "$WRAPPER_DIR"
  rm -f "$ZIP"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
