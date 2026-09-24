---
title: "Modultestbericht"
subtitle: "Faktura — Desktop-Fakturierungsanwendung"
author: "Lucas Strubel"
date: "24.09.2026"
version: "1.1"
lang: de-DE
---


| Autor | Rolle | Stand |
|-------|-------|-------|
| Lucas Strubel | Verfasser für das SE1-Projektteam (Gesamtsystem) | 24.06.2026 (Abnahme v1.0), 24.09.2026 (Nachtest v3.0) |

**Freigabevermerk:** Dieses Dokument dokumentiert die Durchführung und die Ergebnisse des
Modultests des Gesamtsystems *Desktop-Fakturierungsanwendung* (Komponenten A–D, gemeinsame
Infrastruktur sowie Performance-Nachweise) gemäß Modultestplan v2.0. Es ist nach Prüfung und
Freigabe durch den Modulverantwortlichen verbindlicher Nachweis des durchgeführten Modultests.

## Dokumentenhistorie

| Version | Datum      | Autor                       | Grund der Änderung  |
|----|------|--------|------------------------------|
| 1.0     | 24.06.2026 | Lucas Strubel  | Initiale Erstellung; Dokumentation der Modultest-Durchführung auf Basis des Modultestplans v2.0 |
| 1.1     | 24.09.2026 | Lucas Strubel  | Kapitel 5: Nachtest Version 3.0 (201 Ausführungen, 0 Fehlschläge, Abdeckung und SpotBugs); Kapitel 1–4 unverändert |


## 1. Einleitung

### 1.1 Zweck des Dokuments
Dieser Modultestbericht dokumentiert die **Durchführung** und die **Ergebnisse** der im
Modultestplan v2.0 spezifizierten Modul-/Komponententests des **Gesamtsystems**
*Desktop-Fakturierungsanwendung*. Berichtet werden die Ergebnisse aller vier Komponenten
(A – Prozess/Dokumentenzyklus, B – Produktverwaltung, C – Kundenverwaltung,
D – Programmoberfläche), der gemeinsam genutzten Infrastruktur (Paket `gemeinsam`) sowie der
übergreifenden Performance-/Lastnachweise. Für jeden im Plan festgelegten Testfall werden die
ausführende JUnit-5-Testmethode und das tatsächliche Ergebnis (`Bestanden`/`Fehlgeschlagen`)
ausgewiesen. Quelle der Wahrheit sind die Testklassen unter
`src/test/java/de/lucasstrubel/faktura/…` sowie die beim Testlauf erzeugten Surefire-Berichte
(`target/surefire-reports/`).

> **Stand:** Dieses Dokument ist der Nachweis des Modultestlaufs zur Abnahme der
> **Version 1.0** (Stand 24.06.2026) und wird als historischer Beleg unverändert geführt.
> Alle Ergebnisse, Laufzeiten und Aussagen zur Oberfläche in den Kapiteln 1–4 und 6
> beziehen sich auf diesen Stand — insbesondere die Angabe „ohne Swing-Rendering“ in
> Kapitel 6 — und geben nicht den aktuellen Stand der Anwendung wieder. Den Nachtest der
> Version 3.0 dokumentiert Kapitel 5.

### 1.2 Testgegenstand und Referenzdokumente
Prüfgegenstand ist die Anwendung `fakturierung` in Version 1.0.0. Grundlage der Testfälle
(Vorbedingungen, Eingaben, erwartete Ergebnisse) ist der **Modultestplan v2.0**; die
fachlichen Anforderungen stammen aus den Teilen A–D des Pflichtenhefts sowie dem
Lastenheft v1.3. Die Abkürzungen für die abgedeckten Anforderungen (Spalte *Abgedeckte Anf.*)
sind — wie im Modultestplan — **komponentenlokal** zu lesen: `F-01` in Abschnitt 3.2 bezeichnet
eine Anforderung des Pflichtenhefts B, nicht des Pflichtenhefts A.

## 2. Testdurchführung

### 2.1 Testumgebung
Der vollständige Testlauf wurde mit `./mvnw test` (maven-surefire-plugin) ausgeführt.

| Merkmal                 | Wert |
|-------------------------|------|
| Prüfgegenstand          | `fakturierung` 1.0.0 |
| Build-/Testwerkzeug     | Apache Maven 3.9.9 (Maven-Wrapper), maven-surefire-plugin 3.2.5 |
| Testframework           | JUnit Jupiter 5.10.2 (JUnit Platform) |
| Laufzeitumgebung        | Oracle JDK 26 (Build 26+35-2893) |
| Sprach-/Kompilierziel   | Java 21 (`maven.compiler.release=21`) |
| Betriebssystem          | Windows 11 (10.0), amd64 |
| Locale / Kodierung      | de_DE; `file.encoding=UTF-8` |
| Ausführungsdatum        | 24.06.2026, 08:42 Uhr (Europe/Berlin) |
| Testkommando            | `./mvnw test` |

### 2.2 Vorgehen
Die Testfälle sind deterministisch (feste Ein-/Ausgaben) und mit JUnit 5 umgesetzt.
Geldbeträge werden als `java.math.BigDecimal` mit **Scale 2** geprüft. Die jeweils
benachbarten Komponenten werden im Modultest durch **Stubs/Mocks** ersetzt:

- **Komponente A** ersetzt die Schnittstellen der Komponenten B (Produkte) und C (Kunden) sowie
  den PDF-Export durch Stubs.
- **Komponente B** ersetzt die Schnittstelle `ProduktReferenzPruefung` (Komponente A) durch einen Stub.
- **Komponente C** ersetzt die Schnittstelle `KundenReferenzPruefung` (Komponente A) durch einen Stub.
- **Komponente D** ersetzt die Service-Schnittstellen der Komponenten A–C durch Stubs/Mocks.

Die nachfolgenden Tabellen (Abschnitt 3) führen die Testfälle in der Reihenfolge des
Modultestplans auf und ergänzen je die ausführende JUnit-Testmethode und das tatsächliche
Ergebnis. Bei abweichendem Ergebnis wäre der Befund in Abschnitt 5 dokumentiert.

## 3. Testergebnisse

### 3.1 Komponente A — Prozess / Dokumentenzyklus

#### 3.1.1 Dokumentenzyklus (`DokumentzyklusTest`)

| TC    | Abgedeckte Anf. | JUnit-Testmethode | Erwartetes Ergebnis | Status |
|-------|-----------------|-------------------|---------------------|--------|
| TC-01 | F-23, F-03      | `tc01SteuerUndBruttoEinerPosition` | Steuer = 19.00, Brutto = 119.00 (Scale 2) | Bestanden |
| TC-02 | F-23            | `tc02Positionssumme` | positionssummeNetto = 150.00 | Bestanden |
| TC-03 | F-03, F-13      | `tc03BelegSummen` | Netto 200.00, Steuer 32.00, Brutto 232.00 | Bestanden |
| TC-04 | F-12, GR-01     | `tc04LueckenloseRechnungsnummer` | nächste Nummer `R-2026-000124` (lückenlos) | Bestanden |
| TC-05 | F-12 (Format)   | `tc05NummernFormat` | `R-2026-000007` (führende Nullen, `String`) | Bestanden |
| TC-06 | F-14, GR-06     | `tc06StandardZahlungsziel` | zahlungsziel = Rechnungsdatum + 14 Tage | Bestanden |
| TC-07 | F-14            | `tc07AbweichendesZahlungsziel` | abweichendes Zahlungsziel wird übernommen | Bestanden |
| TC-08 | F-24, NF-INT-01 | `tc08UnveraenderlichkeitVersendet` | Änderung wirft `IllegalStateException` | Bestanden |
| TC-09 | F-19, F-20      | `tc09Storno` | Status `STORNIERT`; `storniertAm`/`storniertVon` gesetzt | Bestanden |
| TC-10 | F-22, GR-05     | `tc10FolgebelegAusAngebot` | AB übernimmt Kunde/Positionen; `vorgaengerNr` gesetzt | Bestanden |
| TC-11 | F-23, F-24      | `tc11Snapshot` | einzelpreisNetto bleibt 50.00 (Snapshot unverändert) | Bestanden |
| TC-12 | F-18, NF-USE-02 | `tc12PflichtfeldValidierung` | Validierungsfehler benennt fehlendes Pflichtfeld | Bestanden |
| TC-13 | F-11, F-12, F-13| `tc13VollstaendigeRechnung` | Rechnung gespeichert; alle § 14 UStG-Pflichtangaben gesetzt | Bestanden |

#### 3.1.2 Belegpersistenz (`JsonDokumentRepositoryTest`)

| TC    | Abgedeckte Anf. | JUnit-Testmethode | Erwartetes Ergebnis | Status |
|-------|-----------------|-------------------|---------------------|--------|
| TC-14 | IF-01 (GoBD)    | `belegeWerdenNachNeustartGeladen` | beide Belege nach Neustart geladen; Typen `Rechnung`/`Angebot` erhalten | Bestanden |

#### 3.1.3 PDF-Export (`PdfBoxPdfExporterTest`)

| TC    | Abgedeckte Anf. | JUnit-Testmethode | Erwartetes Ergebnis | Status |
|-------|-----------------|-------------------|---------------------|--------|
| TC-15 | F-15, IF-01     | `exportiertVollstaendigenBeleg` | PDF-Datei existiert am Zielpfad | Bestanden |
| TC-16 | F-15 (Robustheit)| `exportiertBelegMitNullPositionsfeldern` | kein Fehler (`assertDoesNotThrow`); PDF existiert | Bestanden |
| TC-17 | F-23 (Robustheit)| `summenberechnungToleriertNullPositionen` | summeNetto = summeSteuer = summeBrutto = 0.00 | Bestanden |

#### 3.1.4 CSV-Datenexport der Belege (`DokumentCsvExportTest`)

| TC    | Abgedeckte Anf. | JUnit-Testmethode | Erwartetes Ergebnis | Status |
|-------|-----------------|-------------------|---------------------|--------|
| TC-18 | Q-08, IF-04     | `exportiertAlleBelegeMitPositionen` | CSV mit Kopfzeile + je Position eine Zeile (3 Zeilen); enthält `STORNIERT`/Benutzer | Bestanden |

### 3.2 Komponente B — Produktverwaltung

Anforderungs-Nummern beziehen sich auf Teil B des Pflichtenhefts; `ProduktReferenzPruefung`
(Komponente A) ist durch einen Stub ersetzt.

| TC    | Abgedeckte Anf. | JUnit-Testmethode | Erwartetes Ergebnis | Status |
|-------|-----------------|-------------------|---------------------|--------|
| TC-01 | F-01, F-02      | `tc01NummernVergabe` | Produkt persistiert; Produktnummer = `P-000042` | Bestanden |
| TC-02 | F-02 (Format)   | `tc02NummernFormat` | `P-000007` (führende Nullen, `String`) | Bestanden |
| TC-03 | F-03            | `tc03NegativerPreis` | Speichern abgelehnt (Validierungsfehler „Einzelpreis“) | Bestanden |
| TC-04 | F-03            | `tc04UnzulaessigerSteuersatz` | Steuersatz `0.15` abgelehnt | Bestanden |
| TC-05 | F-04, NF-USE-01 | `tc05FehlendeBezeichnung` | abgelehnt; Validierungsfehler benennt „Bezeichnung“ | Bestanden |
| TC-06 | F-05            | `tc06PreisAendern` | einzelpreisNetto = 95.00 nach Änderung | Bestanden |
| TC-07 | F-07            | `tc07ProduktnummerUnveraenderlich` | Änderung der Produktnummer wirft `IllegalArgumentException` | Bestanden |
| TC-08 | F-08            | `tc08LoeschenUnverknuepft` | unverknüpftes Produkt entfernt | Bestanden |
| TC-09 | F-09, F-10      | `tc09Loeschsperre` | referenziertes Produkt nicht gelöscht (Löschsperre) | Bestanden |
| TC-10 | F-11            | `tc10Sortierung` | Reihenfolge „Anker“, „Mast“, „Zaun“ | Bestanden |
| TC-11 | F-12            | `tc11SucheBezeichnung` | case-insensitive Teilstring-Treffer | Bestanden |
| TC-12 | F-12            | `tc12SucheNummer` | Suche nach Produktnummer trifft genau dieses Produkt | Bestanden |
| TC-13 | F-14            | `tc13FindeProduktNull` | `findeProdukt("P-999999")` liefert `null` | Bestanden |
| TC-14 | F-15            | `tc14CsvExport` | CSV mit Kopfzeile + 3 Datenzeilen, `;`-getrennt, UTF-8 | Bestanden |

### 3.3 Komponente C — Kundenverwaltung

Anforderungs-Nummern beziehen sich auf Teil C des Pflichtenhefts; `KundenReferenzPruefung`
(Komponente A) ist durch einen Stub ersetzt.

| TC    | Abgedeckte Anf. | JUnit-Testmethode | Erwartetes Ergebnis | Status |
|-------|-----------------|-------------------|---------------------|--------|
| TC-01 | F-01, F-02      | `tc01NummernVergabe` | Kunde persistiert; Kundennummer = `K-000017` | Bestanden |
| TC-02 | F-02 (Format)   | `tc02NummernFormat` | `K-000007` (führende Nullen, `String`) | Bestanden |
| TC-03 | F-03, NF-USE-01 | `tc03FehlenderOrt` | abgelehnt; Validierungsfehler benennt „Ort“ | Bestanden |
| TC-04 | F-03            | `tc04LeererName` | leerer Name abgelehnt; benennt „Name“ | Bestanden |
| TC-05 | F-04            | `tc05UngueltigeEMail` | E-Mail `"max.mustermann"` abgelehnt | Bestanden |
| TC-06 | F-04            | `tc06GueltigeEMail` | E-Mail `"max@beispiel.de"` gespeichert | Bestanden |
| TC-07 | F-05            | `tc07OrtAendern` | Ort „Mannheim“ → „Heidelberg“ gespeichert | Bestanden |
| TC-08 | F-07            | `tc08KundennummerUnveraenderlich` | Änderung der Kundennummer wirft `IllegalArgumentException` | Bestanden |
| TC-09 | F-08            | `tc09LoeschenUnverknuepft` | unverknüpfter Kunde entfernt | Bestanden |
| TC-10 | F-09, F-10, GR-04| `tc10Loeschsperre` | Löschen abgelehnt; Hinweis enthält Anzahl `3` | Bestanden |
| TC-11 | F-11            | `tc11Sortierung` | Reihenfolge „Albrecht“, „Maier“, „Zimmer“ | Bestanden |
| TC-12 | F-12            | `tc12SucheName` | case-insensitive Teilstring-Treffer | Bestanden |
| TC-13 | F-12, F-14      | `tc13SucheNummerUndFindeKunde` | Treffer `K-000017`; `findeKunde` liefert `null` für Unbekannte | Bestanden |
| TC-14 | F-15            | `tc14CsvExport` | CSV mit Kopfzeile + 3 Datenzeilen, `;`-getrennt, UTF-8 | Bestanden |

### 3.4 Komponente D — Programmoberfläche

Getestet wird die GUI-freie Controller- und Modell-Schicht; die Service-Schnittstellen der
Komponenten A–C sind durch Stubs/Mocks ersetzt.

| TC    | Abgedeckte Anf. | JUnit-Testmethode | Erwartetes Ergebnis | Status |
|-------|-----------------|-------------------|---------------------|--------|
| TC-01 | F-09            | `tc01ErsterSchritt` | erster Schritt = `KUNDE_WAEHLEN` | Bestanden |
| TC-02 | F-09            | `tc02Schrittfolge` | Schrittfolge bis `SPEICHERN` durchlaufen | Bestanden |
| TC-03 | F-10            | `tc03KeinKunde` | Wechsel verhindert; Meldung benennt „Kunde“ | Bestanden |
| TC-04 | F-10            | `tc04KeinePosition` | Wechsel verhindert; Meldung benennt „Position“ | Bestanden |
| TC-05 | F-10            | `tc05MengeNull` | Menge 0 verhindert Wechsel; Meldung benennt „Menge“ | Bestanden |
| TC-06 | F-11            | `tc06ZurueckOhneDatenverlust` | `kundenNr` und `positionen` bleiben erhalten | Bestanden |
| TC-07 | F-12            | `tc07Zusammenfassung` | enthält Kunde, Positionen, Summen, Datum, Zahlungsziel | Bestanden |
| TC-08 | F-13            | `tc08GenauEinSpeicheraufruf` | genau ein `erstelleRechnung(...)`; Erfolgsmeldung mit Nummer | Bestanden |
| TC-09 | F-13 (Fehlerfall)| `tc09SpeichernFehlerfall` | `Meldung(FEHLER, "Rechnungsdatum", …)` dargestellt | Bestanden |
| TC-10 | F-14            | `tc10StornierenNurOffen` | *Stornieren* nur bei Status `OFFEN` aktiviert | Bestanden |
| TC-11 | F-15            | `tc11StornoNurNachBestaetigung` | ohne Bestätigung kein Aufruf; mit Bestätigung genau einer | Bestanden |
| TC-12 | F-08            | `tc12VersendeterBeleg` | Änderungsaktionen deaktiviert; PDF-Export aktiviert | Bestanden |
| TC-13 | F-06            | `tc13Statusfilter` | Statusfilter `OFFEN` liefert genau die 2 offenen Belege | Bestanden |
| TC-14 | F-03            | `tc14StammdatenSuche` | Controller delegiert an `KundenService.suche(...)`; Treffer | Bestanden |
| TC-15 | F-03 (D-F-03)   | `tc15KundenListe` | leerer Suchbegriff: gesamter Bestand; sonst gefiltert | Bestanden |

### 3.5 Gemeinsame Infrastruktur (Paket `gemeinsam`)

Querschnittliche Dienste, die von allen Komponenten genutzt werden: der `EreignisBus`
(Observer-Muster) sowie die atomare `JsonPersistenz` hinter allen Repositories (IF-01).

#### 3.5.1 Ereignisbenachrichtigung (`EreignisBusTest`)

| TC     | Abgedeckte Anf. | JUnit-Testmethode | Erwartetes Ergebnis | Status |
|--------|-----------------|-------------------|---------------------|--------|
| INF-01 | Observer (PH-A §7) | `benachrichtigtAlleBeobachter` | beide Beobachter genau 2× benachrichtigt | Bestanden |
| INF-02 | Observer        | `benachrichtigtNurBetroffenenBereich` | nur `DOKUMENTE`-Beobachter; `KUNDEN` 0× | Bestanden |
| INF-03 | Observer (Robustheit) | `meldenOhneBeobachterIstWirkungslos` | wirkungslos, keine Exception | Bestanden |

#### 3.5.2 Atomare JSON-Persistenz (`JsonPersistenzTest`)

| TC     | Abgedeckte Anf. | JUnit-Testmethode | Erwartetes Ergebnis | Status |
|--------|-----------------|-------------------|---------------------|--------|
| INF-04 | IF-01 (atomar)  | `schreibtOhneTempDateiRueckstand` | Zieldatei existiert; keine `.tmp`-Restdatei; Inhalt korrekt | Bestanden |
| INF-05 | IF-01           | `ersetztVorhandenenBestand` | Bestand vollständig ersetzt | Bestanden |
| INF-06 | IF-01           | `legtElternverzeichnisseAn` | fehlende Elternverzeichnisse angelegt; Datei existiert | Bestanden |

### 3.6 Performance-/Lastnachweise (`PerformanceTest`)

Übergreifende Lastnachweise gemäß Lastenheft (Referenzgröße: 5.000 Kunden, 5.000 Produkte,
1.000 Belege; das Befüllen fließt nicht in die Messung ein). Die Spalte *Status* nennt neben
dem Ergebnis die gemessene Ausführungszeit (Surefire) gegen die jeweilige Zeitschranke.

| ID   | Abgedeckte Anf. | JUnit-Testmethode | Erwartetes Ergebnis (Zeitschranke) | Status |
|------|-----------------|-------------------|------------------------------------|--------|
| Q-04 | Q-04 (Start)    | `q04Anwendungsstart` | drei Repositories laden in ≤ 5 s | Bestanden (gemessen 0,039 s) |
| Q-02 | Q-02 (Suche)    | `q02Suche` | Suche/Auflistung in ≤ 1 s | Bestanden (gemessen 0,025 s) |
| Q-03 | Q-03 (PDF)      | `q03PdfErstellung` | PDF-Erstellung (50 Positionen) in ≤ 2 s | Bestanden (gemessen 0,011 s) |
| Q-08 | Q-08 (Export)   | `q08Datenexport` | Vollexport (CSV) in ≤ 30 s | Bestanden (gemessen 0,181 s) |

## 4. Ergebnisübersicht und Statistik

| Testklasse | Komponente | Anzahl | Bestanden | Fehlgeschlagen |
|------------|-----------|--------|-----------|----------------|
| `dokumente/DokumentzyklusTest`        | A – Prozess/Dokumentenzyklus            | 13 | 13 | 0 |
| `dokumente/JsonDokumentRepositoryTest`| A – Belegpersistenz (IF-01/GoBD)        | 1  | 1  | 0 |
| `dokumente/PdfBoxPdfExporterTest`     | A – PDF-Export (F-15)                   | 3  | 3  | 0 |
| `dokumente/DokumentCsvExportTest`     | A – CSV-Export Belege (Q-08)            | 1  | 1  | 0 |
| `produkte/ProduktVerwaltungTest`      | B – Produktverwaltung                   | 14 | 14 | 0 |
| `kunden/KundenVerwaltungTest`         | C – Kundenverwaltung                    | 14 | 14 | 0 |
| `gui/OberflaechenControllerTest`      | D – Programmoberfläche                  | 15 | 15 | 0 |
| `gemeinsam/EreignisBusTest`           | Gemeinsame Infrastruktur (Observer)     | 3  | 3  | 0 |
| `gemeinsam/JsonPersistenzTest`        | Gemeinsame Infrastruktur (Persistenz)   | 3  | 3  | 0 |
| `PerformanceTest`                     | Querschnitt (Q-02/Q-03/Q-04/Q-08)       | 4  | 4  | 0 |
| **Summe**                             |                                         | **71** | **71** | **0** |

**Surefire-Gesamtergebnis:** `Tests run: 71, Failures: 0, Errors: 0, Skipped: 0`.
Reine Testausführung (Surefire): ≈ 1,4 s; Maven-Gesamtlauf: ≈ 3,1 s. Die **Bestehensquote
beträgt 100 %** (71 von 71 Testfällen bestanden).

## 5. Nachtest Version 3.0 (24.09.2026)

Zum Abschluss der Version 3.0 wurde die gesamte Testsuite erneut ausgeführt —
diesmal über `./mvnw verify`, also einschließlich der Abdeckungsschwelle (JaCoCo)
und der statischen Analyse (SpotBugs), die seit Version 2.0 bzw. 3.0 Teil des Builds
sind. Die Testfälle der Version 1.0 (Kapitel 3) sind darin vollständig enthalten;
die ergänzten Testfälle sind im Modultestplan, Kapitel 3, spezifiziert.

### 5.1 Testumgebung

| Merkmal | Wert |
|---------|------|
| Prüfgegenstand | `faktura` 3.0.0 |
| Build-/Testwerkzeug | Apache Maven 3.9.9 (Maven-Wrapper), maven-surefire-plugin 3.2.5 |
| Laufzeit | JDK 26.0.1 lokal (übersetzt mit `--release 21`); CI: Temurin 21 unter Ubuntu und Windows |
| Testframework | JUnit Jupiter (über die Spring-Boot-Stückliste) |
| Qualitätsschranken | JaCoCo 0.8.15 (≥ 85 % Anweisungen, ≥ 70 % Zweige), SpotBugs 4.10.3 |

### 5.2 Ergebnisse je Testklasse

| Testklasse | Bereich | Ausführungen | Fehlgeschlagen | Dauer (s) |
|------------|---------|-------------:|---------------:|----------:|
| `dokumente/DokumentCsvExportTest` | A – Dokumentenzyklus | 1 | 0 | 0.53 |
| `dokumente/DokumentzyklusTest` | A – Dokumentenzyklus | 23 | 0 | 0.28 |
| `dokumente/ERechnungExportTest` | A – Dokumentenzyklus | 8 | 0 | 0.42 |
| `dokumente/JdbcDokumentRepositoryTest` | A – Dokumentenzyklus | 7 | 0 | 1.34 |
| `dokumente/JsonDokumentRepositoryTest` | A – Dokumentenzyklus | 1 | 0 | 0.07 |
| `dokumente/KennzahlenDienstTest` | A – Dokumentenzyklus | 6 | 0 | 0.01 |
| `dokumente/NummernkreisIntegritaetTest` | A – Dokumentenzyklus | 3 | 0 | 2.61 |
| `dokumente/PdfBoxPdfExporterTest` | A – Dokumentenzyklus | 9 | 0 | 0.50 |
| `produkte/ProduktVerwaltungTest` | B – Produktverwaltung | 16 | 0 | 0.08 |
| `kunden/KundenVerwaltungTest` | C – Kundenverwaltung | 20 | 0 | 0.11 |
| `gui/DokumentListenControllerTest` | D – Programmoberfläche | 6 | 0 | 0.01 |
| `gui/OberflaechenControllerTest` | D – Programmoberfläche | 17 | 0 | 0.03 |
| `gui/TabellenFormatTest` | D – Programmoberfläche | 16 | 0 | 0.03 |
| `gemeinsam/CsvTest` | Gemeinsame Infrastruktur | 3 | 0 | 0.01 |
| `gemeinsam/DatensicherungTest` | Gemeinsame Infrastruktur | 2 | 0 | 0.40 |
| `gemeinsam/EreignisBusTest` | Gemeinsame Infrastruktur | 3 | 0 | 0.00 |
| `gemeinsam/JsonPersistenzTest` | Gemeinsame Infrastruktur | 3 | 0 | 0.04 |
| `gemeinsam/ValidierungTest` | Gemeinsame Infrastruktur | 38 | 0 | 0.11 |
| `firma/FirmenprofilServiceTest` | Firmenprofil | 9 | 0 | 1.37 |
| `FakturaApplicationTest` | Anwendung/Querschnitt | 6 | 0 | 1.52 |
| `PerformanceTest` | Anwendung/Querschnitt | 4 | 0 | 0.62 |
| **Summe** | | **201** | **0** | |

### 5.3 Qualitätsschranken

| Prüfung | Schranke | Ergebnis |
|---------|----------|----------|
| Anweisungsabdeckung (nicht-grafischer Code) | ≥ 85 % | 87 % — eingehalten |
| Zweigabdeckung (nicht-grafischer Code) | ≥ 70 % | 73 % — eingehalten |
| SpotBugs | 0 Befunde | 0 Befunde |

### 5.4 Ergänzende Prüfung der Oberfläche

Die Oberfläche selbst ist nicht Gegenstand automatisierter Tests (Kapitel 6). Zum Abschluss
der Version 3.0 wurde sie mit einem fiktiven Demodatenbestand (8 Kunden, 8 Produkte,
17 Belege) manuell durchlaufen: Übersicht und Kennzahlen, Statusfilter, Zahlungseingang,
Storno einer versendeten Rechnung mit Stornorechnung, Firmenprofil, Tastenkürzel nach
Ansichtswechsel und der vollständige Rechnungswizard mit 7 %/19 %-Positionen. Die dabei
gefundenen Darstellungsfehler (abgeschnittene Detailangaben, fehlender Bezahlt-Status in
der Übersicht, Sortierung von Namen mit „Ł“) wurden behoben; die erzeugten PDF- und
E-Rechnungsdateien wurden gegeneinander geprüft (identische Summen).

## 6. Bewertung und Abweichungen

**Abweichungen:** keine. Alle **71** im Modultestplan v2.0 spezifizierten Testfälle wurden
ausgeführt und sind bestanden; es traten weder Fehlschläge (*Failures*) noch Fehler (*Errors*)
auf, kein Testfall wurde übersprungen.

**Performance:** Alle vier Lastnachweise (Q-02, Q-03, Q-04, Q-08) wurden deutlich innerhalb der
im Lastenheft geforderten Zeitschranken erfüllt (gemessene Ausführungszeiten ≤ 0,2 s gegenüber
Schranken von 1 s bis 30 s).

**Hinweis zum Prüfumfang:** Die Programmoberfläche (Komponente D) wurde — wie im Modultestplan
vorgesehen — auf Ebene der Controller- und Modell-Schicht ohne Swing-Rendering geprüft; die
Service-Schnittstellen der Nachbarkomponenten wurden durch Stubs/Mocks ersetzt.

**Gesamturteil:** Der Modultest des Gesamtsystems *Desktop-Fakturierungsanwendung* gilt als
**bestanden**. Die im Modultestplan festgelegten funktionalen Kernregeln, die zentralen
Geschäftsregeln (GR-01…GR-06) sowie die Qualitäts-/Performanceanforderungen (Q-02, Q-03, Q-04,
Q-08, Q-09) sind nachgewiesen.

## 7. Abkürzungen
| Abkürzung | Bedeutung |
|-----------|-----------|
| TC  | Testfall (Test Case) |
| INF | Infrastruktur-Testfall (Paket `gemeinsam`) |
| F   | Funktionale Anforderung (Pflichtenheft, komponentenlokal) |
| NF  | Nicht-funktionale Anforderung (Pflichtenheft) |
| IF  | Schnittstelle (Interface) |
| GR  | Geschäftsregel (Lastenheft) |
| Q   | Qualitätsanforderung (Lastenheft) |
| PH  | Pflichtenheft |
| CSV | Comma-Separated Values (offenes Exportformat) |
| GoBD | Grundsätze zur ordnungsmäßigen Führung und Aufbewahrung von Büchern |
| SUT | System under Test (Prüfgegenstand) |
| Surefire | Maven-Plugin zur Testausführung (Berichte unter `target/surefire-reports/`) |
| Bestanden | Testfall erfolgreich: tatsächliches = erwartetes Ergebnis |
| Fehlgeschlagen | Testfall nicht erfolgreich (*Failure* oder *Error*) |
