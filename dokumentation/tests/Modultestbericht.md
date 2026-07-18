---
title: "Modultestbericht"
subtitle: "Desktop-Fakturierungsanwendung — Gesamtsystem (alle Komponenten)"
author:
  - Lucas Strubel
version: "1.0"
lang: de-DE
toc: true
toc-depth: 3
numbersections: false
papersize: a4
geometry: "margin=3cm"
fontsize: 12pt
linestretch: 1.5
mainfont: "Times New Roman"
sansfont: "Arial"
monofont: "DejaVu Sans Mono"
header-includes: |
  \usepackage{fancyhdr}
  \usepackage{lastpage}
  \pagestyle{fancy}
  \fancyhf{}
  \fancyhead[L]{Faktura}
  \fancyhead[C]{Modultestbericht}
  \fancyhead[R]{Version 1.0}
  \fancyfoot[C]{\thepage\ /\ \pageref{LastPage}}
  \renewcommand{\headrulewidth}{0.4pt}
  \renewcommand{\footrulewidth}{0pt}
  \makeatletter
  \def\brk@scan#1{\ifx\brk@end#1\else#1\allowbreak\expandafter\brk@scan\fi}
  \newcommand{\brk}[1]{\brk@scan#1\brk@end}
  \let\origtexttt\texttt
  \renewcommand{\texttt}[1]{\origtexttt{\brk{#1}}}
  \makeatother
  \AtBeginEnvironment{longtable}{\small}
---

\newpage

+-------------------------+-------------------------+-------------------------+
| Autor                   | Prüfer                  | Freigebender            |
+=========================+=========================+=========================+
| Strubel, Lucas          |
+-------------------------+-------------------------+-------------------------+
| Entwickler (Gesamtsystem) |
+-------------------------+-------------------------+-------------------------+
| 24.06.2026              | 24.06.2026              | 24.06.2026              |
+-------------------------+-------------------------+-------------------------+

**Freigabevermerk:** Dieses Dokument dokumentiert die Durchführung und die Ergebnisse des
Modultests des Gesamtsystems *Desktop-Fakturierungsanwendung* (Komponenten A–D, gemeinsame
Infrastruktur sowie Performance-Nachweise) gemäß Modultestplan v2.0. Es ist nach Prüfung und
Freigabe durch den Modulverantwortlichen verbindlicher Nachweis des durchgeführten Modultests.

## Dokumentenhistorie

| Version | Datum      | Autor                       | Grund der Änderung  |
|---------|------------|-----------------------------|---------------------|
| 1.0     | 24.06.2026 | Lucas Strubel  | Initiale Erstellung; Dokumentation der Modultest-Durchführung auf Basis des Modultestplans v2.0 |

\newpage

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
`src/test/java/de/team1/faktura/…` sowie die beim Testlauf erzeugten Surefire-Berichte
(`target/surefire-reports/`).

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
| TC-03 | F-03            | `tc03NegativerPreis` | Speichern abgelehnt (Validierungsfehler „Einzelpreis") | Bestanden |
| TC-04 | F-03            | `tc04UnzulaessigerSteuersatz` | Steuersatz `0.15` abgelehnt | Bestanden |
| TC-05 | F-04, NF-USE-01 | `tc05FehlendeBezeichnung` | abgelehnt; Validierungsfehler benennt „Bezeichnung" | Bestanden |
| TC-06 | F-05            | `tc06PreisAendern` | einzelpreisNetto = 95.00 nach Änderung | Bestanden |
| TC-07 | F-07            | `tc07ProduktnummerUnveraenderlich` | Änderung der Produktnummer wirft `IllegalArgumentException` | Bestanden |
| TC-08 | F-08            | `tc08LoeschenUnverknuepft` | unverknüpftes Produkt entfernt | Bestanden |
| TC-09 | F-09, F-10      | `tc09Loeschsperre` | referenziertes Produkt nicht gelöscht (Löschsperre) | Bestanden |
| TC-10 | F-11            | `tc10Sortierung` | Reihenfolge „Anker", „Mast", „Zaun" | Bestanden |
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
| TC-03 | F-03, NF-USE-01 | `tc03FehlenderOrt` | abgelehnt; Validierungsfehler benennt „Ort" | Bestanden |
| TC-04 | F-03            | `tc04LeererName` | leerer Name abgelehnt; benennt „Name" | Bestanden |
| TC-05 | F-04            | `tc05UngueltigeEMail` | E-Mail `"max.mustermann"` abgelehnt | Bestanden |
| TC-06 | F-04            | `tc06GueltigeEMail` | E-Mail `"max@beispiel.de"` gespeichert | Bestanden |
| TC-07 | F-05            | `tc07OrtAendern` | Ort „Mannheim" → „Heidelberg" gespeichert | Bestanden |
| TC-08 | F-07            | `tc08KundennummerUnveraenderlich` | Änderung der Kundennummer wirft `IllegalArgumentException` | Bestanden |
| TC-09 | F-08            | `tc09LoeschenUnverknuepft` | unverknüpfter Kunde entfernt | Bestanden |
| TC-10 | F-09, F-10, GR-04| `tc10Loeschsperre` | Löschen abgelehnt; Hinweis enthält Anzahl `3` | Bestanden |
| TC-11 | F-11            | `tc11Sortierung` | Reihenfolge „Albrecht", „Maier", „Zimmer" | Bestanden |
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
| TC-03 | F-10            | `tc03KeinKunde` | Wechsel verhindert; Meldung benennt „Kunde" | Bestanden |
| TC-04 | F-10            | `tc04KeinePosition` | Wechsel verhindert; Meldung benennt „Position" | Bestanden |
| TC-05 | F-10            | `tc05MengeNull` | Menge 0 verhindert Wechsel; Meldung benennt „Menge" | Bestanden |
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

## 5. Bewertung und Abweichungen

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

## 6. Abkürzungen
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
