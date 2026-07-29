---
title: "Fallstudie"
subtitle: "Faktura — Desktop-Fakturierungsanwendung"
author:
  - Lucas Strubel
version: "1.0"
lang: de-DE
toc: true
toc-depth: 3
numbersections: true
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
  \fancyhead[C]{Fallstudie}
  \fancyhead[R]{Version 1.0}
  \fancyfoot[C]{\thepage\ /\ \pageref{LastPage}}
  \renewcommand{\headrulewidth}{0.4pt}
  \renewcommand{\footrulewidth}{0pt}
---

\newpage

# Dokumentenhistorie

| Version | Datum      | Grund der Änderung  |
|---------|------------|---------------------|
| 1.0     | 29.07.2026 | Erstfassung: Fallstudie zur Weiterentwicklung vom Hochschulprojekt zum Produkt |

Dieses Dokument ist die deutsche Fassung von `CASE_STUDY.md` im Wurzelverzeichnis des
Projekts. Die englische Fassung ist die führende Version; Änderungen werden dort
vorgenommen und hier nachgezogen.

# Überblick

| | |
|---|---|
| **Gegenstand** | Faktura — lokal betriebene Fakturierungsanwendung für Freiberufler und Kleinstunternehmen |
| **Rolle** | Version 1.0 im siebenköpfigen Studierendenteam; Version 2.0 und 3.0 als Einzelentwickler |
| **Zeitraum** | Version 1.0 im Sommersemester 2026 (TH Mannheim, *Software Engineering 1*), Weiterentwicklung als Einzelprojekt ab Juli 2026 |
| **Umfang** | rund 10.500 Zeilen Java (96 Produktiv-, 20 Testdateien), 6 FXML-Ansichten, 3 Datenbankmigrationen, rund 3.300 Zeilen deutschsprachige Spezifikation |
| **Technik** | Java 21 · Spring Boot · JavaFX · SQLite + Flyway · PDFBox · Mustang (EN 16931) · Maven · JUnit 5 |
| **Stand** | 135 Tests grün, Abdeckungsschwelle und SpotBugs im Build erzwungen, Windows-Installer per Git-Tag veröffentlicht |

# Das Problem

Das Projekt enthält zwei Probleme, die aufeinander liegen. Das zweite ist das
interessantere.

## Fakturierung ist ein Compliance-Problem im CRUD-Gewand

Wer in Deutschland freiberuflich Rechnungen schreibt, hat drei naheliegende Möglichkeiten
— und alle drei taugen nicht. Tabellenkalkulationen sind kostenlos, aber nicht
rechtssicher: Die **GoBD** verlangen lückenlose Rechnungsnummern und die
Unveränderbarkeit versendeter Rechnungen; beides erzwingt eine Tabelle nicht.
SaaS-Lösungen sind rechtssicher, kosten aber dauerhaft Miete für wenige Belege im Monat
und legen Kundendaten auf fremde Server. Open-Source-Alternativen wie *Fakturama* sind
funktional stark, verlangen aber Einrichtungs- und Wartungsaufwand von jemandem, dessen
Beruf nicht IT ist.

Hinzu kommt: Strukturierte elektronische Rechnungen (ZUGFeRD / XRechnung nach EN 16931)
werden im inländischen B2B-Bereich schrittweise verpflichtend. Seit dem **01.01.2025** muss
jedes Unternehmen sie *empfangen* und verarbeiten können; die Pflicht zum *Ausstellen* greift
ab 2027 für größere Unternehmen und ab 2028 für alle übrigen. Ein PDF genügt dann nicht mehr.
Die Zielgruppe braucht damit Software für etwas, das sie von Hand nicht mehr leisten kann.

Das macht die Fachlichkeit trügerisch schwer. Die Masken sehen aus wie gewöhnliches CRUD,
darunter liegen jedoch Regeln, die **immer** gelten müssen, nicht meistens:

| Regel | Was sie tatsächlich verlangt |
|---|---|
| GoBD — Lückenlosigkeit | Keine Rechnungsnummer darf ausgelassen werden, auch nicht bei einem Fehler beim Speichern |
| GoBD — Unveränderbarkeit | Eine versendete Rechnung kann storniert, aber nie geändert oder gelöscht werden |
| § 14 UStG | Jede Rechnung trägt einen festen Satz Pflichtangaben |
| EN 16931 | Strukturiertes XML zusätzlich zur menschenlesbaren PDF-Fassung |
| DSGVO | Kundendaten bleiben auf dem Rechner — keine Cloud, keine Telemetrie |

## Ein abgenommenes Projekt ist noch kein Produkt

Version 1.0 leistete, was ein Lehrprojekt leisten soll. Sie durchlief den vollständigen
V-Modell-Prozess — Lastenheft, Pflichtenheft, Entwurf, Modultestplan, Abnahme — und
bestand ihn: alle Muss-Anforderungen umgesetzt, 71 von 71 Modultestfällen grün.

Auslieferbar war sie damit nicht:

- Die Daten lagen in **JSON-Dateien**, die bei jeder Änderung vollständig neu geschrieben
  wurden.
- Der Rechnungszähler war eine **Zahl im Speicher** und wurde *vor* dem Speichern erhöht.
  Schlug das Speichern fehl, war die Nummer verbraucht — ein stiller Verstoß gegen genau
  die Regel, auf die es fachlich am meisten ankommt.
- Die Verdrahtung war **manuelle Dependency Injection** in `Main.java`.
- Die Oberfläche war **Swing**, und jede länger laufende Aktion — PDF-Export, CSV, Druck —
  lief im Ereignis-Thread; das Fenster fror ein.
- „Installation“ hieß: JDK vorhanden, JAR starten.
- Die E-Rechnung, also das gesetzlich Geforderte, war ausdrückliches Nichtziel.

Das eigentliche Projekt war das zweite: eine Codebasis, die *funktioniert*, so umzubauen,
dass sie *trägt* — ohne die Teile wegzuwerfen, die ihren Platz verdient haben.

# Die Lösung

Faktura bildet den vollständigen kaufmännischen Dokumentenzyklus ab — Angebot →
Auftragsbestätigung → Lieferschein → Rechnung —, jeder Beleg aus seinem Vorgänger
ableitbar, mit PDF- und EN-16931-Ausgabe, Kunden- und Produktverwaltung sowie einem
Firmenprofil, das den Briefkopf speist.

Die Architektur behält die Vier-Komponenten-Teilung der ursprünglichen Spezifikation bei,
weil die Anforderungs-IDs der Spezifikationen darauf abgebildet sind und diese
Nachvollziehbarkeit erhaltenswert ist:

| Paket | Komponente | Verantwortung |
|---|---|---|
| `dokumente` | A | Dokumentenzyklus, Belegnummern, PDF-Export, E-Rechnung EN 16931 |
| `produkte` | B | Produktverwaltung (CRUD, Nummernvergabe, Löschsperre) |
| `kunden` | C | Kundenverwaltung (CRUD, Nummernvergabe, Löschsperre) |
| `gui` | D | JavaFX-Oberfläche (FXML-Ansichten, Spring-injizierte Controller, Rechnungswizard) |
| `gemeinsam` | — | Ereignisbus, Validierung, CSV, Datensicherung, Nummernkreise, Ausnahmen |
| `firma` | — | Firmenprofil (Briefkopf, Bankverbindung) |

Vier Entscheidungen tragen die regulatorischen Regeln. Bei jeder ist die naheliegende
Umsetzung die falsche.

## Lückenlosigkeit ist eine Eigenschaft der *Vergabe*, nicht der sichtbaren Nummern

Der Zähler aus Version 1.0 lieferte lückenlose Nummern — bis etwas fehlschlug. Die Lösung
bestand darin, die Nummernvergabe nicht länger als Buchführung der Anwendung zu behandeln,
sondern als Datenbank-Invariante: eine Tabelle `nummernkreis` (Flyway-Migration V3),
gelesen und erhöht über `JdbcNummernkreis`, ausgeführt **innerhalb derselben Transaktion**
wie das Einfügen des Belegs. Ein fehlgeschlagenes Speichern rollt die Vergabe mit zurück.

Den Nachweis führt ein Test, der den Fehler erzwingt: `NummernkreisIntegritaetTest` schiebt
ein `@Primary`-Repository davor, das auf Kommando eine Ausnahme wirft, und prüft
anschließend, dass die nächste erfolgreiche Rechnung die laufende Nummer 2 trägt und nicht
3 — und dass der gescheiterte Beleg nirgends persistiert wurde. Ein weiterer Testfall
prüft, dass der Zähler einen Neustart übersteht; „lückenlos“ überdauert den Prozess.

## Versendete Belege sind unveränderlich, gelöscht wird nie

`Dokument.pruefeAenderbar()` weist jede inhaltliche Änderung ab, sobald ein Beleg den
Status `VERSENDET` oder `STORNIERT` erreicht hat. An die Stelle des Löschens tritt die
Stornierung — eine stornierte Rechnung behält ihre Nummer, denn ihr Entfernen erzeugte
genau die Lücke, die die Regel verbietet. Eine Feinheit zeigt sich erst in der Praxis: Beim
Laden eines versendeten Belegs aus der Datenbank muss der Status **zuletzt** gesetzt
werden, sonst weist die Prüfung genau die Daten ab, die sie schützen soll.

## Belege speichern Kunde und Preise als Snapshot

Ein Beleg hält Name und Anschrift des Kunden sowie die Preise der Positionen so fest, wie
sie *zum Erstellzeitpunkt* galten. Wird die Anschrift eines Kunden im nächsten Jahr
geändert, darf das die Rechnungen des Vorjahres nicht stillschweigend umschreiben. Diese
Regel stand bereits in der ursprünglichen Spezifikation (`C-F-06`) und ist einer der Teile
der Version 1.0, die jeden Umbau unverändert überstanden haben.

## Die Oberfläche erfährt nur von Daten, die den Commit überlebt haben

Services veröffentlichen nach jeder schreibenden Operation ein `DatenGeaendertEreignis`,
die Ansichten aktualisieren sich selbst. Sobald es echte Transaktionen gab, wurde der
naive Zuhörer zum Fehler: Nach einem Rollback hätte die Oberfläche einen Datensatz
angezeigt, den es nicht gibt. Die Brücke im `EreignisBus` lautet deshalb
`@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` — der
Rückfallpfad hält Ereignisse aus Aufrufen ohne laufende Transaktion am Leben.

Darüber hinaus: Alles, was dauern kann (PDF, E-Rechnung, CSV, Datensicherung, Druck,
Mailversand), läuft über einen Hintergrund-Executor. Zwischen-PDFs liegen in einem
Sitzungsverzeichnis, das beim Beenden restlos gelöscht wird, statt Kundendaten im
Temp-Verzeichnis des Systems zu hinterlassen. Und die Datensicherung zieht ihren Abzug
über `VACUUM INTO`, statt eine Datei zu kopieren, an der offene Verbindungen und eine
WAL-Seitendatei hängen.

# Vorgehen

Elf Phasen in zwei Releases, jede als ein Commit hinter grüner CI:

| Release | Phasen | Inhalt |
|---|---|---|
| **2.0** | 1–7 | Qualitäts-Baseline (Validierung, Logging, CI, JaCoCo, SpotBugs) · Spring-IoC · SQLite + Flyway hinter den bestehenden Repository-Schnittstellen · vollständiger JavaFX-Neubau · Firmenprofil · E-Rechnung EN 16931 · MSI-Installer (jpackage) mit Tag-gesteuertem Release-Workflow |
| **3.0** | 8–11 | Transaktionale Nummernvergabe · Hintergrundausführung und sitzungsgebundene Zwischendateien · Datensicherung über `VACUUM INTO` · Belegausgabe als eigene Komponente · neue Oberfläche (Seitennavigation, Übersicht, Master-Detail, Statusabzeichen, Dunkelmodus) · Abdeckungsschwelle als Abbruchkriterium |

Die Reihenfolge war bewusst gewählt: Die Qualitäts-Baseline kam *zuerst*, damit jeder
folgende strukturelle Eingriff — Austausch der Persistenzschicht, Ersetzen des gesamten
UI-Toolkits — eine Testsuite und eine statische Analyse *unter* sich hatte und nicht
daneben.

# Technologiewahl und Begründung

| Ebene | Wahl | Begründung |
|---|---|---|
| Sprache | **Java 21** | LTS-Version; Records für die Wertetypen (`Summen`, `Kennzahlen`, `Positionsangabe`, …), Textblöcke für lesbares SQL |
| Verdrahtung | **Spring Boot** | Nicht wegen des Webservers, sondern wegen IoC-Container, deklarativem `@Transactional` und Ereignissystem. Transaktionsgebundene Ereignisse sind genau das, was die Nummernvergabe brauchte |
| Oberfläche | **JavaFX + AtlantaFX + Ikonli** | Lokaler Betrieb ist das Produktversprechen, also Desktop-Toolkit statt Browser. AtlantaFX liefert Theme-*Variablen*, dadurch trägt der Dunkelmodus ohne zweites Stylesheet |
| Datenhaltung | **SQLite + Spring JDBC + Flyway** | Ein Benutzer, keine Installation, eine Datei zum Sichern. PostgreSQL brächte einen zu betreibenden Dienst ohne Gegenwert. Flyway macht Schemaänderungen prüfbar; JDBC statt JPA, weil das Schema klein und explizites SQL leichter nachvollziehbar ist |
| PDF | **Apache PDFBox** | Volle Kontrolle über ein festes kaufmännisches Layout |
| E-Rechnung | **Mustang** | EN 16931 ist ein großer, validierter Standard — ZUGFeRD-CII-XML von Hand wäre ein eigenes Projekt |
| Austauschformat | **Jackson (JSON)** | Erhalten für die einmalige Übernahme und für Sicherungen; polymorphe Belegtypen über `@JsonTypeInfo` |
| Build und Qualität | **Maven · JUnit 5 · JaCoCo · SpotBugs · GitHub Actions** | `verify` führt Tests, Abdeckungsschwelle und statische Analyse aus; die CI ruft denselben Befehl auf |
| Auslieferung | **jpackage** | MSI je Benutzer, dadurch weder Administratorrechte noch vorinstalliertes JDK nötig |

Geldbeträge sind durchgängig `BigDecimal` mit Scale 2 und Rundung `HALF_UP`. Bezeichner,
Kommentare und Spezifikationen sind deutsch — die Sprache der Fachlichkeit und ihrer
Regularien —, während README und die englische Fassung dieser Fallstudie auf Englisch
vorliegen.

# Ergebnisse

- **135 automatisierte Tests**, kein Fehlschlag; zur Abnahme der Version 1.0 waren es 71.
- **Abdeckungsschwelle im Build**: `verify` bricht unterhalb von 70 % Anweisungs- und 60 %
  Zweigabdeckung im nicht-grafischen Code ab. FXML-gebundene Ansichtsklassen sind
  ausgenommen, da ihre Dialogführung in eigenen GUI-freien Controller-Klassen liegt, die
  mitgezählt werden.
- **SpotBugs: keine Befunde**, und zwar erzwungen statt berichtet und ignoriert.
- **Performance-Anforderungen sind Zusicherungen, keine Anekdoten.** `PerformanceTest`
  fährt die Referenzgröße des Lastenhefts — 5.000 Kunden, 5.000 Produkte, 1.000 Belege —
  gegen die spezifizierten Grenzwerte (Start ≤ 5 s, Suche ≤ 1 s, PDF mit 50 Positionen
  ≤ 2 s, Vollexport ≤ 30 s). Macht eine Änderung die Anwendung langsamer als gefordert,
  meldet es die CI.
- **Vollständige Nachvollziehbarkeit**: `Anforderungsabgleich.md` bildet jede Anforderung
  auf den umsetzenden Code und den belegenden Testfall ab. Zwei Punkte sind ehrlich als
  *nicht* durch Code belegbar gekennzeichnet — die Usability-Tests brauchen fünf echte
  Personen.
- **Auslieferung auf Knopfdruck**: Ein Tag `v*` baut und veröffentlicht einen
  Windows-Installer.

# Was ich gelernt habe

**Ein Zähler im Speicher ist keine Geschäftsregel.** „Lückenlos“ ist eine Eigenschaft der
*Vergabe*, nicht der Nummern, die man zufällig in der Liste sieht. Die Umsetzung aus
Version 1.0 wirkte in jedem Test korrekt, der nicht fehlschlug — genau das machte sie
gefährlich. Die Lehre: Bei einer Invariante, die auch im Fehlerfall halten muss, *ist* der
Test, der den Fehler erzwingt, die Anforderung. Alles andere ist Zierrat.

**Autokonfiguration ist keine Zusicherung.** Flyway lief unter Spring Boot 4 nicht allein
dadurch, dass `flyway-core` im Klassenpfad lag; die Migration musste programmatisch im
`dataSource`-Bean angestoßen werden. Ich habe Zeit damit verloren anzunehmen, ein Framework
habe etwas erledigt, weil es das üblicherweise tut. Heute prüfe ich, dass die Magie
stattgefunden hat, bevor ich darauf aufbaue.

**Schnittstellen machen einen Umbau überlebbar.** Die Repository-Schnittstellen entstanden
in Version 1.0 für JSON-Dateien. Der Wechsel auf SQLite verlangte neue Implementierungen
und änderte darüber *nichts*. Dasselbe galt für die Oberfläche: Weil die Dialogführung in
GUI-freien Controller-Klassen lag, ließ das vollständige Ersetzen von Swing durch JavaFX
diese Klassen unberührt. Beide Gewinne waren nicht sichtbar, als die Grenzen gezogen
wurden — das ist der Sinn des Ziehens.

**Kompilieren und grüne Tests heißen nicht „funktioniert“.** Vier reale Fehler — eine
Kennzahlenkachel aus dem Fenster geschoben, abgeschnittene Tabellenspalten, eine
Werkzeugleiste mit durchweg beschnittenen Beschriftungen, Detailbeschriftungen als „K…“ —
waren für den Build unsichtbar und binnen Sekunden nach dem Start der Anwendung
offensichtlich. Bei allem mit Oberfläche gehört das Hinsehen zur Definition von „fertig“
und nicht zur Nacharbeit.

**Eine Abdeckungsschwelle soll nachziehen, nicht anstreben.** Die Schwelle liegt knapp
unter dem, was die Suite tatsächlich erreicht. Ihre Aufgabe ist, Rückschritte zu
verhindern, nicht ein Ziel zu verkünden, nach dem niemand handelt; wer die Abdeckung hebt,
zieht die Schwelle hinter sich nach. Eine auf einen Wunschwert gesetzte Schwelle wird beim
ersten Mal abgeschaltet, wenn sie jemanden aufhält.

**Dokumentation verfällt schneller als Code — und leiser.** Wenn ein Dokument veraltet,
schlägt nichts fehl. Die Abschlusspräsentation beschreibt weiterhin Swing, manuelle DI und
die E-Rechnung als Nichtziel — alle drei Aussagen stimmten in der Woche, in der die Folien
entstanden, und waren zwei Wochen später falsch, ohne dass irgendetwas darauf hingewiesen
hätte. Spezifikationen brauchen dieselbe Prüfung auf Aktualität wie Code — und wo ein
Dokument ein historischer Beleg ist, sollte es das offen ausweisen, statt stillschweigend
als aktueller Stand gelesen zu werden.

## Arbeiten mit Claude Code

Dieses Projekt wurde mit **Claude Code** als Werkzeug entwickelt. Es erscheint sinnvoller,
das genau zu beschreiben, als es implizit zu lassen.

**Was das Werkzeug gut konnte:** mechanische Arbeit in einem Umfang, den ich von Hand nicht
geleistet hätte — ein Paket über 96 Dateien hinweg umbenennen, Repository- und
Service-Gerüste erzeugen, deutschsprachige Spezifikationen und Testgerüste entwerfen,
PlantUML-Quellen und Maven-Konfiguration schreiben. Es ist schnell darin, konsistente
Struktur zu erzeugen, sobald die Struktur entschieden ist.

**Was bei mir blieb:** jede Entscheidung des Kapitels „Die Lösung“. Welche Regeln
Invarianten sind, ob die Nummernvergabe in eine Transaktion gehört, was aus Version 1.0
bleibt und was verschwindet, und ob eine Änderung tatsächlich fertig war. Das Werkzeug
schlug plausiblen Code vor; dass die Lückenlosigkeit die Anforderung ist, um die sich der
gesamte Entwurf biegen muss, wusste es nicht — das steht im Lastenheft, nicht im Code.

**Wo Korrektur nötig war:** Ohne ausdrückliche Vorgabe fällt der Vorschlag generisch aus —
der plausibel aussehende Zähler statt des transaktionalen. Fachliche Regeln müssen
ausdrücklich benannt und im Ergebnis nachgeprüft werden, besonders in der Geschäftslogik.

**Was das Vorgehen tragfähig machte:** Änderungen im Zuschnitt einer Phase und eines
Commits; CI, SpotBugs und Abdeckungsschwelle als ständige Leitplanke statt als Nacharbeit;
und bei allem Sichtbaren: die Anwendung starten und hinsehen. Leitplanken wiegen schwerer,
wenn Arbeit schnell entsteht — ein schneller Beitragender ohne Testsuite ist nur ein
schneller Weg, Dinge zu zerbrechen.

# Ausblick

- **Usability-Tests mit fünf Personen** — das einzige Abnahmekriterium (Q-05 / AC-11), das
  Code nicht belegen kann, und weiterhin offen.
- **Pakete für Linux und macOS** — das Bauen aus den Quellen funktioniert bereits auf allen
  drei Plattformen; nur der Installer ist heute Windows-spezifisch.
- **Mahnwesen** — in Version 1.0 als Nichtziel geführt und der naheliegende nächste Schritt,
  nachdem Rechnungen und Zahlungsziele existieren.
