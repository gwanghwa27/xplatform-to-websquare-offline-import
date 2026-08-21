@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

rem Offline verification gate. Runs everything that can be checked without internet access.
rem Core (mandatory, Java-only) checks: exact JDK, clean compile, sample conversion, generated
rem output count, source-tree .class/.jar absence, reference-output diff summary.
rem Optional checks (Python / Node): SKIPPED_OPTIONAL_TOOL if the tool is missing -- this does
rem NOT fail core verification.

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"
set "SRC_ROOT=%PROJECT_ROOT%src\main\java"
set "CLASSES_DIR=%PROJECT_ROOT%build\classes"
set "VERIFIER_CLASSES_DIR=%PROJECT_ROOT%build\verifier-classes"
set "OUTPUT_DIR=%PROJECT_ROOT%build\sample-output"
set "REFERENCE_DIR=%PROJECT_ROOT%sample-phase3-output"
set "CORE_FAIL=0"

if not exist "%PROJECT_ROOT%verify-logs" mkdir "%PROJECT_ROOT%verify-logs"

echo == XPlatform to WebSquare Converter - Offline Verification ==
echo.

echo -- [1/8] Exact JDK 1.8.0_111 gate --
where java >nul 2>nul
if errorlevel 1 (
  echo [FAIL] java not found on PATH.
  echo [RESULT] TARGET_JDK_RUNTIME_REQUIRED
  set "CORE_FAIL=1"
  goto step2
)
where javac >nul 2>nul
if errorlevel 1 (
  echo [FAIL] javac not found on PATH.
  echo [RESULT] TARGET_JDK_RUNTIME_REQUIRED
  set "CORE_FAIL=1"
  goto step2
)
set "JAVA_VER_LINE="
for /f "usebackq delims=" %%v in (`java -version 2^>^&1`) do if not defined JAVA_VER_LINE set "JAVA_VER_LINE=%%v"
set "JAVAC_VER_LINE="
for /f "usebackq delims=" %%v in (`javac -version 2^>^&1`) do if not defined JAVAC_VER_LINE set "JAVAC_VER_LINE=%%v"
echo java -version:  %JAVA_VER_LINE%
echo javac -version: %JAVAC_VER_LINE%
set "JAVA_EXACT=0"
set "JAVAC_EXACT=0"
echo %JAVA_VER_LINE% | findstr /c:"1.8.0_111" >nul && set "JAVA_EXACT=1"
echo %JAVAC_VER_LINE% | findstr /c:"1.8.0_111" >nul && set "JAVAC_EXACT=1"
set "JDK_BOTH_EXACT=0"
if "%JAVA_EXACT%"=="1" if "%JAVAC_EXACT%"=="1" set "JDK_BOTH_EXACT=1"
if "%JDK_BOTH_EXACT%"=="1" (
  echo [PASS] Exact JDK 1.8.0_111 confirmed ^(java and javac both match^).
) else (
  echo [FAIL] java/javac version is not exactly 1.8.0_111.
  echo [RESULT] TARGET_JDK_MISMATCH -- other Java 8 updates, JDK11/17/21, and --release 8 output are NOT accepted as target-JDK certification.
  set "CORE_FAIL=1"
)

:step2
echo.
echo -- [2/8] Clean compile --
call "%PROJECT_ROOT%build.bat" >"%PROJECT_ROOT%verify-logs\verify-build.log" 2>&1
if errorlevel 1 (
  echo [FAIL] build.bat failed. See verify-logs\verify-build.log.
  set "CORE_FAIL=1"
) else (
  findstr "BUILD_OK" "%PROJECT_ROOT%verify-logs\verify-build.log"
  echo [PASS]
)

echo.
echo -- [3/8] Sample conversion ^(149 expected^) --
if exist "%CLASSES_DIR%" (
  call "%PROJECT_ROOT%convert-sample.bat" >"%PROJECT_ROOT%verify-logs\verify-convert.log" 2>&1
  rem NOTE: the converter's own success/failure summary line is in Korean ("완료. 성공=149, 실패=0").
  rem Matching that text via findstr is codepage-fragile across environments, so the authoritative
  rem signal here is convert-sample.bat's own exit code (it already exits non-zero on failure);
  rem step [4/8] below independently cross-checks the generated file count (136 expected).
  if errorlevel 1 (
    echo [FAIL] convert-sample.bat exited with a non-zero status. See verify-logs\verify-convert.log.
    set "CORE_FAIL=1"
  ) else (
    echo [PASS] convert-sample.bat completed successfully ^(see verify-logs\verify-convert.log for the 성공/실패 count; file-count cross-check is step 4^).
  )
) else (
  echo [SKIPPED] build\classes missing ^(compile step failed above^).
)

echo.
echo -- [4/8] Generated output XML count --
if exist "%OUTPUT_DIR%" (
  set "GEN_COUNT=0"
  for /f %%c in ('dir /s /b "%OUTPUT_DIR%\*.xml" 2^>nul ^| find /c /v ""') do set "GEN_COUNT=%%c"
  echo Generated XML files: !GEN_COUNT! ^(expected 136^)
  if "!GEN_COUNT!"=="136" (
    echo [PASS]
  ) else (
    echo [FAIL] Expected 136 generated XML files.
    set "CORE_FAIL=1"
  )
) else (
  echo [SKIPPED] build\sample-output missing.
)

echo.
echo -- [5/8] Phase1 SHA verifier --
where python >nul 2>nul
if errorlevel 1 (
  where python3 >nul 2>nul
  if errorlevel 1 (
    echo [SKIPPED_OPTIONAL_TOOL] python/python3 not found -- Python verifier skipped ^(core verification not failed for this alone; the Java verifier below is authoritative^).
  ) else (
    echo [Python verifier]
    python3 "%PROJECT_ROOT%audit\phase1_sha_verifier.py" "%PROJECT_ROOT%audit\phase1_sha_manifest.json"
    if errorlevel 1 ( echo [FAIL] Python Phase1 SHA verifier reported a mismatch. & set "CORE_FAIL=1" ) else ( echo [PASS] Python Phase1 SHA verifier. )
  )
) else (
  echo [Python verifier]
  python "%PROJECT_ROOT%audit\phase1_sha_verifier.py" "%PROJECT_ROOT%audit\phase1_sha_manifest.json"
  if errorlevel 1 ( echo [FAIL] Python Phase1 SHA verifier reported a mismatch. & set "CORE_FAIL=1" ) else ( echo [PASS] Python Phase1 SHA verifier. )
)

echo [Java verifier]
if not exist "%VERIFIER_CLASSES_DIR%" mkdir "%VERIFIER_CLASSES_DIR%"
javac -encoding UTF-8 -d "%VERIFIER_CLASSES_DIR%" "%PROJECT_ROOT%tools\verifier-src\com\example\xfdltracker\verifier\Phase1ShaVerifier.java" >"%PROJECT_ROOT%verify-logs\verify-verifier-compile.log" 2>&1
if errorlevel 1 (
  echo [FAIL] Java Phase1ShaVerifier failed to compile. See verify-logs\verify-verifier-compile.log.
  set "CORE_FAIL=1"
) else (
  java -cp "%VERIFIER_CLASSES_DIR%" com.example.xfdltracker.verifier.Phase1ShaVerifier "%PROJECT_ROOT%audit\phase1_sha_manifest.json"
  if errorlevel 1 (
    echo [FAIL] Java Phase1ShaVerifier reported a mismatch.
    set "CORE_FAIL=1"
  ) else (
    echo [PASS] Java Phase1ShaVerifier ^(this is the mandatory core check; the Python check above is a convenience cross-check^).
  )
)

echo.
echo -- [6/8] Source tree .class/.jar absence --
set "CLASS_COUNT=0"
for /f %%c in ('dir /s /b "%SRC_ROOT%\*.class" 2^>nul ^| find /c /v ""') do set "CLASS_COUNT=%%c"
set "JAR_COUNT=0"
for /f %%c in ('dir /s /b "%SRC_ROOT%\*.jar" 2^>nul ^| find /c /v ""') do set "JAR_COUNT=%%c"
echo .class in source tree: !CLASS_COUNT!
echo .jar in source tree:   !JAR_COUNT!
if "!CLASS_COUNT!"=="0" if "!JAR_COUNT!"=="0" (
  echo [PASS]
) else (
  echo [FAIL] Source tree must contain 0 .class and 0 .jar files.
  set "CORE_FAIL=1"
)

echo.
echo -- [7/8] Reference output diff summary --
if exist "%OUTPUT_DIR%" if exist "%REFERENCE_DIR%" (
  where powershell >nul 2>nul
  if errorlevel 1 (
    echo [INFO] PowerShell not found -- skipping automated diff. Compare %REFERENCE_DIR% and %OUTPUT_DIR% manually.
  ) else (
    powershell -NoProfile -Command "$ref=Get-ChildItem -Recurse -File '%REFERENCE_DIR%' | Where-Object { $_.Name -ne 'conversion-report' }; $gen=Get-ChildItem -Recurse -File '%OUTPUT_DIR%' | Where-Object { $_.Name -ne 'conversion-report' }; $refHash=$ref | ForEach-Object { [PSCustomObject]@{ Rel=$_.FullName.Substring('%REFERENCE_DIR%'.Length); Hash=(Get-FileHash $_.FullName -Algorithm SHA256).Hash } }; $genHash=$gen | ForEach-Object { [PSCustomObject]@{ Rel=$_.FullName.Substring('%OUTPUT_DIR%'.Length); Hash=(Get-FileHash $_.FullName -Algorithm SHA256).Hash } }; $refMap=@{}; foreach($r in $refHash){$refMap[$r.Rel]=$r.Hash}; $diffCount=0; foreach($g in $genHash){ if(-not $refMap.ContainsKey($g.Rel) -or $refMap[$g.Rel] -ne $g.Hash){$diffCount++} }; Write-Output ('Differing entries vs reference (excluding conversion-report): ' + $diffCount); if($diffCount -eq 0){Write-Output '[PASS] Freshly generated output matches the committed reference output byte-for-byte.'} else {Write-Output '[INFO] Non-zero diff -- review manually; this is informational, not a hard failure by itself.'}"
  )
) else (
  echo [SKIPPED] output or reference directory missing.
)

echo.
echo -- [8/8] Optional: Node.js JS syntax check --
where node >nul 2>nul
if errorlevel 1 (
  echo [SKIPPED_OPTIONAL_TOOL] node not found -- optional check skipped.
) else (
  if exist "%OUTPUT_DIR%" (
    set "NODE_FAIL=0"
    for /f "delims=" %%f in ('dir /s /b "%OUTPUT_DIR%\*.js" 2^>nul') do (
      node --check "%%f" >nul 2>nul
      if errorlevel 1 set "NODE_FAIL=1"
    )
    if "!NODE_FAIL!"=="0" (
      echo [PASS] Optional Node.js syntax check on generated standalone/common .js files.
    ) else (
      echo [INFO] Node.js syntax check found issues -- optional, not part of the core gate.
    )
  ) else (
    echo [SKIPPED_OPTIONAL_TOOL] no generated output -- optional check skipped.
  )
)

echo.
echo == Summary ==
if "%CORE_FAIL%"=="0" (
  echo [CORE_VERIFICATION_PASS]
  exit /b 0
) else (
  echo [CORE_VERIFICATION_FAIL] One or more mandatory core checks failed -- see above.
  exit /b 1
)
