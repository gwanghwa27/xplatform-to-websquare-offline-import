#!/bin/sh
# grp_main style="" disappearance trace -- offline pipeline stage comparator.
#
# NOT part of the Production converter. Run this INSIDE the closed network, where the full
# pipeline (candidate source -> build -> convert -> transfer -> WebSquare workspace -> Studio
# save) actually exists. It cannot be run from outside that environment, which is exactly why
# this script exists: it packages the exact commands so the closed-network operator can run the
# same trace Claude Code would run if it had access.
#
# What it does:
#   1. Stage A: clean rebuild the candidate Production source, convert the real project fresh
#      into a new timestamped output dir, and read grp_resultArea/grp_main style DIRECTLY from
#      the converter's own output file (no Studio involved).
#   2. Stage B/C/D (optional): if you pass already-on-disk file paths for the transferred file,
#      the WebSquare workspace copy, and the Studio-saved copy, it hashes and greps each the
#      same way, so all 4 stages are compared on equal footing.
#   3. Optional duplicate-classpath / duplicate-filename search under a given root: looks for
#      more than one WebSquareGenerator.class / ComponentLayoutConverter.class or any .jar file,
#      and for more than one file with the same basename as the screen you're tracing.
#
# No screen id/business id is hardcoded here -- SCREEN_REL_PATH is a required argument you
# supply at run time.
#
# Usage:
#   sh tools/build-pipeline-trace.sh <candidate-repo-root> <project-root> <fresh-output-root> \
#       <screen-rel-path-no-ext> [stage-b-file] [stage-c-file] [stage-d-file] [search-root]
#
# Example:
#   sh tools/build-pipeline-trace.sh \
#       /path/to/candidate/working-copy \
#       /path/to/xplatform/project \
#       /tmp/pipeline-trace-out \
#       Form/stt/STT00030 \
#       /path/to/transferred/STT00030.xml \
#       /path/to/webtop/workspace/.../stt/STT00030.xml \
#       /path/to/webtop/workspace/.../stt/STT00030.xml \
#       /path/to/webtop
#
# (stage C and D commonly point at the same on-disk workspace file -- pass it twice, once
# before opening/saving in Studio and once after, as two separate script runs if you want to
# isolate the Studio-save step specifically.)

set -eu

CAND_ROOT="${1:?candidate repo root required}"
PROJECT_ROOT="${2:?project root (real XPlatform source project) required}"
OUT_ROOT="${3:?fresh output root required}"
SCREEN_REL="${4:?screen relative path, no extension, e.g. Form/stt/STT00030 required}"
STAGE_B="${5:-}"
STAGE_C="${6:-}"
STAGE_D="${7:-}"
SEARCH_ROOT="${8:-}"

echo "== grp_main style disappearance trace =="
echo

cd "$CAND_ROOT"
echo "-- Git --"
echo "HEAD=$(git rev-parse HEAD)"
DIRTY=$(git status --short)
if [ -n "$DIRTY" ]; then
  echo "[WARN] working tree not clean:"
  echo "$DIRTY"
else
  echo "working tree clean"
fi
echo

echo "-- Clean build --"
rm -rf build
mkdir -p build/classes
find src/main/java -name '*.java' > build/srclist.txt
javac -encoding UTF-8 -d build/classes @build/srclist.txt
rm -f build/srclist.txt
echo "[PASS] clean compile"
echo

echo "-- Stage A: fresh conversion --"
TS=$(date +%Y%m%d-%H%M%S)
FRESH_OUT="$OUT_ROOT/stageA-$TS"
mkdir -p "$FRESH_OUT"
java -Dfile.encoding=UTF-8 -cp build/classes com.example.xfdltracker.project.XPlatformProjectConverter \
    "$PROJECT_ROOT" "$FRESH_OUT" UTF-8 > "$FRESH_OUT.log" 2>&1 || true
tail -5 "$FRESH_OUT.log"
STAGE_A_FILE="$FRESH_OUT/$SCREEN_REL.xml"
echo

report_stage() {
  label="$1"
  file="$2"
  echo "-- $label --"
  if [ -z "$file" ] || [ ! -f "$file" ]; then
    echo "${label}_FILE=(not provided or not found: $file)"
    echo "${label}_SHA256=N/A"
    echo "${label}_SIZE=N/A"
    echo "${label}_GRP_RESULT_AREA_STYLE=N/A"
    echo "${label}_GRP_MAIN_STYLE=N/A"
    echo
    return
  fi
  echo "${label}_FILE=$file"
  echo "${label}_SHA256=$(sha256sum "$file" | awk '{print $1}')"
  echo "${label}_SIZE=$(wc -c < "$file" | tr -d ' ')"
  resultarea=$(grep -o 'id="grp_resultArea"[^/]*' "$file" || echo "(id=grp_resultArea not found)")
  main=$(grep -o 'id="grp_main"[^/]*' "$file" || echo "(id=grp_main not found)")
  echo "${label}_GRP_RESULT_AREA_STYLE=$resultarea"
  echo "${label}_GRP_MAIN_STYLE=$main"
  echo
}

report_stage "STAGE_A" "$STAGE_A_FILE"
report_stage "STAGE_B" "$STAGE_B"
report_stage "STAGE_C" "$STAGE_C"
report_stage "STAGE_D" "$STAGE_D"

if [ -n "$SEARCH_ROOT" ] && [ -d "$SEARCH_ROOT" ]; then
  echo "-- Duplicate classpath / duplicate filename search under $SEARCH_ROOT --"
  WSG_COUNT=$(find "$SEARCH_ROOT" -iname 'WebSquareGenerator.class' 2>/dev/null | wc -l | tr -d ' ')
  CLC_COUNT=$(find "$SEARCH_ROOT" -iname 'ComponentLayoutConverter.class' 2>/dev/null | wc -l | tr -d ' ')
  JAR_COUNT=$(find "$SEARCH_ROOT" -iname '*.jar' 2>/dev/null | wc -l | tr -d ' ')
  echo "DUPLICATE_WEBSQUARE_GENERATOR_CLASS_COUNT=$WSG_COUNT"
  find "$SEARCH_ROOT" -iname 'WebSquareGenerator.class' 2>/dev/null | sed 's/^/  /'
  echo "DUPLICATE_COMPONENT_LAYOUT_CONVERTER_CLASS_COUNT=$CLC_COUNT"
  find "$SEARCH_ROOT" -iname 'ComponentLayoutConverter.class' 2>/dev/null | sed 's/^/  /'
  echo "JAR_FILE_COUNT_UNDER_SEARCH_ROOT=$JAR_COUNT"
  find "$SEARCH_ROOT" -iname '*.jar' 2>/dev/null | sed 's/^/  /'
  echo

  SCREEN_BASENAME=$(basename "$SCREEN_REL").xml
  echo "-- Same-name XML search (basename=$SCREEN_BASENAME) --"
  SAME_NAME_COUNT=$(find "$SEARCH_ROOT" -iname "$SCREEN_BASENAME" 2>/dev/null | wc -l | tr -d ' ')
  echo "SAME_NAME_XML_COUNT=$SAME_NAME_COUNT"
  find "$SEARCH_ROOT" -iname "$SCREEN_BASENAME" 2>/dev/null | while read -r p; do
    echo "  $p  sha256=$(sha256sum "$p" | awk '{print $1}')"
  done
else
  echo "-- Duplicate/same-name search skipped (no search-root given) --"
fi

echo
echo "== Done. Compare STAGE_*_GRP_MAIN_STYLE above to find the first stage where it becomes empty. =="
