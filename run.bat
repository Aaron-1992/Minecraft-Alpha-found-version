@echo off
cd /d "%~dp0"
set "APP_JAR=alpha-desktop-0.5.1.jar"
if not exist "%APP_JAR%" set "APP_JAR=target\alpha-desktop-0.5.1.jar"
if not exist "%APP_JAR%" (
  echo Download and extract the desktop release, or run build.bat first.
  exit /b 1
)
java -jar "%APP_JAR%" %*
exit /b %errorlevel%
