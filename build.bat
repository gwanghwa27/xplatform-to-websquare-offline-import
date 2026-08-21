@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

rem Offline, dependency-free build: javac only, no Maven/Gradle, no external JAR.
rem Compiles Production Java source (src\main\java) into build\classes\.
rem Source tree itself is never written to (no .class files under src\).

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"
set "SRC_ROOT=%PROJECT_ROOT%src\main\java"
set "BUILD_DIR=%PROJECT_ROOT%build"
set "CLASSES_DIR=%BUILD_DIR%\classes"
set "SOURCES_LIST=%BUILD_DIR%\sources.txt"

echo == XPlatform to WebSquare Converter - Offline Build ==
echo Project root: %PROJECT_ROOT%

where java >nul 2>nul
if errorlevel 1 (
  echo [FAIL] java not found on PATH. Set JAVA_HOME to your JDK 1.8.0_111 install and add its bin\ to PATH.
  exit /b 1
)
where javac >nul 2>nul
if errorlevel 1 (
  echo [FAIL] javac not found on PATH. Set JAVA_HOME to your JDK 1.8.0_111 install and add its bin\ to PATH.
  exit /b 1
)

for /f "usebackq delims=" %%v in (`java -version 2^>^&1`) do (
  if not defined JAVA_VER_LINE set "JAVA_VER_LINE=%%v"
)
for /f "usebackq delims=" %%v in (`javac -version 2^>^&1`) do (
  if not defined JAVAC_VER_LINE set "JAVAC_VER_LINE=%%v"
)
echo java -version:  %JAVA_VER_LINE%
echo javac -version: %JAVAC_VER_LINE%

echo %JAVA_VER_LINE% | findstr /c:"1.8.0_111" >nul
if errorlevel 1 (
  echo [TARGET_JDK_MISMATCH_WARNING] Detected JDK is not exactly 1.8.0_111. Compile will still be attempted; this warning does NOT mean the build failed, and this build script does NOT certify TARGET_JDK_RUNTIME_VERIFIED. Use verify-offline.bat for the mandatory exact-JDK gate.
) else (
  echo [TARGET_JDK_MATCH] Detected exact JDK 1.8.0_111.
)

if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%CLASSES_DIR%"

set "COUNT=0"
for /f "delims=" %%f in ('dir /s /b "%SRC_ROOT%\*.java"') do (
  set "line=%%f"
  set "line=!line:\=/!"
  echo !line!>>"%SOURCES_LIST%"
  set /a COUNT+=1
)
echo Java source files: %COUNT%

javac -encoding UTF-8 -d "%CLASSES_DIR%" "@%SOURCES_LIST%"
if errorlevel 1 (
  echo [BUILD_FAIL] javac reported errors.
  exit /b 1
)

echo [BUILD_OK] Compiled %COUNT% source files into %CLASSES_DIR% (0 errors).
exit /b 0
