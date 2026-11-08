@echo off
cd /d "%~dp0"
call mvn -f agent\pom.xml clean package
if errorlevel 1 exit /b %errorlevel%
call mvn clean package
exit /b %errorlevel%
