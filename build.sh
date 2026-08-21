#!/bin/sh
# Offline, dependency-free build: javac only, no Maven/Gradle, no external JAR.
# Compiles Production Java source (src/main/java) into build/classes/.
# Source tree itself is never written to (no .class files under src/).
set -eu

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
SRC_ROOT="$PROJECT_ROOT/src/main/java"
BUILD_DIR="$PROJECT_ROOT/build"
CLASSES_DIR="$BUILD_DIR/classes"
SOURCES_LIST="$BUILD_DIR/sources.txt"

echo "== XPlatform to WebSquare Converter - Offline Build =="
echo "Project root: $PROJECT_ROOT"

if ! command -v java >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1; then
  echo "[FAIL] java/javac not found on PATH. Set JAVA_HOME to your JDK 1.8.0_111 install and add its bin/ to PATH."
  exit 1
fi

JAVA_VER="$(java -version 2>&1 | head -n 1)"
JAVAC_VER="$(javac -version 2>&1 | head -n 1)"
echo "java -version:  $JAVA_VER"
echo "javac -version: $JAVAC_VER"

case "$JAVA_VER" in
  *1.8.0_111*) echo "[TARGET_JDK_MATCH] Detected exact JDK 1.8.0_111." ;;
  *) echo "[TARGET_JDK_MISMATCH_WARNING] Detected JDK is not exactly 1.8.0_111. Compile will still be attempted; this warning does NOT mean the build failed, and this build script does NOT certify TARGET_JDK_RUNTIME_VERIFIED. Use verify-offline.sh for the mandatory exact-JDK gate." ;;
esac

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"

# Forward slashes in the argfile avoid the historical JDK9+ "@sources.txt + Windows backslash"
# argfile parsing issue. javac accepts forward slashes as path separators on Windows too.
find "$SRC_ROOT" -name '*.java' | sed 's#\\#/#g' > "$SOURCES_LIST"
COUNT=$(wc -l < "$SOURCES_LIST" | tr -d ' ')
echo "Java source files: $COUNT"

javac -encoding UTF-8 -d "$CLASSES_DIR" "@$SOURCES_LIST"
echo "[BUILD_OK] Compiled $COUNT source files into $CLASSES_DIR (0 errors)."
