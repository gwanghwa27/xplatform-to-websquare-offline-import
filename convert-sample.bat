@echo off
setlocal
chcp 65001 >nul

rem Compiles (if needed) and converts sample-phase3-project\ into build\sample-output\.
rem The reference output under sample-phase3-output\ is never overwritten by this script.

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"
set "CLASSES_DIR=%PROJECT_ROOT%build\classes"
set "INPUT_DIR=%PROJECT_ROOT%sample-phase3-project"
set "OUTPUT_DIR=%PROJECT_ROOT%build\sample-output"

echo == XPlatform to WebSquare Converter - Sample Conversion ==

if not exist "%CLASSES_DIR%" (
  echo build\classes not found -- running build.bat first.
  call "%SCRIPT_DIR%build.bat"
  if errorlevel 1 exit /b 1
)

if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"

echo Input:  %INPUT_DIR%
echo Output: %OUTPUT_DIR%
echo Reference (not overwritten): %PROJECT_ROOT%sample-phase3-output

java -Dfile.encoding=UTF-8 -cp "%CLASSES_DIR%" com.example.xfdltracker.project.XPlatformProjectConverter "%INPUT_DIR%" "%OUTPUT_DIR%" "UTF-8"
if errorlevel 1 (
  echo [CONVERT_FAIL] converter exited with a non-zero status.
  exit /b 1
)

echo [CONVERT_DONE] See %OUTPUT_DIR%\conversion-report for the success/failure summary line.
echo Expected: 149/149.
exit /b 0
