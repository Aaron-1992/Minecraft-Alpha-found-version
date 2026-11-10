@echo off
cd /d "%~dp0"
if not exist .test-classes mkdir .test-classes
javac -encoding UTF-8 --release 17 -d .test-classes src\main\java\*.java tests\java\*.java
if errorlevel 1 exit /b %errorlevel%
java -Djava.awt.headless=true -cp .test-classes DesktopTests
if errorlevel 1 exit /b %errorlevel%
java -Djava.awt.headless=true -cp .test-classes OriginalEngineTests
if errorlevel 1 exit /b %errorlevel%
java -Djava.awt.headless=true -cp .test-classes RecoveryTests
if errorlevel 1 exit /b %errorlevel%

java -Djava.awt.headless=true -cp .test-classes DirectLaunchTests
if errorlevel 1 exit /b %errorlevel%
java -Djava.awt.headless=true -cp .test-classes WorldBackupTests
if errorlevel 1 exit /b 1
exit /b %errorlevel%
