@echo off
cd /d "%~dp0"
if not exist .test-classes mkdir .test-classes
javac --release 17 -d .test-classes src\main\java\*.java tests\java\*.java
if errorlevel 1 exit /b %errorlevel%
java -Djava.awt.headless=true -cp .test-classes DesktopTests
exit /b %errorlevel%
