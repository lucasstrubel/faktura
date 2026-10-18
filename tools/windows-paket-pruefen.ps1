<# Prüft ein portables Windows-Paket mit gebündelter Laufzeit, ohne Installation. #>
param([Parameter(Mandatory=$true)][string]$Jar)
$ErrorActionPreference = 'Stop'
if (!(Get-Command jpackage -ErrorAction SilentlyContinue)) { throw 'JDK 21 mit jpackage im PATH erforderlich; eine reine Java-Laufzeit reicht nicht' }
$jarPfad = (Resolve-Path -LiteralPath $Jar).Path
$basis = Join-Path (Get-Location) ('target/windows-paket-' + [guid]::NewGuid())
& (Join-Path $PSScriptRoot 'windows-paket-bauen.ps1') -Jar $jarPfad -Typ app-image -Ziel $basis
$paket = Join-Path $basis 'Faktura'
if (!(Test-Path -LiteralPath (Join-Path $paket 'runtime/bin/java.dll'))) { throw 'Gebündelte Laufzeit fehlt' }
if (!(Test-Path -LiteralPath (Join-Path $paket 'app/lib'))) { throw 'Entpacktes Layout fehlt (app/lib, Q-04)' }
$daten = Join-Path $basis 'pruefdaten'
$info = [Diagnostics.ProcessStartInfo]::new((Join-Path $paket 'Faktura.exe'))
$info.UseShellExecute = $false
$info.CreateNoWindow = $true
$info.EnvironmentVariables['JAVA_TOOL_OPTIONS'] = '-Dfaktura.smoke=true'
$info.EnvironmentVariables.Remove('JAVA_HOME')
$info.EnvironmentVariables.Remove('DEBUG')
$info.Arguments = '"--faktura.daten-verzeichnis=' + $daten + '"'
$app = [Diagnostics.Process]::Start($info)
try {
    if (!$app.WaitForExit(30000)) { throw 'Paket erreicht keine Bereitschaft innerhalb von 30 Sekunden' }
    if ($app.ExitCode -ne 0) { throw 'Gepackte Anwendung fehlgeschlagen' }
    if (!(Test-Path -LiteralPath (Join-Path $daten 'faktura.db'))) { throw 'Isolierte Datenbank fehlt' }
    if (!(Get-ChildItem -LiteralPath $daten -Recurse -Filter '*.log' | Select-String 'FAKTURA_READY')) { throw 'Bereitschaftsmarker fehlt' }
    if (Test-Path -LiteralPath (Join-Path $paket 'faktura.db')) { throw 'Datenbank im Programmverzeichnis' }
    @("PORT-02: Portables Windows-Paket erfolgreich gestartet", "JAR SHA256: $((Get-FileHash -LiteralPath $jarPfad -Algorithm SHA256).Hash)", "Betriebssystem: $([Environment]::OSVersion)", 'Gebündelte Laufzeit; JAVA_HOME entfernt; isolierte Daten; 30 Sekunden Smoke-Grenze.', 'Kein MSI-Test und kein Nachweis der 5-Sekunden-Startgrenze.') | Set-Content -LiteralPath (Join-Path $basis 'pruefbericht.txt')
    Set-Content -LiteralPath target/windows-paket-pfad.txt -Value $basis
    Write-Host "Geprüftes portables Paket: $paket"
} finally {
    if (!$app.HasExited) { $app.Kill() }
    $app.Dispose()
}
