<# Baut aus dem geprüften Fat-JAR ein Windows-Paket mit gebündelter Laufzeit (MSI oder portables app-image).

   Startzeit (Q-04): Das Paket enthält das JAR im entpackten Spring-Boot-Layout
   (Anwendungs-JAR + lib/) statt der verschachtelten Fat-JAR-Struktur; Klassen
   laden dann direkt aus gewöhnlichen JARs. Gemessen auf dem Referenzrechner
   spart das gegenüber 3.1.0-rc.1 rund 0,9 Sekunden je Start. CDS-Archive
   bringen mit Java 21 hier nichts Verwertbares: Ein Anwendungsarchiv bindet sich
   an den absoluten Installationspfad, das Archiv der JDK-Klassen spart nicht
   messbar (Modultestbericht 8.3). #>
param(
    [Parameter(Mandatory=$true)][string]$Jar,
    [Parameter(Mandatory=$true)][ValidateSet('msi', 'app-image')][string]$Typ,
    [Parameter(Mandatory=$true)][string]$Ziel
)
$ErrorActionPreference = 'Stop'
if (!(Get-Command jpackage -ErrorAction SilentlyContinue)) { throw 'JDK 21 mit jpackage im PATH erforderlich; eine reine Java-Laufzeit reicht nicht' }
$jarPfad = (Resolve-Path -LiteralPath $Jar).Path
$eingabe = Join-Path ([IO.Path]::GetTempPath()) ('faktura-paketeingabe-' + [guid]::NewGuid())
try {
    & java -Djarmode=tools -jar $jarPfad extract --destination $eingabe
    if ($LASTEXITCODE -ne 0) { throw 'Fat-JAR konnte nicht entpackt werden' }
    $hauptJar = [IO.Path]::GetFileName($jarPfad)
    if (!(Test-Path -LiteralPath (Join-Path $eingabe $hauptJar)) -or !(Test-Path -LiteralPath (Join-Path $eingabe 'lib'))) {
        throw 'Entpacktes Layout unvollständig (Anwendungs-JAR oder lib/ fehlt)'
    }

    # jpackage verlangt eine rein numerische Version; ein RC (3.1.0-rc.1) wird zu "3.1.0"
    $version = ([xml](Get-Content pom.xml)).project.version -replace '-.*$', ''
    $argumente = @(
        '--type', $Typ, '--name', 'Faktura', '--app-version', $version,
        '--vendor', 'Lucas Strubel',
        '--input', $eingabe, '--main-jar', $hauptJar,
        '--icon', 'installer/faktura.ico',
        '--dest', $Ziel
    )
    if ($Typ -eq 'msi') {
        $argumente += @('--win-menu', '--win-shortcut', '--win-per-user-install',
            '--win-upgrade-uuid', '6ff17f18-0f83-4f66-9b3b-2f5c1a4de0aa')
    }
    & jpackage @argumente
    if ($LASTEXITCODE -ne 0) { throw "jpackage ($Typ) fehlgeschlagen" }
} finally {
    if (Test-Path -LiteralPath $eingabe) { Remove-Item -LiteralPath $eingabe -Recurse -Force }
}
