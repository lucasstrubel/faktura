# Betrieb und Upgrade auf 3.1.0

Gilt für die endgültige Version 3.1.0; die Abschnitte zu Belegen, Einheiten, Export und
Sicherung entstanden mit der Vorabversion 3.1.0-rc.1 und gelten unverändert.

## Installation und Upgrade

- **Von 3.0.0:** Den 3.1.0-Installer einfach ausführen; er ersetzt die alte Installation.
  Beim ersten Start sichert Faktura den Bestand (`daten/sicherungen/vor-upgrade-<UUID>.zip`),
  prüft die Migration an einer Kopie und hebt das Schema dann auf V6 (siehe unten).
- **Von 3.1.0-rc.1:** Beide Installer tragen die Version 3.1.0; Windows lehnt das
  Überinstallieren ab („Eine andere Version dieses Produkts ist bereits installiert“,
  Fehler 1638). Die Vorabversion deshalb zuerst unter „Einstellungen › Apps“ deinstallieren,
  danach 3.1.0 installieren. Die Daten liegen im Benutzerordner (`Faktura/daten`) und bleiben
  beim Deinstallieren erhalten; das Schema ist bereits aktuell, eine Migration entfällt.
- Beide Wege prüft der Release-Workflow auf einem Windows-Runner mit angelegten Daten
  (MSI-UPG-01, Modultestbericht 9.8).

## Historische Belege

V1 bis V4 der Flyway-Migrationen bleiben unverändert. V5 ergänzt Berechnungsherkunft,
Steuerzeilen und Positionseinheiten; V6 rekonstruiert nachweisbare Steuerzeilen.
Gespeicherte Nettosumme, Steuer und Bruttosumme sowie Nummernkreise werden nicht geändert.
Ein leerer Geldwert aus älteren JSON-Dateien bleibt unbekannt (in den historischen
NOT-NULL-TEXT-Spalten als leerer Text abgelegt); er wird nicht als bekannte Null behandelt.
Alte Produktpreise werden nicht automatisch berichtigt. Hohe Preise können legitim sein.

Die Herkunft lautet `JE_STEUERSATZ`, `ALTE_POSITIONSRUNDUNG` oder `UNGEKLAERT`.
PDF zeigt gespeicherte Summen und belegbare Steuerzeilen. Bei ungeklärter Aufteilung
erscheinen die gespeicherte Gesamtsteuer und ein Hinweis; es wird keine Verteilung erfunden.
Statuswechsel, Zahlungseingänge und Exporte berechnen gespeicherte Summen nicht neu.
Stornorechnungen negieren den ursprünglichen Betragsstand und bleiben auch im Status
OFFEN unverändert. Unvollständige historische Summen müssen vor einer Stornierung
anhand der Originalunterlagen geklärt werden; das Programm erfindet keine Beträge.

## Einheiten und XML

Neue Produkte zeigen Stück ausdrücklich als Vorgabe. Presets: Stück/C62, Stunde/HUR,
Tag/DAY, Monat/MON, Pauschale/LS, Kilogramm/KGM, Gramm/GRM, Meter/MTR und Liter/LTR.
Aliase werden normalisiert. Produkte ohne Einheit erzeugen Positionen mit Stück/C62 — wie
vor 3.1, als jede Position als C62 exportiert wurde. Freie Einheiten ohne Code sind im PDF
nutzbar; beim Speichern des Produkts wird die XML-Einschränkung bestätigt. Positionen
speichern Label und Code unabhängig vom aktuellen Produkt. Alte Positionen erhalten keine
nachträglich abgeleitete Einheit; ihr PDF zeigt dann keine Einheit, ihre XML-Ausgabe bleibt
gesperrt. Folgebelege und Stornos übernehmen den Snapshot.

Implementiert ist EN 16931 CII XML, kein XRechnung-Profil. Mustang-Library und Validator
sind auf 2.24.0 abgestimmt; die Schema- und Schematron-Artefakte liegen im Paket.
Validierung erfolgt beim Export im Hintergrund. Externe XML-Entitäten werden abgelehnt.
Unbekannte Einheiten, historische Positionsrundung, ungeklärte Beträge, Normfehler oder
Abweichungen von Summen, Steuerzeilen, Mengen und Einheiten verhindern XML-Ausgabe.
PDF bleibt verfügbar. Ein vorhandenes XML-Ziel bleibt bei Fehlern bestehen.

## Datenexport

Die ersten 26 Dokument-CSV-Spalten bleiben in derselben Reihenfolge. Angehängt werden
elf Ausstellerfelder, Einheit, Einheitencode und Berechnungsart. `steuern.csv` enthält
die Aufschlüsselung je Beleg und Steuersatz; `firmenprofil.csv` das aktuelle Firmenprofil.
Beide Dateien lassen sich in den Einstellungen auch separat exportieren. Dabei zählt
das zuletzt gespeicherte Firmenprofil; ungespeicherte Formularänderungen bleiben unberücksichtigt.
„Alle Daten exportieren“ erzeugt Formatversion 1 als ZIP mit Kunden, Produkten,
Dokumenten, Steuern, Firmenprofil, Nummernkreisen und `FORMAT.txt` aus einer konsistenten
Lesetransaktion. UTF-8-BOM, Semikolon und Formelschutz bleiben erhalten.
Leere Werte sind unbekannt oder nicht gesetzt. Der CSV-Export ist keine Rücksicherung.

## Sicherung, Wiederherstellung und Rollback

Sicherungen verwenden `VACUUM INTO`. Das temporäre ZIP wird geschlossen, vollständig
gelesen und auf CRC/Größe geprüft. Erst danach ersetzt ein atomarer Dateitausch das Ziel.
Unterstützt das Dateisystem diesen Austausch nicht, bricht der Vorgang ab und erhält das
vorhandene Ziel. Datenbank, Sidecars und JSON-Quelldateien sind als Ziel unzulässig.

Vor einem ausstehenden Schema-Upgrade eines bestehenden Bestands schreibt die Anwendung
eine Sicherung unter `daten/sicherungen/vor-upgrade-<UUID>.zip`. Sie stellt diese in einem
temporären Verzeichnis wieder her, prüft SQLite-Integrität und führt die Migration dort
probeweise aus. Fehler stoppen den Start vor der eigentlichen Migration. Diese Sicherung
zusätzlich auf einen getrennten Datenträger kopieren und bis zur endgültigen Abnahme behalten.

Wiederherstellung:

1. Faktura vollständig beenden; keine zweite Instanz darf auf denselben Bestand zugreifen.
2. Den bisherigen Datenordner als Ganzes aufbewahren. Ein neues, leeres Zielverzeichnis anlegen.
3. `faktura.db` und gegebenenfalls JSON-Bestände aus der geprüften Sicherung dorthin entpacken.
   Keine alten `-wal`, `-shm` oder `-journal`-Dateien mit der restaurierten Datenbank mischen.
4. Mit `--faktura.daten-verzeichnis=<Ziel>` starten und Belege, Summen und Nummern prüfen.

Rollback: Die **vor dem Upgrade erstellte** Sicherung in einen leeren Ordner restaurieren
und mit der vorherigen Anwendungsversion öffnen. Eine migrierte Datenbank wird nicht
rückwärts migriert. Ein interaktiver Wiederherstellungsdialog gehört nicht zu dieser Version.

## Freigabeprüfungen

`mvnw verify` erzwingt Regressionen, 85 % Anweisungs-/70 % Zweigabdeckung und SpotBugs.
GUI-Regressionen: `mvnw verify -Dtest=GuiRegressionTest -Dfaktura.gui.tests=true
-Djacoco.skip=true`. Leistungsnachweis: `ProduktionsPerformanceTest` mit
`-Dfaktura.performance=true -Djacoco.skip=true`; anschließend `tools/paket-pruefen.ps1`
dreimal auf dem gebauten JAR und dem erzeugten SQLite-Referenzbestand.
Die bisherigen JSON-Messungen gelten nur als JSON-Kompatibilitätsmessungen.

Der Release-Workflow verlangt das gleiche JAR auf Windows x64, Linux x64 und macOS arm64
mit JDK 21, installierten MSI-Start mit gebündelter Laufzeit und anschließender Deinstallation,
Leistungsgrenzen und vollständigen Dependency-Check. High/Critical blockieren (CVSS ≥ 7).
Aktuell bestehen keine Sicherheitsausnahmen. Eine künftige Ausnahme benötigt dokumentierte
Begründung, Verantwortlichen, Ablaufdatum und eine entsprechend begrenzte Scannerregel.
Pull Requests erhalten zusätzlich Dependency Review. Der NVD-API-Schlüssel ist ein CI-Secret;
fehlgeschlagene oder unvollständige Scans zählen nicht als bestanden.

Usability-Studien mit fünf Personen und rechtliche Gesamtbewertungen sind weiterhin offen.
