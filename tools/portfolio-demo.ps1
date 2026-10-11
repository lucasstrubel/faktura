<# Erstellt bei jedem Aufruf einen neuen Demobestand; Standarddaten bleiben unberührt. #>
param([switch]$Starten)
$ErrorActionPreference = 'Stop'
$wurzel = Split-Path -Parent $PSScriptRoot
Push-Location $wurzel
try {
    & ./mvnw.cmd --batch-mode --no-transfer-progress test-compile
    if ($LASTEXITCODE -ne 0) { throw 'Demoklassen konnten nicht kompiliert werden' }
    $version = ([xml](Get-Content pom.xml)).project.version
    $jar = Join-Path $wurzel "target/faktura-$version.jar"
    if (!(Test-Path -LiteralPath $jar)) { throw 'Zuerst ./mvnw.cmd verify ausführen' }
    $basis = Join-Path $wurzel ('target/portfolio-demo-' + [guid]::NewGuid())
    & java '-Dloader.main=de.lucasstrubel.faktura.PortfolioDemodaten' '-Dloader.path=target/test-classes' "-Dfaktura.daten-verzeichnis=$basis/daten" -cp $jar org.springframework.boot.loader.launch.PropertiesLauncher $basis
    if ($LASTEXITCODE -ne 0) { throw 'Demovorbereitung fehlgeschlagen' }
    Set-Content -LiteralPath target/portfolio-demo-pfad.txt -Value $basis
    Write-Host "Demobestand und Beispielausgaben: $basis"
    if ($Starten) {
        & java -jar $jar "--faktura.daten-verzeichnis=$basis/daten"
        if ($LASTEXITCODE -ne 0) { throw 'Demo-Anwendung wurde mit einem Fehler beendet' }
    }
} finally { Pop-Location }
