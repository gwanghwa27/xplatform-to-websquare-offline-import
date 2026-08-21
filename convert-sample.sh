#!/bin/sh
# Compiles (if needed) and converts sample-phase3-project/ into build/sample-output/.
# The reference output under sample-phase3-output/ is never overwritten by this script.
set -eu

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
CLASSES_DIR="$PROJECT_ROOT/build/classes"
INPUT_DIR="$PROJECT_ROOT/sample-phase3-project"
OUTPUT_DIR="$PROJECT_ROOT/build/sample-output"

echo "== XPlatform to WebSquare Converter - Sample Conversion =="

if [ ! -d "$CLASSES_DIR" ]; then
  echo "build/classes not found -- running build.sh first."
  "$SCRIPT_DIR/build.sh"
fi

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

echo "Input:  $INPUT_DIR"
echo "Output: $OUTPUT_DIR"
echo "Reference (not overwritten): $PROJECT_ROOT/sample-phase3-output"

java -Dfile.encoding=UTF-8 -cp "$CLASSES_DIR" \
  com.example.xfdltracker.project.XPlatformProjectConverter \
  "$INPUT_DIR" "$OUTPUT_DIR" "UTF-8"

echo "[CONVERT_DONE] See $OUTPUT_DIR/conversion-report for the 성공/실패 (success/failure) summary line."
echo "Expected: 149/149 (성공=149, 실패=0)."
