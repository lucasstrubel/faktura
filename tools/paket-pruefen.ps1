param(
    [string]$Jar,
    # Alternativ zum JAR: Starter einer jpackage-Installation (Faktura.exe) mit gebündelter Laufzeit
    [string]$Starter,
    [string]$Datenquelle,
    [int]$Laeufe = 3,
    [int]$GrenzeMs = 5000
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrEmpty($Jar) -eq [string]::IsNullOrEmpty($Starter)) { throw 'Genau einen der Parameter -Jar oder -Starter angeben' }
$programm = if ($Jar) { (Resolve-Path -LiteralPath $Jar).Path } else { (Resolve-Path -LiteralPath $Starter).Path }
$basis = Join-Path (Get-Location) ('target/paketpruefung/' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Force $basis | Out-Null
if ($IsWindows -or $env:OS -eq 'Windows_NT') { Get-ComputerInfo | Out-File (Join-Path $basis 'runner.txt') }
else { & uname -a | Out-File (Join-Path $basis 'runner.txt') }
if ($Jar) {
    $ErrorActionPreference = 'Continue'
    & java -version 2>&1 | Out-File (Join-Path $basis 'java.txt')
    $ErrorActionPreference = 'Stop'
} else {
    # Die gebündelte Laufzeit enthält kein java.exe; ihre Version steht in runtime/release
    Copy-Item -LiteralPath (Join-Path (Split-Path $programm) 'runtime/release') -Destination (Join-Path $basis 'java.txt')
}
$ergebnisse = @()
for ($lauf=1; $lauf -le $Laeufe; $lauf++) {
    $daten = Join-Path $basis ('daten-' + [guid]::NewGuid())
    New-Item -ItemType Directory $daten | Out-Null
    if ($Datenquelle) { Copy-Item -LiteralPath (Join-Path $Datenquelle 'faktura.db') -Destination $daten }
    $info = [System.Diagnostics.ProcessStartInfo]::new()
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    # Fremde Entwicklungsvariablen dürfen die Referenzmessung nicht auf DEBUG umstellen.
    $info.EnvironmentVariables.Remove('DEBUG')
    if ($programm.Contains('"') -or $daten.Contains('"')) { throw 'Anfuehrungszeichen im Testpfad unzulaessig' }
    if ($Jar) {
        $info.FileName = 'java'
        $info.Arguments = '-Dfaktura.smoke=true -jar "' + $programm + '" "--faktura.daten-verzeichnis=' + $daten + '"'
    } else {
        $info.FileName = $programm
        $info.Arguments = '"--faktura.daten-verzeichnis=' + $daten + '"'
        $info.EnvironmentVariables['JAVA_TOOL_OPTIONS'] = '-Dfaktura.smoke=true'
        $info.EnvironmentVariables.Remove('JAVA_HOME')
    }
    $uhr = [Diagnostics.Stopwatch]::StartNew()
    $prozess = [Diagnostics.Process]::Start($info)
    $fehler = $prozess.StandardError.ReadToEndAsync()
    $zeilen = [Collections.Generic.List[string]]::new()
    $bereitMs = $null
    $phasen = [ordered]@{}
    try {
        while ($true) {
            $lesen = $prozess.StandardOutput.ReadLineAsync()
            if (!$lesen.Wait(30000)) { throw 'Zeitüberschreitung beim Anwendungsstart' }
            $zeile = $lesen.Result
            if ($null -eq $zeile) { break }
            $zeilen.Add($zeile)
            if ($zeile.Contains('FAKTURA_READY')) { $bereitMs = $uhr.ElapsedMilliseconds }
            if ($zeile -match 'FAKTURA_STARTPHASEN (.*)$') {
                foreach ($paar in $Matches[1].Split(' ')) {
                    $teile = $paar.Split('=')
                    $phasen[$teile[0]] = [int]$teile[1]
                }
            }
        }
        if (!$prozess.WaitForExit(30000)) { throw 'Anwendung beendet den Smoke-Test nicht' }
        $zeilen | Set-Content (Join-Path $basis "start-$lauf.log")
        $fehler.Result | Set-Content (Join-Path $basis "start-$lauf.stderr.log")
        if ($null -eq $bereitMs) { throw "Readiness-Marker fehlt (Exitcode $($prozess.ExitCode))" }
        if ($prozess.ExitCode -ne 0) { throw "Prozessfehler nach dem Readiness-Marker: Exitcode $($prozess.ExitCode)" }
        if (!(Test-Path -LiteralPath (Join-Path $daten 'faktura.db'))) { throw 'Isolierte Datenbank fehlt' }
        $ergebnis = [ordered]@{Lauf=$lauf; Millisekunden=$bereitMs; Grenze=$GrenzeMs; OS=[Environment]::OSVersion; Prozessoren=[Environment]::ProcessorCount}
        # Phasen in ms seit Prozessstart, gemessen von der Anwendung selbst (Startphasen)
        foreach ($phase in $phasen.Keys) { $ergebnis["phase_$phase"] = $phasen[$phase] }
        $ergebnisse += [pscustomobject]$ergebnis
        $ergebnisse | Export-Csv (Join-Path $basis 'startzeiten.csv') -NoTypeInformation
    } finally {
        if (!$prozess.HasExited) { $prozess.Kill() }
        $prozess.Dispose()
    }
}
$ergebnisse | Format-Table -AutoSize | Out-String | Write-Host
if (@($ergebnisse | Where-Object Millisekunden -gt $GrenzeMs).Count -gt 0) { throw 'Startzeit überschreitet die Freigabegrenze' }
