@echo off
setlocal EnableDelayedExpansion
REM grp_main style="" disappearance trace -- Windows cmd version of build-pipeline-trace.sh.
REM
REM NOT part of the Production converter. For closed-network Windows 10/11 cmd, no Git Bash /
REM WSL / Cygwin required. Uses only: cmd.exe built-ins, javac/java (JDK8 already required by
REM this project), and PowerShell (ships with Windows 10/11) for SHA-256 hashing and for
REM extracting the grp_resultArea/grp_main style attribute from the generated XML text (regex
REM on the raw file text -- not an XML parser, no external library).
REM
REM Usage (all on one line; split here across REM lines only for readability, no line
REM continuation caret used so batch parsing of this comment block cannot be affected):
REM   build-pipeline-trace.bat "candidate-repo-root" "project-root" "output-directory"
REM     "screen-rel-path-no-ext, e.g. Form\stt\STT00030 or Form/stt/STT00030"
REM     ["stage-b file"] ["stage-c file"] ["stage-d file"] ["search-root"]
REM
REM All arguments must be double-quoted (Korean/space paths are supported throughout via
REM delayed expansion + explicit quoting).

set "CAND_ROOT=%~1"
set "PROJECT_ROOT=%~2"
set "OUT_ROOT=%~3"
set "SCREEN_REL=%~4"
set "STAGE_B=%~5"
set "STAGE_C=%~6"
set "STAGE_D=%~7"
set "SEARCH_ROOT=%~8"

if "%CAND_ROOT%"=="" goto :usage
if "%PROJECT_ROOT%"=="" goto :usage
if "%OUT_ROOT%"=="" goto :usage
if "%SCREEN_REL%"=="" goto :usage
goto :main

:usage
echo Usage: build-pipeline-trace.bat "candidate-repo-root" "project-root" "output-dir" "screen-rel-path-no-ext" ["stage-b-file"] ["stage-c-file"] ["stage-d-file"] ["search-root"]
exit /b 2

:main
echo == grp_main style disappearance trace (Windows) ==
echo.

REM ---- PowerShell availability check (SHA-256 / style extraction depend on it) ----
set "PS_AVAILABLE=1"
powershell -NoProfile -Command "$null" >nul 2>nul
if errorlevel 1 set "PS_AVAILABLE=0"
echo POWERSHELL_AVAILABLE=%PS_AVAILABLE%
echo.

REM ---- Git ----
pushd "%CAND_ROOT%" || (echo [FAIL] cannot cd into candidate-repo-root & exit /b 1)
echo -- Git --
for /f "delims=" %%H in ('git rev-parse HEAD') do set "GIT_HEAD=%%H"
echo HEAD=%GIT_HEAD%
set "GIT_DIRTY="
for /f "delims=" %%L in ('git status --short') do set "GIT_DIRTY=1"
if defined GIT_DIRTY (
  echo [WARN] working tree not clean:
  git status --short
) else (
  echo working tree clean
)
echo.

REM ---- Clean build ----
echo -- Clean build --
if exist "build" rmdir /s /q "build"
mkdir "build\classes"
set "SRCLIST=build\srclist.txt"
if exist "%SRCLIST%" del /q "%SRCLIST%"
for /f "delims=" %%F in ('dir /s /b "src\main\java\*.java"') do (
  echo %%F>>"%SRCLIST%"
)
javac -encoding UTF-8 -d "build\classes" @"%SRCLIST%"
if errorlevel 1 (
  echo [FAIL] javac compile failed
  popd
  exit /b 1
)
del /q "%SRCLIST%"
echo [PASS] clean compile
echo.

REM ---- Report the exact command/classpath used (no stale class/jar mixed in) ----
set "CLASSPATH_USED=%CAND_ROOT%\build\classes"
echo JAVA_COMMAND=java -Dfile.encoding=UTF-8 -cp "%CLASSPATH_USED%" com.example.xfdltracker.project.XPlatformProjectConverter "%PROJECT_ROOT%" ^<fresh-output-dir^> UTF-8
echo JAVA_CLASSPATH=%CLASSPATH_USED%
echo.

REM ---- Stage A: fresh conversion ----
echo -- Stage A: fresh conversion --
for /f "delims=" %%T in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "TS=%%T"
if "%TS%"=="" set "TS=%RANDOM%"
set "FRESH_OUT=%OUT_ROOT%\stageA-%TS%"
mkdir "%FRESH_OUT%" 2>nul
java -Dfile.encoding=UTF-8 -cp "%CLASSPATH_USED%" com.example.xfdltracker.project.XPlatformProjectConverter "%PROJECT_ROOT%" "%FRESH_OUT%" UTF-8 > "%FRESH_OUT%.log" 2>&1
echo CONVERSION_LOG=%FRESH_OUT%.log ^(마지막 줄, 인코딩 문제를 피하기 위해 findstr로 한글 문자열을 매칭하지 않고 그대로 출력^)
powershell -NoProfile -Command "Get-Content -LiteralPath '%FRESH_OUT%.log' -Tail 5 -Encoding UTF8"
echo.

set "SCREEN_REL_BS=%SCREEN_REL:/=\%"
set "STAGE_A_FILE=%FRESH_OUT%\%SCREEN_REL_BS%.xml"

call :report_stage STAGE_A "%STAGE_A_FILE%"
call :report_stage STAGE_B "%STAGE_B%"
call :report_stage STAGE_C "%STAGE_C%"
call :report_stage STAGE_D "%STAGE_D%"

REM ---- Duplicate classpath / duplicate filename search ----
REM Implemented as a CALL:ed subroutine (not an inline parenthesized if/else block) -- batch
REM parses an entire ( ... ) block ahead of time, which is fragile with REM lines / nested
REM quoting / pipes inside it. A subroutine is parsed and executed one line at a time instead.
if "%SEARCH_ROOT%"=="" goto :search_skip
call :search_duplicates
goto :after_search
:search_skip
echo -- Duplicate/same-name search skipped ^(no search-root given^) --
:after_search

echo.
echo == Done. Compare STAGE_*_GRP_MAIN_STYLE above to find the first stage where it becomes empty. ==
popd
endlocal
exit /b 0

REM ==================================================================
:search_duplicates
setlocal EnableDelayedExpansion
echo -- Duplicate classpath / duplicate filename search under %SEARCH_ROOT% --
set "WSG_COUNT=0"
for /f %%C in ('dir /s /b "%SEARCH_ROOT%\WebSquareGenerator.class" 2^>nul ^| find /c /v ""') do set "WSG_COUNT=%%C"
echo DUPLICATE_WEBSQUARE_GENERATOR_CLASS_COUNT=!WSG_COUNT!
dir /s /b "%SEARCH_ROOT%\WebSquareGenerator.class" 2>nul

set "CLC_COUNT=0"
for /f %%C in ('dir /s /b "%SEARCH_ROOT%\ComponentLayoutConverter.class" 2^>nul ^| find /c /v ""') do set "CLC_COUNT=%%C"
echo DUPLICATE_COMPONENT_LAYOUT_CONVERTER_CLASS_COUNT=!CLC_COUNT!
dir /s /b "%SEARCH_ROOT%\ComponentLayoutConverter.class" 2>nul

set "JAR_COUNT=0"
for /f %%C in ('dir /s /b "%SEARCH_ROOT%\*.jar" 2^>nul ^| find /c /v ""') do set "JAR_COUNT=%%C"
echo JAR_FILE_COUNT_UNDER_SEARCH_ROOT=!JAR_COUNT!
dir /s /b "%SEARCH_ROOT%\*.jar" 2>nul
echo.

for %%N in ("%SCREEN_REL_BS%.xml") do set "SCREEN_BASENAME=%%~nxN"
echo -- Same-name XML search (basename=!SCREEN_BASENAME!) --
set "SAME_NAME_COUNT=0"
for /f %%C in ('dir /s /b "%SEARCH_ROOT%\!SCREEN_BASENAME!" 2^>nul ^| find /c /v ""') do set "SAME_NAME_COUNT=%%C"
echo SAME_NAME_XML_COUNT=!SAME_NAME_COUNT!
for /f "delims=" %%P in ('dir /s /b "%SEARCH_ROOT%\!SCREEN_BASENAME!" 2^>nul') do (
  call :sha256 "%%P" HASH_TMP
  echo   %%P  sha256=!HASH_TMP!
)
endlocal
goto :eof

REM ==================================================================
:report_stage
REM %1 = label, %2 = file path (already quoted by caller via %~2 semantics)
setlocal EnableDelayedExpansion
set "LABEL=%~1"
set "FILE=%~2"
echo -- %LABEL% --
if "%FILE%"=="" (
  echo %LABEL%_FILE=^(not provided^)
  echo %LABEL%_SHA256=N/A
  echo %LABEL%_SIZE=N/A
  echo %LABEL%_GRP_RESULT_AREA_STYLE=N/A
  echo %LABEL%_GRP_MAIN_STYLE=N/A
  echo.
  endlocal
  goto :eof
)
if not exist "%FILE%" (
  echo %LABEL%_FILE=%FILE% ^(NOT FOUND^)
  echo %LABEL%_SHA256=N/A
  echo %LABEL%_SIZE=N/A
  echo %LABEL%_GRP_RESULT_AREA_STYLE=N/A
  echo %LABEL%_GRP_MAIN_STYLE=N/A
  echo.
  endlocal
  goto :eof
)
echo %LABEL%_FILE=%FILE%
call :sha256 "%FILE%" HASH_OUT
echo %LABEL%_SHA256=!HASH_OUT!
for %%Z in ("%FILE%") do echo %LABEL%_SIZE=%%~zZ

REM grp-main-style-check.ps1 is invoked with -File (not -Command), so the target path is
REM passed as a normal quoted argument -- no nested-quote escaping through cmd is needed, and
REM Korean/space paths work the same as any other path.
set "RESULTAREA_STYLE="
set "GRPMAIN_STYLE="
set "GRPMAIN_EMPTY="
for /f "usebackq tokens=1,* delims==" %%A in (`powershell -NoProfile -File "%~dp0grp-main-style-check.ps1" -TargetPath "%FILE%"`) do (
  if "%%A"=="RESULTAREA" set "RESULTAREA_STYLE=%%B"
  if "%%A"=="GRPMAIN" set "GRPMAIN_STYLE=%%B"
  if "%%A"=="EMPTY" set "GRPMAIN_EMPTY=%%B"
)
echo %LABEL%_GRP_RESULT_AREA_STYLE=!RESULTAREA_STYLE!
echo %LABEL%_GRP_MAIN_STYLE=!GRPMAIN_STYLE!
echo %LABEL%_GRP_MAIN_STYLE_EMPTY=!GRPMAIN_EMPTY!
echo.
endlocal
goto :eof

REM ==================================================================
:sha256
REM %1 = file path (quoted), %2 = name of caller variable to receive the hash
if "%PS_AVAILABLE%"=="0" (
  set "%~2=NOT_EXECUTED"
  goto :eof
)
set "_H="
for /f "usebackq delims=" %%X in (`powershell -NoProfile -Command "(Get-FileHash -Algorithm SHA256 -LiteralPath '%~1').Hash"`) do set "_H=%%X"
if "%_H%"=="" set "_H=NOT_EXECUTED"
set "%~2=%_H%"
goto :eof
