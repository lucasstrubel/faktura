<#
.SYNOPSIS
  Baut die PDF-Fassungen der Projektdokumentation nach dokumentation/pdf/.

.DESCRIPTION
  Spezifikationen, Testdokumente, Projektübersicht und Fallstudie werden mit
  pandoc und XeLaTeX gesetzt (gemeinsames Layout in tools/pandoc/), die
  Abschlusspräsentation mit Marp. Optional werden vorher die UML-Diagramme
  mit PlantUML neu gerendert.

  Voraussetzungen: pandoc 3.x, eine XeLaTeX-Distribution (z. B. MiKTeX),
  die Schriften Times New Roman, Arial und Consolas; für die Präsentation
  Node.js (npx) und ein Chromium-basierter Browser; für -Diagramme Java und
  tools/plantuml.jar.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools/dokumentation-bauen.ps1
  powershell -ExecutionPolicy Bypass -File tools/dokumentation-bauen.ps1 -Diagramme
#>
param(
    [switch]$Diagramme
)

$ErrorActionPreference = 'Stop'
$wurzel = Split-Path -Parent $PSScriptRoot
$doku = Join-Path $wurzel 'dokumentation'
$ziel = Join-Path $doku 'pdf'
$pandocDir = Join-Path $PSScriptRoot 'pandoc'
New-Item -ItemType Directory -Force $ziel | Out-Null

if ($Diagramme) {
    Write-Host 'Diagramme rendern (PlantUML, Smetana)…'
    & java '-Djava.awt.headless=true' -jar (Join-Path $PSScriptRoot 'plantuml.jar') `
        '-Playout=smetana' '-charset' 'UTF-8' (Join-Path $doku 'diagramme\*.puml')
    if ($LASTEXITCODE -ne 0) { throw 'PlantUML ist fehlgeschlagen.' }
}

$dokumente = @(
    'anforderungen\Lastenheft.md',
    'anforderungen\Pflichtenheft.md',
    'anforderungen\Anforderungsabgleich.md',
    'tests\Modultestplan.md',
    'tests\Modultestbericht.md',
    'projekt\Projektuebersicht.md',
    'projekt\Fallstudie.md'
)

foreach ($relativ in $dokumente) {
    $quelle = Join-Path $doku $relativ
    $name = [System.IO.Path]::GetFileNameWithoutExtension($quelle)
    $pdf = Join-Path $ziel "$name.pdf"
    Write-Host "pandoc: $relativ -> pdf\$name.pdf"
    # Aus dem Ordner des Dokuments heraus, damit relative Bildpfade (../diagramme) stimmen
    Push-Location (Split-Path -Parent $quelle)
    try {
        & pandoc $quelle `
            --defaults (Join-Path $pandocDir 'standard.yaml') `
            --lua-filter (Join-Path $pandocDir 'kopfzeile.lua') `
            --resource-path '.' `
            -o $pdf
        if ($LASTEXITCODE -ne 0) { throw "pandoc ist für $relativ fehlgeschlagen." }
    } finally {
        Pop-Location
    }
}

Write-Host 'Marp: projekt\Praesentation.md -> pdf\Praesentation.pdf'
& npx --yes '@marp-team/marp-cli@4' (Join-Path $doku 'projekt\Praesentation.md') `
    --pdf --html --allow-local-files -o (Join-Path $ziel 'Praesentation.pdf')
if ($LASTEXITCODE -ne 0) { throw 'Marp ist fehlgeschlagen.' }

Write-Host "Fertig: $ziel"
