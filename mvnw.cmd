@echo off
rem ---------------------------------------------------------------------------
rem Maven-Bootstrap-Skript (Windows): laedt Apache Maven beim ersten Aufruf
rem nach %USERPROFILE%\.m2\wrapper und ruft es anschliessend mit allen
rem uebergebenen Argumenten auf. Es ist keine lokale Maven-Installation noetig.
rem ---------------------------------------------------------------------------
setlocal

set "MAVEN_VERSION=3.9.9"
set "MAVEN_SHA512=8beac8d11ef208f1e2a8df0682b9448a9a363d2ad13ca74af43705549e72e74c9378823bf689287801cbbfc2f6ea9596201d19ccacfdfb682ee8a2ff4c4418ba"
set "WRAPPER_DIR=%USERPROFILE%\.m2\wrapper\dists"
set "MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto run

echo Lade Apache Maven %MAVEN_VERSION% herunter (einmalig)...
rem Aus PowerShell 7 aufgerufen (z. B. GitHub-Runner) erbt Windows PowerShell 5.1 sonst
rem dessen PSModulePath und findet Cmdlets wie Get-FileHash nicht. Leer = Standardpfade.
set "PSModulePath="
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;" ^
  "New-Item -ItemType Directory -Force '%WRAPPER_DIR%' | Out-Null;" ^
  "$zip = Join-Path '%WRAPPER_DIR%' 'apache-maven-%MAVEN_VERSION%-bin.zip';" ^
  "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip;" ^
  "$sha = [Security.Cryptography.SHA512]::Create(); $strom = [IO.File]::OpenRead($zip);" ^
  "try { $ist = [BitConverter]::ToString($sha.ComputeHash($strom)).Replace('-', '') } finally { $strom.Dispose() };" ^
  "if ($ist -ne '%MAVEN_SHA512%') { Remove-Item -LiteralPath $zip; throw 'Maven-Pruefsumme stimmt nicht ueberein' };" ^
  "Expand-Archive -Path $zip -DestinationPath '%WRAPPER_DIR%' -Force;" ^
  "Remove-Item $zip"
if errorlevel 1 (
  echo FEHLER: Maven konnte nicht heruntergeladen werden.
  exit /b 1
)

:run
"%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
