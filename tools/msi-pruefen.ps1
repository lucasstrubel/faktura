param([Parameter(Mandatory=$true)][string]$Msi)
$ErrorActionPreference = 'Stop'
if (!$env:RUNNER_TEMP) { throw 'Nur auf einem disponiblen CI-Runner ausführen' }
$paket = (Resolve-Path -LiteralPath $Msi).Path
$installation = Join-Path $env:RUNNER_TEMP 'faktura-installiert'
$daten = Join-Path $env:RUNNER_TEMP ('faktura-msi-daten-' + [guid]::NewGuid())
$installiert = $false
try {
    $prozess = Start-Process msiexec.exe -ArgumentList @('/i', "`"$paket`"", '/qn', "INSTALLDIR=`"$installation`"") -PassThru -Wait -WindowStyle Hidden
    if ($prozess.ExitCode -notin @(0,3010)) { throw "Installation fehlgeschlagen: $($prozess.ExitCode)" }
    $installiert = $true
    if (!(Test-Path -LiteralPath (Join-Path $installation 'runtime/bin/java.dll'))) { throw 'Gebündelte Java-Laufzeit fehlt' }
    $info = [Diagnostics.ProcessStartInfo]::new((Join-Path $installation 'Faktura.exe'))
    $info.UseShellExecute = $false
    $info.Environment['JAVA_TOOL_OPTIONS'] = '-Dfaktura.smoke=true'
    $info.Environment.Remove('JAVA_HOME') | Out-Null
    $info.ArgumentList.Add('--faktura.daten-verzeichnis=' + $daten)
    $app = [Diagnostics.Process]::Start($info)
    if (!$app.WaitForExit(30000)) { $app.Kill($true); throw 'Installierte Anwendung erreicht keine Bereitschaft' }
    if ($app.ExitCode -ne 0) { throw 'Installierte Anwendung fehlgeschlagen' }
    if (!(Test-Path -LiteralPath (Join-Path $daten 'faktura.db'))) { throw 'Isolierte Datenbank fehlt' }
    $logs = Get-ChildItem -LiteralPath $daten -Recurse -Filter '*.log'
    if (!($logs | Select-String 'FAKTURA_READY')) { throw 'Readiness-Marker der installierten Anwendung fehlt' }
    if (Test-Path -LiteralPath (Join-Path $installation 'faktura.db')) { throw 'Daten im Installationsverzeichnis' }
} finally {
    if ($installiert) {
        $prozess = Start-Process msiexec.exe -ArgumentList @('/x', "`"$paket`"", '/qn') -PassThru -Wait -WindowStyle Hidden
        if ($prozess.ExitCode -notin @(0,3010)) { throw 'Deinstallation fehlgeschlagen' }
        if (Test-Path -LiteralPath (Join-Path $installation 'Faktura.exe')) { throw 'Anwendung nach Deinstallation vorhanden' }
    }
}
