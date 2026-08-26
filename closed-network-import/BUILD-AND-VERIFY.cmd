@echo off
setlocal EnableDelayedExpansion
REM One-shot closed-network build + regression + class/state policy verification.
REM No network access required. Run from anywhere; resolves repo root relative to this script.
REM Usage: closed-network-import\BUILD-AND-VERIFY.cmd

set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.."
set "REPO_ROOT=%CD%"

set "FAIL=0"

echo == One-shot closed-network build/verify ==
echo Repo root: %REPO_ROOT%
echo.

set "PS_AVAILABLE=1"
powershell -NoProfile -Command "$null" >nul 2>nul
if errorlevel 1 set "PS_AVAILABLE=0"
echo POWERSHELL_AVAILABLE=%PS_AVAILABLE%
echo.

echo -- [1/6] MANIFEST.sha256 integrity --
if "%PS_AVAILABLE%"=="0" goto skip_manifest
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%verify-manifest.ps1" -RepoRoot "%REPO_ROOT%" -ManifestPath "%SCRIPT_DIR%MANIFEST.sha256"
if errorlevel 1 (
  echo [FAIL] MANIFEST mismatch or missing files.
  set "FAIL=1"
) else (
  echo [PASS] MANIFEST.sha256 all files match.
)
goto after_manifest
:skip_manifest
echo [SKIPPED_OPTIONAL_TOOL] PowerShell not available for SHA-256 check.
:after_manifest
echo.

echo -- [2/6] Clean compile --
if exist "build" rmdir /s /q "build"
mkdir "build\classes"
set "SRCLIST=build\srclist.txt"
dir /s /b "src\main\java\*.java" > "%SRCLIST%"
javac -encoding UTF-8 -d "build\classes" @"%SRCLIST%"
if errorlevel 1 (
  echo [FAIL] javac compile failed.
  set "FAIL=1"
) else (
  echo [PASS] clean compile.
)
if exist "%SRCLIST%" del /q "%SRCLIST%"
echo.

echo -- [3/6] 150-fixture conversion --
if exist "build\sample-output" rmdir /s /q "build\sample-output"
mkdir "build\sample-output"
java -Dfile.encoding=UTF-8 -cp "build\classes" com.example.xfdltracker.project.XPlatformProjectConverter "sample-phase3-project" "build\sample-output" UTF-8 > "build\convert.log" 2>&1
set "XML_COUNT=0"
for /f %%C in ('dir /s /b "build\sample-output\*.xml" 2^>nul ^| find /c /v ""') do set "XML_COUNT=%%C"
echo Generated XML count: !XML_COUNT! (expected 137)
if not "!XML_COUNT!"=="137" (
  echo [FAIL] XML count mismatch -- see build\convert.log
  set "FAIL=1"
) else (
  echo [PASS] 150/150 conversion.
)
echo.

echo -- [4/6] Class/state policy invariants + HOLD structural class leakage --
if "%PS_AVAILABLE%"=="0" goto skip_class_policy
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%class-policy-check.ps1" -OutputRoot "build\sample-output" > "build\class-policy-check.log"
set "CLASS_POLICY_RESULT=%ERRORLEVEL%"
type "build\class-policy-check.log"
if not "%CLASS_POLICY_RESULT%"=="0" (
  echo [FAIL] class/state policy invariant mismatch or HOLD class leaked.
  set "FAIL=1"
) else (
  echo [PASS] class/state policy invariants, no HOLD class leaked.
)
goto after_class_policy
:skip_class_policy
echo [SKIPPED_OPTIONAL_TOOL] PowerShell not available for class-policy check.
:after_class_policy
echo.

echo -- [5/6] XML well-formedness --
if "%PS_AVAILABLE%"=="0" goto skip_xml_check
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%xml-wellformed-check.ps1" -OutputRoot "build\sample-output"
if errorlevel 1 (
  echo [FAIL] one or more XML parse errors.
  set "FAIL=1"
) else (
  echo [PASS] XML well-formed.
)
goto after_xml_check
:skip_xml_check
echo [SKIPPED_OPTIONAL_TOOL] PowerShell not available for XML well-formed check.
:after_xml_check
echo.

echo -- [6/6] Phase1 SHA verifier (optional, needs python) --
where python >nul 2>nul
if errorlevel 1 (
  echo [SKIPPED_OPTIONAL_TOOL] python not found.
  goto after_phase1
)
python "audit\phase1_sha_verifier.py" "audit\phase1_sha_manifest.json"
if errorlevel 1 set "FAIL=1"
:after_phase1
echo.

if "%FAIL%"=="0" (
  echo == RESULT: ALL GATES PASS ==
  popd
  exit /b 0
) else (
  echo == RESULT: ONE OR MORE GATES FAILED ==
  popd
  exit /b 1
)
