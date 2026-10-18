# Änderungsprotokoll

Alle nennenswerten Änderungen an Faktura. Das Format folgt
[Keep a Changelog](https://keepachangelog.com/de/1.1.0/), die Versionierung
[Semantic Versioning](https://semver.org/lang/de/).

## [Unveröffentlicht]

- **Startzeit Q-04 eingehalten:** Mit dem Referenzbestand (5.000 Kunden, 5.000 Produkte,
  1.000 Belege) startet das Windows-Paket mit gebündelter Java-21-Laufzeit in 3,5–3,7 s
  statt 4,5–5,8 s (Modultestbericht Kapitel 9).
  - Der Installer enthält das geprüfte JAR im entpackten Spring-Boot-Layout statt des
    Fat-JARs; das Paket baut `tools/windows-paket-bauen.ps1` für Release und Prüfläufe.
  - Steht der Bestand auf der ausgelieferten Schemaversion, startet die Anwendung ohne
    Flyway; ausstehende, fehlgeschlagene oder neuere Migrationen laufen wie bisher über
    Flyway samt Upgradeprobe.
  - Die Übersicht wertet den Belegbestand im Hintergrund aus und zeigt bis dahin
    Platzhalterkarten.
- Jeder Start protokolliert seine Phasen (`FAKTURA_STARTPHASEN`); `tools/paket-pruefen.ps1`
  misst wahlweise das Windows-Paket (`-Starter`) und meldet den Exitcode bei Fehlern.
- GitHub Actions auf aktuelle Hauptversionen (Node.js 24) angehoben; Dependabot hält sie
  künftig wöchentlich aktuell.

## [3.1.0-rc.1] — 2026-10-11 (Vorabversion)

- Preise mit eindeutigem deutschem Editorformat; strenge Kalenderprüfung.
- Persistente Betragssnapshots und historische Steuerrekonstruktion ohne Änderung alter Summen.
- Einheiten-Snapshots mit Presets (neu: Monat/MON, Pauschale/LS) und freien PDF-Einheiten; Produkte ohne
  Einheit ergeben Positionen mit Stück/C62 wie bisher; unbekannte historische Einheiten sperren XML.
- EN-16931-CII-Prüfung mit Mustang 2.24.0, Betragsabgleich und atomarer Veröffentlichung.
- Gemessene PDF-Spalten mit gestapeltem Ausweichlayout und getrennten Summenspalten.
- Erweiterte CSV-Ausgabe und versionierter Gesamtexport aus einer Lesetransaktion.
- Typisierte Tabellensortierung, Schutz ungespeicherter Einstellungen beim Beenden.
- Atomare geprüfte ZIP-Sicherung; Upgradeprüfung auf wiederhergestellter Sicherung.
- Gebündelte Belegabfragen, gezielte Referenzabfragen, SQLite-Leistungsmessungen.
- Prüfsummen im Maven-Bootstrap, engere SpotBugs-Ausnahmen und zusätzliche Release-Prüfungen.
- Dokumentation benennt Kontrollen und offene Nachweise; XRechnung gehört nicht zum Umfang.
- Das Fat-JAR wird durch den Mustang-Validator (Schema, Schematron, Saxon) deutlich größer.
- Sicherheitsupdates nach dem ersten vollständigen Abhängigkeitsscan: Spring Boot 4.1.1
  (Spring Framework 7.0.9, Jackson 2.21.5, Log4j 2.25.5), PDFBox 3.0.8 für alle PDFBox-Module und
  Rhino 1.7.15.1 (über den Mustang-Validator).
- Maven-Wrapper (Windows) prüft die Prüfsumme ohne `Get-FileHash`, damit der erste Download auch
  aus PowerShell 7 heraus gelingt.
- Review-Korrekturen: Rückfrage beim Beenden auch außerhalb der Einstellungen, erneut gedruckte
  Alt-PDFs ohne Hinweistext, Positionen nicht über Seitenwechsel getrennt, Aufräumfehler blockieren
  den Start nicht mehr, V6-Migration ohne Fachklassen eingefroren und im Paket geprüft,
  Exportversion aus dem Build.

**Bekannte Lücke:** Q-04 (Start in höchstens 5 Sekunden mit 5.000 Kunden/Produkten) wird
nicht zuverlässig eingehalten; gemessen 4,5–5,3 s auf dem Entwicklungsrechner und bis 6,6 s
auf langsameren CI-Runnern. Der Release-Workflow prüft die Startzeit deshalb nur noch als
Regressionswächter (8 s). Diese Version erscheint als Vorabversion; 3.1.0 bleibt gesperrt,
bis die Startzeit eingehalten wird.

### Portfolio-Aufbereitung (Teil von 3.1.0-rc.1)

- Usability ausdrücklich zurückgestellt; spätere Produktfunktionen vom Portfolio-Abschluss getrennt.
- Dokumentationswidersprüche zu Download, Stornierung und Nachweisen korrigiert; KI-Unterstützung ergänzt.
- Isolierte Demodaten, Drei-Minuten-Drehbuch und Prüfung eines portablen Windows-Pakets ergänzt.
- Separater manueller Windows-/MSI-Workflow ohne Veröffentlichung vorbereitet.

## [3.0.0] — 2026-09-24

Erste öffentliche Veröffentlichung mit Installer. Enthält die Überarbeitung vom Juli 2026
und den Abschluss vom September 2026.

### Hinzugefügt

- Zahlungseingang erfassen; bezahlte Rechnungen zählen nicht mehr als offen oder überfällig
- Stornorechnung für versendete Rechnungen (negative Mengen, eigene Rechnungsnummer,
  E-Rechnung als korrigierte Rechnung, Dokumentart 384)
- Steuernummer im Firmenprofil; Rechnungen verlangen Steuernummer oder USt-IdNr.
- Aussteller-Snapshot je Beleg: spätere Änderungen des Firmenprofils verändern bestehende
  Belege nicht mehr
- Umsatzsteuer je Steuersatz im PDF (§ 14 Abs. 4 Nr. 7/8 UStG)
- Leistungsdatum im Rechnungsassistenten; Rechnungen aus Lieferscheinen übernehmen das
  Lieferdatum
- Übersicht mit Kennzahlen, Seitennavigation, Master-Detail-Ansicht der Belege,
  Dunkelmodus, Tastenkürzel, nicht blockierende Erfolgsmeldungen
- Plattformübergreifendes JAR (Windows, Linux, macOS mit Apple Silicon)

### Geändert

- Rechnungsnummern werden in derselben Transaktion vergeben, in der der Beleg gespeichert
  wird — ein fehlgeschlagener Speichervorgang verbraucht keine Nummer mehr
- Umsatzsteuer wird je Steuersatz statt je Position gerundet; PDF und E-Rechnung weisen
  dieselben Beträge aus
- PDF mit eingebetteter Schrift (Liberation Sans) und Zeilenumbruch nach Breite
- Datenverzeichnis standardmäßig `<Benutzerverzeichnis>/Faktura/daten` statt relativ zum
  Startverzeichnis
- Längere Vorgänge (PDF, E-Rechnung, CSV, Datensicherung, Druck, Mail) laufen im Hintergrund
- Datensicherung über `VACUUM INTO` statt Dateikopie
- Prüfung von USt-IdNr. aller EU-Staaten, IBAN-Prüfsumme, BIC und Steuernummer;
  gespeicherte Werte in einheitlicher Schreibweise
- CSV-Export mit UTF-8-BOM (Excel) und Schutz vor Formel-Injektion
- Abdeckungsschwelle im Build auf 85 % Anweisungen / 70 % Zweige angehoben
- Dokumentation vollständig auf Deutsch und zusätzlich als PDF

### Behoben

- PDF-Export brach bei Namen wie „Łukasz“ oder „Kovács Ödön“ ab
- Enter im Suchfeld öffnete den Dialog „Neu …“; Tastenkürzel wirkten nach dem
  Ansichtswechsel in der falschen Ansicht weiter
- „1.500“ wurde als 1,50 € statt 1.500 € gespeichert; getippte Daten wie „1.11.26“ wurden
  stillschweigend verworfen
- „Alle Belege als CSV“ war ohne ausgewählten Beleg gesperrt; das Kontextmenü ignorierte
  den Belegstatus
- Eine fehlerhafte JSON-Übernahme hinterließ einen halb übernommenen Bestand
- Anschriften mit Komma in der Straße verschoben die PLZ in der E-Rechnung
- Überlappende Hintergrundvorgänge meldeten „Bereit“, obwohl noch einer lief

## [2.0.0] — 2026-07-19

Weiterentwicklung als Einzelprojekt.

### Hinzugefügt

- Spring Boot als IoC-Container, SQLite mit Flyway-Migrationen, einmalige Übernahme des
  JSON-Bestands
- Vollständig neue JavaFX-Oberfläche (FXML, AtlantaFX) mit Live-Validierung
- Firmenprofil, E-Rechnung nach EN 16931, Datensicherung als ZIP
- Windows-Installer (jpackage) und Release-Workflow
- Continuous Integration mit JaCoCo und SpotBugs, strengere Eingabevalidierung, Logging

## [1.0.0] — 2026-06-24

Abnahme des Hochschulprojekts im Modul Software Engineering 1 (TH Mannheim): Kunden- und
Produktverwaltung, Dokumentenzyklus mit PDF-Export, geführte Rechnungserstellung,
Swing-Oberfläche, JSON-Ablage; 71 von 71 Modultestfällen bestanden.

[3.0.0]: https://github.com/lucasstrubel/faktura/releases/tag/v3.0.0
