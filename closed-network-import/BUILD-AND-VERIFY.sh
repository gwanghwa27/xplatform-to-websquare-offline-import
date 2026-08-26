#!/bin/sh
# One-shot closed-network build + regression + class/state policy verification.
# No network access required. Run from anywhere -- resolves repo root relative to this script.
# Usage: sh closed-network-import/BUILD-AND-VERIFY.sh
set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

FAIL=0

echo "== One-shot closed-network build/verify =="
echo "Repo root: $REPO_ROOT"
echo

echo "-- [1/6] MANIFEST.sha256 integrity --"
if command -v sha256sum >/dev/null 2>&1; then
  if (cd "$REPO_ROOT" && sha256sum -c "closed-network-import/MANIFEST.sha256" > closed-network-import/manifest-check.log 2>&1); then
    echo "[PASS] MANIFEST.sha256 all files match."
  else
    echo "[FAIL] MANIFEST mismatch or missing files -- see closed-network-import/manifest-check.log"
    FAIL=1
  fi
else
  echo "[SKIPPED_OPTIONAL_TOOL] sha256sum not found."
fi
echo

echo "-- [2/6] Clean compile --"
rm -rf build
mkdir -p build/classes
find src/main/java -name '*.java' > build/srclist.txt
if javac -encoding UTF-8 -d build/classes @build/srclist.txt; then
  echo "[PASS] clean compile."
else
  echo "[FAIL] javac compile failed."
  FAIL=1
fi
rm -f build/srclist.txt
echo

echo "-- [3/6] 150-fixture conversion --"
rm -rf build/sample-output
mkdir -p build/sample-output
java -Dfile.encoding=UTF-8 -cp build/classes com.example.xfdltracker.project.XPlatformProjectConverter sample-phase3-project build/sample-output UTF-8 > build/convert.log 2>&1
XML_COUNT=$(find build/sample-output -name '*.xml' | wc -l | tr -d ' ')
echo "Generated XML count: $XML_COUNT (expected 137)"
if [ "$XML_COUNT" = "137" ] && grep -q "실패=0" build/convert.log; then
  echo "[PASS] 150/150 conversion."
else
  echo "[FAIL] conversion count/result mismatch -- see build/convert.log"
  FAIL=1
fi
echo

echo "-- [4/6] Class/state policy invariants --"
BTN_CM=$(grep -ro 'class="[^"]*btn_cm[^"]*"' build/sample-output --include='*.xml' | wc -l | tr -d ' ')
WQ_GVW=$(grep -ro 'wq_gvw' build/sample-output --include='*.xml' | wc -l | tr -d ' ')
DISABLED_SB=$(grep -ro 'disabledClass="w2selectbox_disabled"' build/sample-output --include='*.xml' | wc -l | tr -d ' ')
echo "btn_cm=$BTN_CM (expected 12), wq_gvw=$WQ_GVW (expected 3), w2selectbox_disabled=$DISABLED_SB (expected 4)"
if [ "$BTN_CM" = "12" ] && [ "$WQ_GVW" = "3" ] && [ "$DISABLED_SB" = "4" ]; then
  echo "[PASS] class/state policy invariants."
else
  echo "[FAIL] class/state policy invariant mismatch."
  FAIL=1
fi

echo "-- HOLD structural class leakage check (expect 0 for all) --"
LEAK=0
for cls in shbox dfbox tbbox lybox ly_column ly_form btnbox pgtbox rcard; do
  n=$(grep -rlo "class=\"[^\"]*\b$cls\b[^\"]*\"" build/sample-output --include='*.xml' | wc -l | tr -d ' ')
  if [ "$n" != "0" ]; then
    echo "  [WARN] unexpected HOLD class '$cls' found in $n file(s)"
    LEAK=1
  fi
done
if [ "$LEAK" = "0" ]; then
  echo "[PASS] no HOLD structural class leaked into output."
else
  echo "[FAIL] HOLD structural class leaked into output -- review."
  FAIL=1
fi
echo

echo "-- [5/6] XML well-formedness --"
PYBIN=""
if command -v python3 >/dev/null 2>&1; then
  PYBIN=python3
elif command -v python >/dev/null 2>&1; then
  PYBIN=python
fi
if [ -n "$PYBIN" ]; then
  if "$PYBIN" - <<'PYEOF'
import glob, xml.dom.minidom as m, sys
files = glob.glob("build/sample-output/**/*.xml", recursive=True)
err = 0
for f in files:
    try:
        m.parse(f)
    except Exception as e:
        print("FAIL:", f, e)
        err += 1
print("XML_PARSE_TOTAL=" + str(len(files)) + " XML_PARSE_ERR=" + str(err))
sys.exit(1 if err else 0)
PYEOF
  then
    echo "[PASS] XML well-formed."
  else
    echo "[FAIL] XML parse error(s)."
    FAIL=1
  fi
else
  echo "[SKIPPED_OPTIONAL_TOOL] python/python3 not found."
fi
echo

echo "-- [6/6] Phase1 SHA verifier --"
if [ -n "$PYBIN" ]; then
  if "$PYBIN" audit/phase1_sha_verifier.py audit/phase1_sha_manifest.json; then
    :
  else
    FAIL=1
  fi
else
  echo "[SKIPPED_OPTIONAL_TOOL] python/python3 not found."
fi
echo

if [ "$FAIL" = "0" ]; then
  echo "== RESULT: ALL GATES PASS =="
  exit 0
else
  echo "== RESULT: ONE OR MORE GATES FAILED =="
  exit 1
fi
