#!/bin/sh
# Offline verification gate. Runs everything that can be checked without internet access.
#
# Core (mandatory, Java-only) checks: exact JDK, clean compile, sample conversion, generated
# output count, source-tree .class/.jar absence, reference-output diff summary.
# Optional checks (Python / Node): SKIPPED_OPTIONAL_TOOL if the tool is missing -- this does
# NOT fail core verification.
set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
SRC_ROOT="$PROJECT_ROOT/src/main/java"
CLASSES_DIR="$PROJECT_ROOT/build/classes"
VERIFIER_CLASSES_DIR="$PROJECT_ROOT/build/verifier-classes"
OUTPUT_DIR="$PROJECT_ROOT/build/sample-output"
REFERENCE_DIR="$PROJECT_ROOT/sample-phase3-output"

CORE_FAIL=0

mkdir -p "$PROJECT_ROOT/verify-logs"

echo "== XPlatform to WebSquare Converter - Offline Verification =="
echo

# 1. exact Java/javac version -- mandatory core gate.
echo "-- [1/8] Exact JDK 1.8.0_111 gate --"
if ! command -v java >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1; then
  echo "[FAIL] java/javac not found on PATH."
  echo "[RESULT] TARGET_JDK_RUNTIME_REQUIRED"
  CORE_FAIL=1
else
  JAVA_VER="$(java -version 2>&1 | head -n 1)"
  JAVAC_VER="$(javac -version 2>&1 | head -n 1)"
  echo "java -version:  $JAVA_VER"
  echo "javac -version: $JAVAC_VER"
  case "$JAVA_VER" in *1.8.0_111*) JAVA_EXACT=1 ;; *) JAVA_EXACT=0 ;; esac
  case "$JAVAC_VER" in *1.8.0_111*) JAVAC_EXACT=1 ;; *) JAVAC_EXACT=0 ;; esac
  if [ "$JAVA_EXACT" = "1" ] && [ "$JAVAC_EXACT" = "1" ]; then
    echo "[PASS] Exact JDK 1.8.0_111 confirmed (java and javac both match)."
  else
    echo "[FAIL] java/javac version is not exactly 1.8.0_111."
    echo "[RESULT] TARGET_JDK_MISMATCH -- other Java 8 updates, JDK11/17/21, and --release 8 output are NOT accepted as target-JDK certification."
    CORE_FAIL=1
  fi
fi
echo

# 2. clean compile
echo "-- [2/8] Clean compile --"
if sh "$PROJECT_ROOT/build.sh" >"$PROJECT_ROOT/verify-logs/verify-build.log" 2>&1; then
  echo "[PASS] $(grep BUILD_OK "$PROJECT_ROOT/verify-logs/verify-build.log" || echo 'build.sh reported success')"
else
  echo "[FAIL] build.sh failed. See verify-logs/verify-build.log."
  CORE_FAIL=1
fi
echo

# 3. sample conversion
echo "-- [3/8] Sample conversion (149 expected) --"
if [ -d "$CLASSES_DIR" ]; then
  if sh "$PROJECT_ROOT/convert-sample.sh" >"$PROJECT_ROOT/verify-logs/verify-convert.log" 2>&1; then
    SUMMARY_LINE="$(grep -a "완료\. 성공=" "$PROJECT_ROOT/verify-logs/verify-convert.log" || true)"
    echo "$SUMMARY_LINE"
    echo "[PASS] convert-sample.sh completed successfully (file-count cross-check is step 4)."
  else
    echo "[FAIL] convert-sample.sh exited with a non-zero status. See verify-logs/verify-convert.log."
    CORE_FAIL=1
  fi
else
  echo "[SKIPPED] build/classes missing (compile step failed above)."
fi
echo

# 4. generated output count
echo "-- [4/8] Generated output XML count --"
if [ -d "$OUTPUT_DIR" ]; then
  GEN_COUNT="$(find "$OUTPUT_DIR" -name '*.xml' | wc -l | tr -d ' ')"
  echo "Generated XML files: $GEN_COUNT (expected 136)"
  if [ "$GEN_COUNT" = "136" ]; then
    echo "[PASS]"
  else
    echo "[FAIL] Expected 136 generated XML files."
    CORE_FAIL=1
  fi
else
  echo "[SKIPPED] build/sample-output missing."
fi
echo

# 5. Phase1 SHA verifier
echo "-- [5/8] Phase1 SHA verifier --"
PY_OK=0
if command -v python3 >/dev/null 2>&1; then PY_BIN=python3; PY_OK=1
elif command -v python >/dev/null 2>&1; then PY_BIN=python; PY_OK=1
fi
if [ "$PY_OK" = "1" ]; then
  echo "[Python verifier]"
  if "$PY_BIN" "$PROJECT_ROOT/audit/phase1_sha_verifier.py" "$PROJECT_ROOT/audit/phase1_sha_manifest.json"; then
    echo "[PASS] Python Phase1 SHA verifier."
  else
    echo "[FAIL] Python Phase1 SHA verifier reported a mismatch."
    CORE_FAIL=1
  fi
else
  echo "[SKIPPED_OPTIONAL_TOOL] python/python3 not found -- Python verifier skipped (core verification not failed for this alone; the Java verifier below is authoritative)."
fi

echo "[Java verifier]"
mkdir -p "$VERIFIER_CLASSES_DIR"
if javac -encoding UTF-8 -d "$VERIFIER_CLASSES_DIR" "$PROJECT_ROOT/tools/verifier-src/com/example/xfdltracker/verifier/Phase1ShaVerifier.java" >"$PROJECT_ROOT/verify-logs/verify-verifier-compile.log" 2>&1; then
  if java -cp "$VERIFIER_CLASSES_DIR" com.example.xfdltracker.verifier.Phase1ShaVerifier "$PROJECT_ROOT/audit/phase1_sha_manifest.json"; then
    echo "[PASS] Java Phase1ShaVerifier (this is the mandatory core check; the Python check above is a convenience cross-check)."
  else
    echo "[FAIL] Java Phase1ShaVerifier reported a mismatch."
    CORE_FAIL=1
  fi
else
  echo "[FAIL] Java Phase1ShaVerifier failed to compile. See verify-logs/verify-verifier-compile.log."
  CORE_FAIL=1
fi
echo

# 6. source tree .class/.jar absence
echo "-- [6/8] Source tree .class/.jar absence --"
CLASS_COUNT="$(find "$SRC_ROOT" "$PROJECT_ROOT/tools/verifier-src" -name '*.class' 2>/dev/null | wc -l | tr -d ' ')"
JAR_COUNT="$(find "$SRC_ROOT" "$PROJECT_ROOT/tools/verifier-src" -name '*.jar' 2>/dev/null | wc -l | tr -d ' ')"
echo ".class in source tree: $CLASS_COUNT"
echo ".jar in source tree:   $JAR_COUNT"
if [ "$CLASS_COUNT" = "0" ] && [ "$JAR_COUNT" = "0" ]; then
  echo "[PASS]"
else
  echo "[FAIL] Source tree must contain 0 .class and 0 .jar files."
  CORE_FAIL=1
fi
echo

# 7. baseline/reference output diff summary
echo "-- [7/8] Reference output diff summary --"
if [ -d "$OUTPUT_DIR" ] && [ -d "$REFERENCE_DIR" ]; then
  DIFF_COUNT="$(diff -rq "$REFERENCE_DIR" "$OUTPUT_DIR" 2>/dev/null | grep -v "conversion-report" | wc -l | tr -d ' ')"
  echo "Differing entries vs reference (excluding conversion-report): $DIFF_COUNT"
  if [ "$DIFF_COUNT" = "0" ]; then
    echo "[PASS] Freshly generated output matches the committed reference output byte-for-byte."
  else
    echo "[INFO] Non-zero diff -- review manually; this is informational, not a hard failure by itself."
  fi
else
  echo "[SKIPPED] output or reference directory missing."
fi
echo

# 8. optional: Node.js JS syntax check
echo "-- [8/8] Optional: Node.js JS syntax check --"
if command -v node >/dev/null 2>&1 && [ -d "$OUTPUT_DIR" ]; then
  NODE_FAIL=0
  for f in $(find "$OUTPUT_DIR" -name '*.js'); do
    node --check "$f" >/dev/null 2>&1 || NODE_FAIL=1
  done
  if [ "$NODE_FAIL" = "0" ]; then
    echo "[PASS] Optional Node.js syntax check on generated standalone/common .js files."
  else
    echo "[INFO] Node.js syntax check found issues -- optional, not part of the core gate."
  fi
else
  echo "[SKIPPED_OPTIONAL_TOOL] node not found or no generated output -- optional check skipped."
fi
echo

echo "== Summary =="
if [ "$CORE_FAIL" = "0" ]; then
  echo "[CORE_VERIFICATION_PASS]"
  exit 0
else
  echo "[CORE_VERIFICATION_FAIL] One or more mandatory core checks failed -- see above."
  exit 1
fi
