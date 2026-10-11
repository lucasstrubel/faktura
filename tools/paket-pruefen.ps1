param(
    [Parameter(Mandatory=$true)][string]$Jar,
    [string]$Datenquelle,
    [int]$Laeufe = 3,
    [int]$GrenzeMs = 5000
)
$ErrorActionPreference = 'Stop'
$jarPfad = (Resolve-Path -LiteralPath $Jar).Path
$basis = Join-Path (Get-Location) ('target/paketpruefung/' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Force $basis | Out-Null
if ($IsWindows -or $env:OS -eq 'Windows_NT') { Get-ComputerInfo | Out-File (Join-Path $basis 'runner.txt') }
else { & uname -a | Out-File (Join-Path $basis 'runner.txt') }
$ErrorActionPreference = 'Continue'
& java -version 2>&1 | Out-File (Join-Path $basis 'java.txt')
$ErrorActionPreference = 'Stop'
$ergebnisse = @()
for ($lauf=1; $lauf -le $Laeufe; $lauf++) {
    $daten = Join-Path $basis ('daten-' + [guid]::NewGuid())
    New-Item -ItemType Directory $daten | Out-Null
    if ($Datenquelle) { Copy-Item -LiteralPath (Join-Path $Datenquelle 'faktura.db') -Destination $daten }
    $info = [System.Diagnostics.ProcessStartInfo]::new()
    $info.FileName = 'java'
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    # Fremde Entwicklungsvariablen dürfen die Referenzmessung nicht auf DEBUG umstellen.
    $info.EnvironmentVariables.Remove('DEBUG')
    if ($jarPfad.Contains('"') -or $daten.Contains('"')) { throw 'Anfuehrungszeichen im Testpfad unzulaessig' }
    $info.Arguments = '-Dfaktura.smoke=true -jar "' + $jarPfad + '" "--faktura.daten-verzeichnis=' + $daten + '"'
    $uhr = [Diagnostics.Stopwatch]::StartNew()
    $prozess = [Diagnostics.Process]::Start($info)
    $fehler = $prozess.StandardError.ReadToEndAsync()
    $zeilen = [Collections.Generic.List[string]]::new()
    $bereitMs = $null
    try {
        while ($true) {
            $lesen = $prozess.StandardOutput.ReadLineAsync()
            if (!$lesen.Wait(30000)) { throw 'Zeitüberschreitung beim Anwendungsstart' }
            $zeile = $lesen.Result
            if ($null -eq $zeile) { break }
            $zeilen.Add($zeile)
            if ($zeile.Contains('FAKTURA_READY')) { $bereitMs = $uhr.ElapsedMilliseconds }
        }
        if (!$prozess.WaitForExit(30000)) { throw 'Anwendung beendet den Smoke-Test nicht' }
        $zeilen | Set-Content (Join-Path $basis "start-$lauf.log")
        $fehler.Result | Set-Content (Join-Path $basis "start-$lauf.stderr.log")
        if ($prozess.ExitCode -ne 0 -or $null -eq $bereitMs) { throw 'Readiness-Marker fehlt oder Prozessfehler' }
        if (!(Test-Path -LiteralPath (Join-Path $daten 'faktura.db'))) { throw 'Isolierte Datenbank fehlt' }
        $ergebnisse += [pscustomobject]@{Lauf=$lauf; Millisekunden=$bereitMs; Grenze=$GrenzeMs; OS=[Environment]::OSVersion; Prozessoren=[Environment]::ProcessorCount}
        $ergebnisse | Export-Csv (Join-Path $basis 'startzeiten.csv') -NoTypeInformation
    } finally {
        if (!$prozess.HasExited) { $prozess.Kill() }
        $prozess.Dispose()
    }
}
if (@($ergebnisse | Where-Object Millisekunden -gt $GrenzeMs).Count -gt 0) { throw 'Startzeit überschreitet die Freigabegrenze' }
