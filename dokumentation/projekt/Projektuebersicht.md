---
title: "Projektübersicht"
subtitle: "Faktura — Desktop-Fakturierungsanwendung"
author: "Lucas Strubel"
date: "24.09.2026"
version: "3.0"
lang: de-DE
numbersections: true
---

# Dokumentenhistorie

| Version | Datum      | Grund der Änderung  |
|----|------|------------------------------|
| 1.0–1.3 | 04–05/2026 | Project Charter des Hochschulprojekts (Software Engineering 1, TH Mannheim) |
| 2.0     | 18.07.2026 | Überarbeitung zur Projektübersicht des weitergeführten Einzelprojekts |
| 3.0     | 24.09.2026 | Abschluss der Version 3.0 (Phase 12), offene Punkte, Teamzusammensetzung ergänzt |

# Projektübersicht

## Projektzweck

*Faktura* ist eine Desktop-Fakturierungsanwendung für Kleinstunternehmen, Freiberufler
und Selbstständige. Die Anwendung bildet den vollständigen kaufmännischen
Dokumentenzyklus ab – von der Angebotserstellung über Auftragsbestätigung und
Lieferschein bis zur finalen Rechnung. Ziel ist eine schlanke, lokal betriebene
Alternative zu kostenpflichtigen SaaS-Lösungen, die dem Nutzer vollständige
Datensouveränität bietet. Als Referenzsystem dient die Open-Source-Software Fakturama.

## Projekthintergrund und Herkunft

Die fortschreitende Digitalisierung des Rechnungswesens sowie die gesetzliche
E-Rechnungspflicht im B2B-Bereich (ab 01.01.2025) stellen insbesondere
Kleinstunternehmen vor erhebliche Herausforderungen.

Das Projekt ist im Rahmen des Moduls *Software Engineering 1* an der TH Mannheim
gestartet (Sommersemester 2026) und durchlief dort den vollständigen
Software-Engineering-Prozess nach V-Modell — von Project Charter, Lastenheft und
Pflichtenheft über Implementierung und Modultest bis zur Abschlusspräsentation.

Das Projektteam bestand aus 12 Studierenden in vier Gruppen zu je drei Personen; jede
Gruppe verantwortete eine der Komponenten A–D (Dokumentenzyklus, Produkte, Kunden,
Programmoberfläche). Lucas Strubel verwaltete das gemeinsame Repository und verfasste
sämtliche Projektdokumente — Lastenheft, die Pflichtenheft-Teile A–D, Modultestplan und
Modultestbericht.

Seit Projektabschluss wird Faktura von Lucas Strubel als Einzelprojekt weiterentwickelt
mit dem Ziel, die Anwendung auf Produktqualität zu heben (siehe Roadmap).

# Projektziele

## Ziele (Version 1.0, erreicht)

| Nr.  | Ziel        | Erfolgskriterien |
|------|-------------|------------------|
| Z-01 | Digitale Verwaltung von Produkten und Kunden  | CRUD-Operationen für beide Module vollständig implementiert und funktionsfähig |
| Z-02 | Abbildung des vollständigen Dokumentenzyklus  | Alle 4 Dokumenttypen (Angebot, Auftragsbestätigung, Lieferschein, Rechnung) erstellbar und untereinander verknüpfbar |
| Z-03 | Funktionsfähige und bedienbare Programmoberfläche  | Anwendung kann ohne technische Vorkenntnisse zur Erstellung eines Dokuments genutzt werden |

## Nicht-Ziele (Version 1.0)

Die folgenden Punkte waren **explizit nicht** Teil der Version 1.0; einzelne davon
sind Gegenstand der Weiterentwicklung (Kapitel „Roadmap der Weiterentwicklung“):

- Mehrbenutzer- oder Netzwerkfähigkeit (gleichzeitiger Zugriff mehrerer Nutzer)
- Vollständiges Buchhaltungsmodul (keine Bilanzierung)
- Webshop-Anbindung (z. B. WooCommerce, Gambio Connectoren)
- Mobile Clients oder Web-Applikation
- Unterstützung von E-Rechnungsformaten (ZUGFeRD / XRechnung) → *in Version 2.0 umgesetzt*
- Mahnwesen und automatisiertes Forderungsmanagement
- Garantierter kommerzieller Support oder Service Level Agreements (SLAs)

# Business Case

Kommerzielle Fakturierungssoftware ist für Kleinstunternehmen und Freiberufler häufig
mit monatlichen Lizenzkosten verbunden. Bestehende Open-Source-Alternativen (z. B.
Fakturama) sind funktional umfangreich, jedoch technisch anspruchsvoll in Installation
und Wartung. Das Projekt schafft eine schlanke, wartungsarme Lösung.

**Nutzen:** Kosteneinsparung gegenüber SaaS-Abonnements, vollständige lokale
Datenhaltung ohne Cloud-Zwang, geringer Einrichtungsaufwand.

## Regulatorischer Rahmen

Folgende regulatorische Rahmenbedingungen sind für Fakturierungssoftware in Deutschland
relevant:

- **GoBD** (Grundsätze zur ordnungsmäßigen Führung und Aufbewahrung von Büchern):
  Erstellte Rechnungen dürfen nach Versand nicht mehr verändert werden; alle
  Geschäftsvorfälle müssen lückenlos erfasst werden. *In Version 1.0 umgesetzt
  (unveränderliche versendete Belege, lückenlose Rechnungsnummern), seit Version 3.0
  transaktional abgesichert und um Stornorechnung und Aussteller-Snapshot ergänzt.*
- **E-Rechnungspflicht ab 01.01.2025**: Im B2B-Bereich sind strukturierte elektronische
  Rechnungsformate (ZUGFeRD, XRechnung) gesetzlich vorgeschrieben. *In Version 1.0
  bewusst ausgeklammert; seit Version 2.0 umgesetzt (EN 16931, CII-XML), seit 3.0 mit
  Stornorechnung als korrigierter Rechnung.*
- **DSGVO**: Kundendaten werden ausschließlich lokal gespeichert; es erfolgt keine
  Übertragung an Dritte.

# Projektverlauf Version 1.0 (V-Modell)

| Phase | Bezeichnung | Kernaufgaben |
|:--- |:--- |:--- |
| **1** | Anforderungsanalyse | Project Charter, Stakeholder-Analyse, Lastenheft |
| **2** | Systementwurf | Systemarchitektur, Technologiewahl |
| **3** | Komponentenentwurf | UI/UX-Mockups, Datenmodell, Pflichtenheft |
| **4** | Implementierung | Produkt- und Kundenverwaltung, Dokumentenzyklus, UI |
| **5** | Integrationstest | Schnittstellentests, modulübergreifende Tests |
| **6** | Systemtest | Systemvalidierung gegen das Pflichtenheft |
| **7** | Abnahmetest | Abnahme, Abschlusspräsentation |

Jede Entwicklungsphase korrespondiert mit ihrer jeweiligen Testphase im Rahmen des
V-Modells. Ergebnis: alle Muss-Anforderungen implementiert, 71/71 Modultestfälle
bestanden (siehe *Modultestbericht.md*), Anwendung abgenommen und präsentiert.

# Roadmap der Weiterentwicklung

## Abgeschlossen: Version 2.0 (Phasen 1–7)

1. **Qualität:** härtere Eingabevalidierung (PLZ, USt-IdNr., E-Mail), Logging,
   Continuous Integration mit automatisierten Tests und statischer Analyse
2. **Architektur:** Dependency Injection mit Spring, Migration der Persistenz von
   JSON-Dateien auf SQLite (Spring JDBC, Flyway-Migrationen)
3. **Oberfläche:** Neuentwicklung der GUI mit JavaFX (FXML, moderne Themes,
   Live-Validierung in Formularen)
4. **Fachlichkeit:** konfigurierbares Firmenprofil (Briefkopf, Bankverbindung),
   E-Rechnung nach EN 16931 (ZUGFeRD / XRechnung), Datensicherung
5. **Auslieferung:** nativer Windows-Installer (jpackage), Releases über GitHub

## Abgeschlossen: Version 3.0 (Phasen 8–11)

Version 2.0 war funktional vollständig, aber an drei Stellen noch nicht produktreif:
Die Nummernvergabe konnte Lücken hinterlassen, jede Ausgabe blockierte die Oberfläche,
und die Bedienung bestand aus vier Reitern mit ungruppierten Knopfreihen.

6. **Datenintegrität (Phase 8):** Die Vergabe fortlaufender Nummern liegt in der
   Tabelle `nummernkreis` und läuft in derselben Transaktion wie das Speichern —
   ein fehlgeschlagener Speichervorgang verbraucht keine Rechnungsnummer mehr
   (GR-01). Ereignisse werden erst nach dem Commit zugestellt. Ergänzend:
   WAL-Journalmodus, konsistente Datensicherung über `VACUUM INTO`, globale
   Fehlerbehandlung.
7. **Nebenläufigkeit und Datenschutz (Phase 8):** PDF-, E-Rechnungs- und
   CSV-Ausgabe, Datensicherung, Druck und Mailversand laufen im Hintergrund; die
   Oberfläche bleibt bedienbar. Zwischen-PDFs liegen in einem Sitzungsverzeichnis,
   das beim Beenden restlos gelöscht wird (zuvor blieben Belege mit Kundendaten im
   Temp-Verzeichnis des Systems liegen).
8. **Entwurf (Phase 9):** Ausgabe an Drucker und Mailprogramm als eigene Komponente
   (`BelegAusgabe`), Zusammenführung der vier nahezu gleichen Belegerstellungen,
   Kennzahlen-Auswertung als GUI-freier Dienst.
9. **Oberfläche (Phase 10):** Seitennavigation statt Reiter, neue Übersichtsansicht
   mit Kennzahlen, Master-Detail-Ansicht der Belege, gruppierte Werkzeugleisten,
   Statusabzeichen, nicht blockierende Erfolgsmeldungen, Dunkelmodus,
   Tastenkürzel, Leerzustände, gespeicherte Fenstergröße.
10. **Prüfbarkeit (Phase 11):** Mindestabdeckung als Abbruchkriterium im Build,
    Integritätstests des Nummernkreises, Tests für Kennzahlen, Datensicherung,
    Firmenprofil und Belegfilterung.

## Abgeschlossen: Version 3.0 — Abschluss (Phase 12)

Vor der Veröffentlichung wurde die gesamte Codebasis systematisch auf Fehler durchsucht
und die Anwendung mit realistischen Demodaten durchgespielt. Ergebnis:

11. **Fachliche Lücken geschlossen:** Umsatzsteuer je Steuersatz (PDF und E-Rechnung
    stimmen auf den Cent überein), Steuernummer im Firmenprofil und Pflichtprofil für
    Belege, Aussteller-Snapshot je Beleg, Zahlungseingang, Stornorechnung für versendete
    Rechnungen, Leistungsdatum aus dem Lieferschein (Pflichtenheft v2.4, A-F-25 bis
    A-F-32).
12. **Fehler behoben:** PDF-Export mit eingebetteter Schrift (Namen wie „Łukasz“ brachen
    ihn ab), Umbruch statt Abschneiden, atomare JSON-Übernahme, Datenverzeichnis im
    Benutzerverzeichnis, EU-USt-IdNr./IBAN-Prüfsumme, CSV mit BOM und Formelschutz,
    Tastenkürzel, Datums- und Betragseingabe, Hintergrundaufgaben.
13. **Auslieferung:** plattformübergreifendes JAR (Windows, Linux, macOS auf Apple
    Silicon), Versionsprüfung im Release-Workflow, Dokumentation vollständig auf Deutsch
    und als PDF.

## Offene Punkte

- Usability-Test mit fünf Personen (Q-05, AC-11) — nicht durch Code belegbar
- Steuerbefreiungen und Kleinunternehmerregelung (§ 19 UStG) mit Pflichthinweis
- Mahnwesen
- Installationspakete für Linux und macOS

# Risikomanagement

| ID   | Risiko                              | W/A | Gegenmaßnahme                            |
|------|-------------------------------------|-----|------------------------------------------|
| R-01 | Technische Komplexität unterschätzt | M/H | Frühzeitige Spikes, Scope-Reduktion      |
| R-02 | Anforderungsänderungen              | N/M | Anforderungen versioniert im Pflichtenheft |
| R-03 | Regressionsfehler bei Umbauten      | M/H | Automatisierte Testsuite als Sicherheitsnetz, CI |
