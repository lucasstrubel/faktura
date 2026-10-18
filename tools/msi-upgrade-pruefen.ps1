<# MSI-UPG-01: Upgrade einer installierten Vorversion auf das neue MSI, nur auf einem
   disponiblen CI-Runner (installiert und entfernt Software im Benutzerkontext).

   Ablauf: Vorversion installieren und auf einem isolierten Datenverzeichnis starten,
   Prüfkunden in deren Datenbank anlegen, neues MSI darüber installieren. Erwartet wird
   genau eine Installation im neuen Layout, ein Start auf den alten Daten mit erhaltenem
   Kunden, bei Schemawechsel die Sicherung vor dem Upgrade (Q-06) und eine rückstandsfreie
   Deinstallation. #>
param(
    [Parameter(Mandatory=$true)][string]$AltMsi,
    [Parameter(Mandatory=$true)][string]$NeuMsi,
    # Projektversion des neuen Pakets, z. B. 3.1.0; bestimmt den erwarteten JAR-Namen
    [Parameter(Mandatory=$true)][string]$NeueVersion,
    # Vorversion vor der Installation entfernen, wie für Vorabversionen mit gleicher
    # MSI-Version dokumentiert (Windows Installer lehnt das Überinstallieren mit 1638 ab)
    [switch]$VorversionEntfernen
)
$ErrorActionPreference = 'Stop'
if (!$env:RUNNER_TEMP) { throw 'Nur auf einem disponiblen CI-Runner ausführen' }
$altPfad = (Resolve-Path -LiteralPath $AltMsi).Path
$neuPfad = (Resolve-Path -LiteralPath $NeuMsi).Path
$protokolle = Join-Path $env:RUNNER_TEMP 'msi-upgrade'
New-Item -ItemType Directory -Force $protokolle | Out-Null
$daten = Join-Path $env:RUNNER_TEMP ('faktura-upgrade-daten-' + [guid]::NewGuid())
$datenbank = Join-Path $daten 'faktura.db'
$uninstall = @('HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
               'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*')

function Faktura-Eintraege {
    @(Get-ItemProperty -Path $uninstall -ErrorAction SilentlyContinue | Where-Object DisplayName -eq 'Faktura')
}

function Msiexec([string[]]$argumente, [string]$protokoll) {
    $prozess = Start-Process msiexec.exe -ArgumentList ($argumente + @('/qn', '/l*v', "`"$protokoll`"")) -PassThru -Wait -WindowStyle Hidden
    return $prozess.ExitCode
}

function Installationsort {
    $eintraege = Faktura-Eintraege
    if ($eintraege.Count -ne 1) {
        $liste = ($eintraege | ForEach-Object { "$($_.DisplayVersion) $($_.PSChildName)" }) -join ', '
        throw "Erwartet genau eine Faktura-Installation, gefunden: $($eintraege.Count) ($liste)"
    }
    $ort = $eintraege[0].InstallLocation
    if (!$ort -or !(Test-Path -LiteralPath (Join-Path $ort 'Faktura.exe'))) { throw "Installationsort ungültig: '$ort'" }
    return $ort
}

# Versionen mit Smoke-Modus (ab 3.1.0-rc.1) beenden sich nach dem Bereitschaftsmarker
# selbst; ältere werden nach Erscheinen des Hauptfensters regulär geschlossen.
function Starte([string]$ort) {
    $info = [Diagnostics.ProcessStartInfo]::new((Join-Path $ort 'Faktura.exe'))
    $info.UseShellExecute = $false
    $info.Environment['JAVA_TOOL_OPTIONS'] = '-Dfaktura.smoke=true'
    $info.Environment.Remove('JAVA_HOME') | Out-Null
    $info.ArgumentList.Add('--faktura.daten-verzeichnis=' + $daten)
    $starter = [Diagnostics.Process]::Start($info)
    $uhr = [Diagnostics.Stopwatch]::StartNew()
    $fenster = $null
    $fensterSeit = 0
    while (!$starter.HasExited) {
        if ($uhr.Elapsed.TotalSeconds -gt 90) { $starter.Kill($true); throw 'Anwendung erreicht kein Hauptfenster' }
        # Der jpackage-Starter führt die Anwendung in einem Kindprozess aus
        $kind = Get-CimInstance Win32_Process -Filter "ParentProcessId=$($starter.Id)" |
            ForEach-Object { Get-Process -Id $_.ProcessId -ErrorAction SilentlyContinue } |
            Where-Object MainWindowTitle -eq 'Faktura' | Select-Object -First 1
        if ($kind -and !$fenster) { $fenster = $kind; $fensterSeit = $uhr.Elapsed.TotalSeconds }
        if ($fenster -and $uhr.Elapsed.TotalSeconds - $fensterSeit -gt 10) {
            # Vorversion ohne Smoke-Modus: wie ein Klick auf das Schließen-Kreuz
            $fenster.CloseMainWindow() | Out-Null
            if (!$starter.WaitForExit(20000)) { $starter.Kill($true); throw 'Vorversion lässt sich nicht schließen' }
        }
        Start-Sleep -Milliseconds 500
    }
    $starter.WaitForExit()
    return $starter.ExitCode
}

function Bereitschaftsmarker {
    @(Get-ChildItem -LiteralPath $daten -Filter '*.log' -ErrorAction SilentlyContinue |
        Select-String 'FAKTURA_READY').Count
}

# Prüfkunde und Schemastand über Python/sqlite3 (auf gehosteten Windows-Runnern vorhanden)
$sqlSkript = Join-Path $protokolle 'datenbank.py'
@'
import sqlite3, sys
db = sqlite3.connect(sys.argv[1])
if sys.argv[2] == "anlegen":
    db.execute("INSERT INTO kunde (kundennummer, name, strasse, plz, ort)"
               " VALUES ('K-900001', 'Upgrade-Probe GmbH', 'Teststr. 1', '68163', 'Mannheim')")
    db.commit()
kunden = db.execute("SELECT COUNT(*) FROM kunde WHERE kundennummer = 'K-900001'").fetchone()[0]
schema = db.execute("SELECT MAX(CAST(version AS INTEGER)) FROM flyway_schema_history WHERE success = 1").fetchone()[0]
db.close()
print(f"{kunden} {schema}")
'@ | Set-Content -LiteralPath $sqlSkript -Encoding utf8
function Datenbank([string]$aktion) {
    $ausgabe = & python -I $sqlSkript $datenbank $aktion
    if ($LASTEXITCODE -ne 0) { throw "Datenbankprüfung ($aktion) fehlgeschlagen" }
    $teile = "$ausgabe".Trim().Split(' ')
    return [pscustomobject]@{ Kunden = [int]$teile[0]; Schema = [int]$teile[1] }
}

try {
    if ((Faktura-Eintraege).Count -ne 0) { throw 'Auf dem Runner ist bereits Faktura installiert' }

    # 1. Vorversion installieren, starten, Prüfkunden anlegen
    $code = Msiexec @('/i', "`"$altPfad`"") (Join-Path $protokolle 'vorversion-installieren.log')
    if ($code -notin @(0, 3010)) { throw "Installation der Vorversion fehlgeschlagen: $code" }
    $ort = Installationsort
    $altVersion = (Faktura-Eintraege)[0].DisplayVersion
    $code = Starte $ort
    if ($code -ne 0) { throw "Vorversion beendet sich mit Exitcode $code" }
    if (!(Test-Path -LiteralPath $datenbank)) { throw 'Vorversion hat keine Datenbank angelegt' }
    $vorher = Datenbank 'anlegen'
    if ($vorher.Kunden -ne 1) { throw 'Prüfkunde konnte nicht angelegt werden' }
    $erstellt = (Get-Item -LiteralPath $datenbank).CreationTimeUtc
    Write-Host "Vorversion $altVersion installiert unter $ort; Schema V$($vorher.Schema), Prüfkunde angelegt"

    # 2. Neues MSI darüber installieren (bzw. nach Entfernen der Vorversion)
    if ($VorversionEntfernen) {
        $code = Msiexec @('/x', "`"$altPfad`"") (Join-Path $protokolle 'vorversion-entfernen.log')
        if ($code -notin @(0, 3010)) { throw "Entfernen der Vorversion fehlgeschlagen: $code" }
        if ((Faktura-Eintraege).Count -ne 0) { throw 'Vorversion nach dem Entfernen noch eingetragen' }
        if (!(Test-Path -LiteralPath $datenbank)) { throw 'Daten beim Entfernen der Vorversion verloren' }
        Write-Host "Vorversion $altVersion entfernt; Datenverzeichnis unverändert vorhanden"
    }
    $code = Msiexec @('/i', "`"$neuPfad`"") (Join-Path $protokolle 'upgrade.log')
    if ($code -notin @(0, 3010)) {
        # 1638: Windows Installer hält die Vorversion für dasselbe Produkt in anderer Fassung
        throw "Upgrade von $altVersion fehlgeschlagen: msiexec $code (Protokoll: upgrade.log)"
    }

    # 3. Genau eine Installation im neuen Layout
    $ort = Installationsort
    $eintrag = (Faktura-Eintraege)[0]
    $app = Join-Path $ort 'app'
    $jars = @(Get-ChildItem -LiteralPath $app -Filter 'faktura-*.jar' | ForEach-Object Name)
    if ($jars.Count -ne 1 -or $jars[0] -ne "faktura-$NeueVersion.jar") { throw "Unerwartete Anwendungs-JARs nach dem Upgrade: $($jars -join ', ')" }
    if (!(Test-Path -LiteralPath (Join-Path $app 'lib'))) { throw 'Entpacktes Layout fehlt nach dem Upgrade (app/lib)' }

    # 4. Neue Version startet auf den alten Daten. Das Protokoll wird fortgeschrieben und
    # enthält bei Vorversionen ab 3.1.0-rc.1 schon deren Marker: Gezählt wird nur ein neuer.
    $markerVorher = Bereitschaftsmarker
    $code = Starte $ort
    if ($code -ne 0) { throw "Neue Version beendet sich mit Exitcode $code" }
    if ((Bereitschaftsmarker) -le $markerVorher) { throw 'Bereitschaftsmarker der neuen Version fehlt' }
    if ((Get-Item -LiteralPath $datenbank).CreationTimeUtc -ne $erstellt) { throw 'Datenbank wurde neu angelegt statt übernommen' }
    $nachher = Datenbank 'lesen'
    if ($nachher.Kunden -ne 1) { throw 'Prüfkunde nach dem Upgrade verloren' }
    if ($nachher.Schema -lt $vorher.Schema) { throw "Schemastand gesunken: V$($vorher.Schema) -> V$($nachher.Schema)" }
    $sicherungen = @(Get-ChildItem -LiteralPath (Join-Path $daten 'sicherungen') -Filter 'vor-upgrade-*.zip' -ErrorAction SilentlyContinue)
    if ($nachher.Schema -gt $vorher.Schema -and $sicherungen.Count -eq 0) { throw 'Sicherung vor dem Schema-Upgrade fehlt (Q-06)' }
    Write-Host "Upgrade $altVersion -> $($eintrag.DisplayVersion): eine Installation, Schema V$($vorher.Schema) -> V$($nachher.Schema), Prüfkunde erhalten, Sicherungen: $($sicherungen.Count)"

    # 5. Rückstandsfreie Deinstallation
    $code = Msiexec @('/x', "`"$neuPfad`"") (Join-Path $protokolle 'deinstallieren.log')
    if ($code -notin @(0, 3010)) { throw "Deinstallation fehlgeschlagen: $code" }
    if ((Faktura-Eintraege).Count -ne 0) { throw 'Nach der Deinstallation ist noch ein Faktura-Eintrag vorhanden' }
    if (Test-Path -LiteralPath (Join-Path $ort 'Faktura.exe')) { throw 'Anwendung nach Deinstallation vorhanden' }
    Write-Host 'MSI-UPG-01 bestanden'
} finally {
    # Der Runner ist disponibel; trotzdem keine Installationen für Folgeschritte zurücklassen
    foreach ($rest in Faktura-Eintraege) {
        Msiexec @('/x', $rest.PSChildName) (Join-Path $protokolle "aufraeumen-$($rest.PSChildName).log") | Out-Null
    }
}
