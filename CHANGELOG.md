# Änderungsprotokoll

Alle nennenswerten Änderungen an Faktura. Das Format folgt
[Keep a Changelog](https://keepachangelog.com/de/1.1.0/), die Versionierung
[Semantic Versioning](https://semver.org/lang/de/).

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
