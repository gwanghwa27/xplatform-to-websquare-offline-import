@echo off
setlocal DisableDelayedExpansion

set "REPO=%~1"
set "PROJECT=%~2"
set "OUTPUT=%~3"
set "SCREEN=%~4"

if "%~4"=="" goto usage

echo == grp_main style trace, Stage A only ==
echo REPO=%REPO%
echo PROJECT=%PROJECT%
echo OUTPUT=%OUTPUT%
echo SCREEN=%SCREEN%
echo.

cd /d "%REPO%"
if errorlevel 1 goto fail_cd

echo GIT_REQUIRED=NO

set "SOURCE_HEAD="
set "SOURCE_HEAD_SOURCE=NOT_AVAILABLE"
set "PROVENANCE=%REPO%\analysis\build-provenance.txt"
set "SOURCE_HEAD_TXT=%REPO%\SOURCE_HEAD.txt"

if not exist "%PROVENANCE%" goto try_source_head_file
for /f "tokens=1,* delims==" %%A in ('findstr "^PRODUCTION_SOURCE_HEAD=" "%PROVENANCE%"') do set "SOURCE_HEAD=%%B"
if not "%SOURCE_HEAD%"=="" set "SOURCE_HEAD_SOURCE=BUILD_PROVENANCE"
if not "%SOURCE_HEAD%"=="" goto source_head_done
for /f "tokens=1,* delims==" %%A in ('findstr "^SOURCE_HEAD=" "%PROVENANCE%"') do set "SOURCE_HEAD=%%B"
if not "%SOURCE_HEAD%"=="" set "SOURCE_HEAD_SOURCE=BUILD_PROVENANCE"
if not "%SOURCE_HEAD%"=="" goto source_head_done

:try_source_head_file
if not exist "%SOURCE_HEAD_TXT%" goto source_head_done
set /p SOURCE_HEAD=<"%SOURCE_HEAD_TXT%"
if not "%SOURCE_HEAD%"=="" set "SOURCE_HEAD_SOURCE=SOURCE_HEAD_FILE"

:source_head_done
if "%SOURCE_HEAD%"=="" set "SOURCE_HEAD=NOT_AVAILABLE"
echo SOURCE_HEAD=%SOURCE_HEAD%
echo SOURCE_HEAD_SOURCE=%SOURCE_HEAD_SOURCE%
echo.

echo -- clean build --
if exist "build" rmdir /s /q "build"
mkdir "build\classes"

set "SRCLIST=build\srclist.txt"
dir /s /b "src\main\java\*.java" > "%SRCLIST%"

javac -encoding UTF-8 -d "build\classes" @"%SRCLIST%"
if errorlevel 1 goto fail_build
echo build ok
echo.

set "CP=%REPO%\build\classes"
echo JAVA_CLASSPATH=%CP%
echo.

if not exist "%OUTPUT%" mkdir "%OUTPUT%"

echo -- fresh conversion --
java -Dfile.encoding=UTF-8 -cp "%CP%" com.example.xfdltracker.project.XPlatformProjectConverter "%PROJECT%" "%OUTPUT%" UTF-8 > "%OUTPUT%\convert.log" 2>&1
if errorlevel 1 goto fail_convert
echo conversion ok
echo.

set "SCREEN_BS=%SCREEN:/=\%"
set "STAGE_A_PATH=%OUTPUT%\%SCREEN_BS%.xml"
echo STAGE_A_PATH=%STAGE_A_PATH%

if not exist "%STAGE_A_PATH%" goto fail_missing

set "SHA_OUT=%OUTPUT%\stage-a-sha256.txt"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0file-sha256.ps1" -TargetPath "%STAGE_A_PATH%" > "%SHA_OUT%"
set /p STAGE_A_SHA256=<"%SHA_OUT%"
echo STAGE_A_SHA256=%STAGE_A_SHA256%

set "STYLE_OUT=%OUTPUT%\stage-a-style.txt"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0grp-main-style-check.ps1" -TargetPath "%STAGE_A_PATH%" > "%STYLE_OUT%"
for /f "tokens=1,* delims==" %%A in ('findstr "^RESULTAREA=" "%STYLE_OUT%"') do set "RESULTAREA_STYLE=%%B"
for /f "tokens=1,* delims==" %%A in ('findstr "^GRPMAIN=" "%STYLE_OUT%"') do set "GRPMAIN_STYLE=%%B"
for /f "tokens=1,* delims==" %%A in ('findstr "^EMPTY=" "%STYLE_OUT%"') do set "GRPMAIN_EMPTY=%%B"
echo STAGE_A_GRP_RESULT_AREA_STYLE=%RESULTAREA_STYLE%
echo STAGE_A_GRP_MAIN_STYLE=%GRPMAIN_STYLE%
echo GRP_MAIN_STYLE_EMPTY=%GRPMAIN_EMPTY%
echo.
echo == done ==
exit /b 0

:usage
echo Usage: build-pipeline-trace.bat "repo-root" "project-root" "output-dir" "screen-rel-path-no-ext"
echo Example: build-pipeline-trace.bat "C:\work\converter" "C:\work\xplatform" "C:\work\output" "Form\stt\STT00030"
exit /b 2

:fail_cd
echo FAIL: cannot cd into repo root: %REPO%
exit /b 1

:fail_build
echo FAIL: javac compile failed
exit /b 1

:fail_convert
echo FAIL: conversion failed, see %OUTPUT%\convert.log
exit /b 1

:fail_missing
echo FAIL: generated file not found: %STAGE_A_PATH%
exit /b 1
