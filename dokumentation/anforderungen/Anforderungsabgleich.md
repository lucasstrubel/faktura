---
title: "Anforderungsabgleich und Traceability-Matrix"
subtitle: "Faktura — Desktop-Fakturierungsanwendung"
author: "Lucas Strubel"
date: "24.09.2026"
version: "2.0"
lang: de-DE
---

# Dokumentenhistorie

| Version | Datum      | Grund der Änderung  |
|----|------|------------------------------|
| 1.0     | 06/2026    | Nachweis der Lastenheft-Anforderungen zur Abnahme der Version 1.0 |
| 2.0     | 24.09.2026 | Neuer Abgleich gegen den Stand 3.0: Codebelege auf SQLite-Persistenz, transaktionalen Nummernkreis und JavaFX-Oberfläche umgestellt; Kapitel 5 mit den Anforderungen der Weiterentwicklung (Pflichtenheft v2.1–v2.4) |

# Zweck

Dieses Dokument weist nach, dass die Implementierung die Anforderungen des Lastenhefts
(v1.3) und die darüber hinausgehenden Anforderungen der Weiterentwicklung (Pflichtenheft
v2.1–v2.4) erfüllt. Es dient als Traceability-Matrix *Anforderung ↔ Codebeleg ↔
Testfall* gemäß Abnahmebedingung Kap. 7.2 des Lastenhefts.

Testfälle werden als *Testklasse ID* angegeben; die IDs entsprechen dem `@DisplayName`
der Testmethode und sind im Modultestplan (Kapitel 2 für Version 1.0, Kapitel 3 für die
Ergänzungen) beschrieben. Status: **erfüllt** — durch Code und automatisierten Test
belegt; **organisatorisch** — nur durch einen Nachweis außerhalb des Codes belegbar.

# Fachliche Anforderungen (BA)

| Anf. | Inhalt | Codebeleg | Testfall | Status |
|---|------|--------|---------|---|
| BA-01 | Kunde anlegen (Pflicht-/Optionalfelder, eindeutige Nr.) | `KundenVerwaltungsService.legeAn`, `JdbcKundennummernGenerator` | `KundenVerwaltungTest` TC-01, TC-18 | erfüllt |
| BA-02 | Kundendaten ändern | `KundenVerwaltungsService.aendere` | `KundenVerwaltungTest` TC-07, TC-19 | erfüllt |
| BA-03 | Kunden löschen (mit Löschsperre) | `KundenVerwaltungsService.loescheKunde` | `KundenVerwaltungTest` TC-09, TC-10 | erfüllt |
| BA-04 | Kunden suchen/auflisten (Name + Nr.) | `JdbcKundenRepository.suche`, `alleSortiertNachName` | `KundenVerwaltungTest` TC-12, TC-13 | erfüllt |
| BA-05 | Produkt anlegen (Preis, Steuersatz, eindeutige Nr.) | `ProduktVerwaltungsService.legeAn`, `JdbcProduktnummernGenerator` | `ProduktVerwaltungTest` TC-01, TC-15 | erfüllt |
| BA-06 | Produktdaten ändern (Snapshot in Belegen) | `ProduktVerwaltungsService.aendere` | `ProduktVerwaltungTest` TC-06, TC-16; `DokumentzyklusTest` TC-11 | erfüllt |
| BA-07 | Produkte löschen (mit Löschsperre) | `ProduktVerwaltungsService.loescheProdukt` | `ProduktVerwaltungTest` TC-08, TC-09 | erfüllt |
| BA-08 | Produkte suchen/auflisten (Bezeichnung + Nr.) | `JdbcProduktRepository.suche`, `alleSortiertNachBezeichnung` | `ProduktVerwaltungTest` TC-11, TC-12 | erfüllt |
| BA-09 | Angebot erstellen + PDF | `StandardDokumentService.erstelleAngebot`, `PdfBoxPdfExporter` | `DokumentzyklusTest` TC-10, DZ-06; `PdfBoxPdfExporterTest` TC-15 | erfüllt |
| BA-10 | Auftragsbestätigung erstellen + PDF | `StandardDokumentService.erzeugeFolgebeleg` | `DokumentzyklusTest` TC-10 | erfüllt |
| BA-11 | Lieferschein erstellen + PDF | `StandardDokumentService.erstelleLieferschein`, `erzeugeFolgebeleg` | `DokumentzyklusTest` DZ-05 | erfüllt |
| BA-12 | Rechnung erstellen (Nr., § 14 UStG, Summen, Ziel) | `StandardDokumentService.erstelleRechnung`, `PdfBoxPdfExporter` | `DokumentzyklusTest` TC-13; `PdfBoxPdfExporterTest` PDF-03 | erfüllt |
| BA-13 | Geführte Rechnungserstellung (Wizard) | `RechnungsWizardController`, `RechnungsWizardModel`, `RechnungsWizardDialog` | `OberflaechenControllerTest` TC-01 bis TC-09, ZE-02 | erfüllt |
| BA-14 | Rechnung stornieren (Datum und Benutzer) | `StandardDokumentService.storniere`, `Rechnung.storniere` | `DokumentzyklusTest` TC-09, DZ-01, DZ-02; `OberflaechenControllerTest` TC-10, TC-11 | erfüllt |

# Geschäftsregeln (GR)

| Anf. | Inhalt | Codebeleg | Testfall | Status |
|---|------|--------|---------|---|
| GR-01 | Lückenlose Rechnungsnummern | `JdbcNummernkreis` (Vergabe in derselben Transaktion wie das Speichern), `StandardDokumentService.vergebeNummer` | `NummernkreisIntegritaetTest` NK-01 bis NK-03; `DokumentzyklusTest` TC-04, TC-05, DZ-08, DZ-09 | erfüllt |
| GR-02 | Unveränderlichkeit versendeter Dokumente | `Dokument.pruefeAenderbar`, gespeicherte Summen beim Laden | `DokumentzyklusTest` TC-08, DZ-03; `JdbcDokumentRepositoryTest` JDB-02, JDB-07 | erfüllt |
| GR-03 | Steuerberechnung (Snapshot) | `Dokumentposition`, `Dokument.berechneSummen`, `Dokument.steueraufschluesselung` | `DokumentzyklusTest` TC-01 bis TC-03, TC-11, DZ-10 | erfüllt |
| GR-04 | Referenzielle Integrität Kunden | `KundenVerwaltungsService.loescheKunde`, `DokumentReferenzPruefung` | `KundenVerwaltungTest` TC-10 | erfüllt |
| GR-05 | Dokumentenzyklus-Konsistenz (Rückreferenz) | `StandardDokumentService.erzeugeFolgebeleg` | `DokumentzyklusTest` TC-10, DZ-05 | erfüllt |
| GR-06 | Standard-Zahlungsziel 14 Tage | `StandardDokumentService.STANDARD_ZAHLUNGSZIEL_TAGE` | `DokumentzyklusTest` TC-06, TC-07 | erfüllt |

# Qualitätsanforderungen (Q)

| Anf. | Inhalt | Codebeleg / Nachweis | Testfall | Status |
|---|------|--------|---------|---|
| Q-01 | Referenzgröße 5.000 Kunden/Produkte | Seeding im Test | `PerformanceTest` (Seeding) | erfüllt |
| Q-02 | Suche/Auflistung ≤ 1 s | `Jdbc*Repository.suche` bzw. `Json*Repository.suche` | `PerformanceTest` `q02Suche` | erfüllt |
| Q-03 | PDF-Erstellung ≤ 2 s (50 Positionen) | `PdfBoxPdfExporter` mit eingebetteter Schrift | `PerformanceTest` `q03PdfErstellung` | erfüllt |
| Q-04 | Anwendungsstart ≤ 5 s | Laden des Bestands, Spring-Kontext | `PerformanceTest` `q04Anwendungsstart`; `FakturaApplicationTest` KTX-01 | erfüllt |
| Q-05 | Ersterstellung Rechnung < 10 min (≥ 5 Personen) | Wizard `RechnungsWizardController` | Usability-Test | organisatorisch |
| Q-06 | 100 % lokale Datenhaltung | SQLite-Datenbank im Benutzerverzeichnis; Mailversand nur über den lokalen Standard-Client (IF-03) | `FakturaApplicationTest` KTX-02, KTX-06; Code-Inspektion | erfüllt |
| Q-07 | Unveränderlichkeit versendeter Rechnungen | `Dokument.pruefeAenderbar`, Stornorechnung statt Änderung | `DokumentzyklusTest` TC-08, DZ-01 | erfüllt |
| Q-08 | Vollexport Stamm- und Bewegungsdaten ≤ 30 s | `KundenCsvExport`, `ProduktCsvExport`, `DokumentCsvExport`, `Csv` | `DokumentCsvExportTest` TC-18; `CsvTest` CSV-01 bis CSV-03; `PerformanceTest` `q08Datenexport` | erfüllt |
| Q-09 | Pflichtfeldhinweis (≥ 80 % Korrektur) | `ValidierungsException`, `FxMeldung`, Wizard-Validierung | `OberflaechenControllerTest` TC-03 bis TC-05; `ValidierungTest` VAL-01; Usability-Test | erfüllt (Code), organisatorisch (Usability) |

# Akzeptanzkriterien (AC)

| Anf. | bezogen auf | Testfall | Status |
|---|----|------------|---|
| AC-01 | BA-01, BA-04 | `KundenVerwaltungTest` TC-01, TC-12 | erfüllt |
| AC-02 | BA-02, BA-03, GR-04 | `KundenVerwaltungTest` TC-07, TC-10 | erfüllt |
| AC-03 | BA-05, BA-06, GR-02 | `ProduktVerwaltungTest` TC-06; `DokumentzyklusTest` TC-11 | erfüllt |
| AC-04 | BA-07, BA-08 | `ProduktVerwaltungTest` TC-08, TC-09, TC-11, TC-12; `PerformanceTest` `q02Suche` | erfüllt |
| AC-05 | BA-09, Q-03 | `DokumentzyklusTest` TC-10; `PerformanceTest` `q03PdfErstellung` | erfüllt |
| AC-06 | BA-10 | `DokumentzyklusTest` TC-10 | erfüllt |
| AC-07 | BA-11 | `DokumentzyklusTest` DZ-05 | erfüllt |
| AC-08 | BA-12, GR-01, GR-06 | `DokumentzyklusTest` TC-04, TC-06, TC-13; `NummernkreisIntegritaetTest` NK-01 | erfüllt |
| AC-09 | BA-13 | `OberflaechenControllerTest` TC-02, TC-07, TC-08 | erfüllt |
| AC-10 | BA-14 | `DokumentzyklusTest` TC-09, DZ-01; `OberflaechenControllerTest` TC-10, TC-11 | erfüllt |
| AC-11 | Q-09 | `OberflaechenControllerTest` TC-03 bis TC-05; Usability-Test | erfüllt (Code), organisatorisch (Usability) |

# Anforderungen der Weiterentwicklung (Pflichtenheft v2.1–v2.4)

Diese Anforderungen gehen über das Lastenheft hinaus. Die Präfixe bezeichnen den Teil des
Pflichtenhefts (A–D).

| Anf. | Inhalt | Codebeleg | Testfall | Status |
|---|------|--------|---------|---|
| A-F-25 | Umsatzsteuer je Steuersatz; PDF und E-Rechnung identisch | `Dokument.steueraufschluesselung`, `Steuerzeile`, `PdfBoxPdfExporter` | `DokumentzyklusTest` DZ-10; `ERechnungExportTest` ERE-03; `PdfBoxPdfExporterTest` PDF-03 | erfüllt |
| A-F-26 | Pflichtprofil, Steuernummer oder USt-IdNr. für Rechnungen | `FirmenprofilService.fuerBeleg`, `Firmenprofil.hatSteuerkennung` | `FirmenprofilServiceTest` FP-01, FP-07; `DokumentzyklusTest` DZ-08; `ERechnungExportTest` ERE-08; `PdfBoxPdfExporterTest` PDF-05 | erfüllt |
| A-F-27 | Aussteller-Snapshot je Beleg | `Dokument.setzeAussteller`, `JdbcDokumentRepository` (Flyway V4) | `DokumentzyklusTest` DZ-07; `ERechnungExportTest` ERE-07; `JdbcDokumentRepositoryTest` JDB-06 | erfüllt |
| A-F-28 | Zahlungseingang | `Rechnung.markiereBezahlt`, `StandardDokumentService.markiereBezahlt`, `KennzahlenDienst` | `DokumentzyklusTest` DZ-03, DZ-04; `KennzahlenDienstTest` KZ-06; `OberflaechenControllerTest` ZE-01 | erfüllt |
| A-F-29 | Stornorechnung für versendete Rechnungen | `StandardDokumentService.storniere`, `Rechnung.getStornoZu` | `DokumentzyklusTest` DZ-01, DZ-02; `ERechnungExportTest` ERE-04; `PdfBoxPdfExporterTest` PDF-04 | erfüllt |
| A-F-30 | E-Rechnung nach EN 16931 | `ERechnungExport` (Mustang) | `ERechnungExportTest` ERE-01 bis ERE-08 | erfüllt |
| A-F-31 | Leistungsdatum, Übernahme aus dem Lieferschein | `StandardDokumentService.erstelleRechnung`, `erzeugeFolgebeleg` | `DokumentzyklusTest` DZ-05, DZ-06 | erfüllt |
| A-F-32 | Datumsplausibilität | `StandardDokumentService`, `RechnungsWizardController.pruefeDaten` | `DokumentzyklusTest` DZ-06; `OberflaechenControllerTest` ZE-02 | erfüllt |
| B-F-03 | Preis mit höchstens zwei Nachkommastellen (ab v2.4) | `ProduktVerwaltungsService.validiere`, `TabellenFormat.parseBetrag` | `ProduktVerwaltungTest` TC-15; `TabellenFormatTest` TF-01, TF-02 | erfüllt |
| C-F-16 bis C-F-18 | PLZ, USt-IdNr. (EU), Telefon | `Validierung` | `ValidierungTest` VAL-02 bis VAL-11; `KundenVerwaltungTest` TC-15 bis TC-17 | erfüllt |
| C-F-19 | Einheitliche Schreibweise gespeicherter Werte | `Validierung.bereinige`, `normalisiere…`, `Firmenprofil.bereinigt` | `ValidierungTest` VAL-15; `KundenVerwaltungTest` TC-18; `FirmenprofilServiceTest` FP-08 | erfüllt |
| C-F-20 | IBAN-Prüfsumme, BIC, Steuernummer | `Validierung.pruefeIban`, `pruefeBic`, `pruefeSteuernummer` | `ValidierungTest` VAL-12 bis VAL-14; `FirmenprofilServiceTest` FP-05 | erfüllt |
| D-F-06, D-F-14, D-F-24 | Anzeigestatus „Bezahlt“, Storno-/Zahlungsaktionen je Status | `DokumentListenController.aktionenFuer`, `Bausteine.anzeigestatus` | `OberflaechenControllerTest` TC-10, TC-12, ZE-01; `DokumentListenControllerTest` DL-01 bis DL-06 | erfüllt |
| D-F-18 | Übersicht mit Kennzahlen | `KennzahlenDienst` | `KennzahlenDienstTest` KZ-01 bis KZ-06 | erfüllt |
| D-F-20 | Hintergrundausführung | `HintergrundAufgaben` | Durchlauf der Oberfläche (Modultestbericht 5.4) | erfüllt |
| D-F-22, D-F-25 | Ansichtsbezogene Tastenkürzel, tolerante Datums- und Betragseingabe | `Tastenkuerzel`, `Dialoge.datum`, `TabellenFormat.parseBetrag` | `TabellenFormatTest` TF-01, TF-02; Durchlauf der Oberfläche | erfüllt |

# Offene organisatorische Punkte

Folgende Punkte sind nicht durch Code oder automatisierte Tests abdeckbar:

- **Q-05 / AC-11 – Usability-Tests** mit mindestens fünf Testpersonen
  (Ersterstellung einer Rechnung < 10 min; Pflichtfeldkorrektur ≥ 80 %).

Die Abschlusspräsentation und Abnahme der Version 1.0 (M-07) sind im Rahmen des Moduls
Software Engineering 1 erfolgt. Alle übrigen Anforderungen sind durch Code und
automatisierte JUnit-Tests belegt; die Oberfläche selbst wurde zusätzlich manuell mit
Demodaten geprüft.
