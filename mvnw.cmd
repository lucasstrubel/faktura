@echo off
rem ---------------------------------------------------------------------------
rem Maven-Bootstrap-Skript (Windows): laedt Apache Maven beim ersten Aufruf
rem nach %USERPROFILE%\.m2\wrapper und ruft es anschliessend mit allen
rem uebergebenen Argumenten auf. Es ist keine lokale Maven-Installation noetig.
rem ---------------------------------------------------------------------------
setlocal

set "MAVEN_VERSION=3.9.9"
set "WRAPPER_DIR=%USERPROFILE%\.m2\wrapper\dists"
set "MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto run

echo Lade Apache Maven %MAVEN_VERSION% herunter (einmalig)...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;" ^
  "New-Item -ItemType Directory -Force '%WRAPPER_DIR%' | Out-Null;" ^
  "$zip = Join-Path '%WRAPPER_DIR%' 'apache-maven-%MAVEN_VERSION%-bin.zip';" ^
  "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip;" ^
  "Expand-Archive -Path $zip -DestinationPath '%WRAPPER_DIR%' -Force;" ^
  "Remove-Item $zip"
if errorlevel 1 (
  echo FEHLER: Maven konnte nicht heruntergeladen werden.
  exit /b 1
)

:run
"%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
