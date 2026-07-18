---
title: "Projektübersicht"
subtitle: "Faktura — Desktop-Fakturierungsanwendung"
author:
  - Lucas Strubel
version: "2.0"
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
  \fancyhead[C]{Projektübersicht}
  \fancyhead[R]{Version 2.0}
  \fancyfoot[C]{\thepage\ /\ \pageref{LastPage}}
  \renewcommand{\headrulewidth}{0.4pt}
  \renewcommand{\footrulewidth}{0pt}
---

\newpage

# Dokumentenhistorie

| Version | Datum      | Grund der Änderung  |
|---------|------------|---------------------|
| 1.0–1.3 | 04–05/2026 | Project Charter des Hochschulprojekts (Software Engineering 1, TH Mannheim) |
| 2.0     | 18.07.2026 | Überarbeitung zur Projektübersicht des weitergeführten Einzelprojekts |

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

Das Projekt entstand im Sommersemester 2026 als Lehrprojekt im Modul *Software
Engineering 1* (TH Mannheim) und durchlief dort den vollständigen
Software-Engineering-Prozess nach V-Modell — von Project Charter, Lastenheft und
Pflichtenheft über Implementierung und Modultest bis zur Abschlusspräsentation.
Seit Projektabschluss wird es von Lucas Strubel als Einzelprojekt weiterentwickelt
mit dem Ziel, die Anwendung auf Produktqualität zu heben (siehe Roadmap, Kapitel 5).

# Projektziele

## Ziele (Version 1.0, erreicht)

| Nr.  | Ziel        | Erfolgskriterien |
|------|-------------|------------------|
| Z-01 | Digitale Verwaltung von Produkten und Kunden  | CRUD-Operationen für beide Module vollständig implementiert und funktionsfähig |
| Z-02 | Abbildung des vollständigen Dokumentenzyklus  | Alle 4 Dokumenttypen (Angebot, Auftragsbestätigung, Lieferschein, Rechnung) erstellbar und untereinander verknüpfbar |
| Z-03 | Funktionsfähige und bedienbare Programmoberfläche  | Anwendung kann ohne technische Vorkenntnisse zur Erstellung eines Dokuments genutzt werden |

## Nicht-Ziele (Version 1.0)

Die folgenden Punkte waren **explizit nicht** Teil der Version 1.0; einzelne davon
sind Gegenstand der Weiterentwicklung (Kapitel 5):

- Mehrbenutzer- oder Netzwerkfähigkeit (gleichzeitiger Zugriff mehrerer Nutzer)
- Vollständiges Buchhaltungsmodul (keine Bilanzierung)
- Webshop-Anbindung (z. B. WooCommerce, Gambio Connectoren)
- Mobile Clients oder Web-Applikation
- Unterstützung von E-Rechnungsformaten (ZUGFeRD / XRechnung) → *jetzt Roadmap*
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
  (unveränderliche versendete Belege, lückenlose Rechnungsnummern).*
- **E-Rechnungspflicht ab 01.01.2025**: Im B2B-Bereich sind strukturierte elektronische
  Rechnungsformate (ZUGFeRD, XRechnung) gesetzlich vorgeschrieben. *In Version 1.0
  bewusst ausgeklammert; zentraler Bestandteil der Roadmap.*
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
| **6** | Systemtest | Integrationstests, Systemvalidierung |
| **7** | Abnahmetest | Abnahme, Abschlusspräsentation |

Jede Entwicklungsphase korrespondiert mit ihrer jeweiligen Testphase im Rahmen des
V-Modells. Ergebnis: alle Muss-Anforderungen implementiert, 71/71 Modultestfälle
bestanden (siehe *Modultestbericht.md*), Anwendung abgenommen und präsentiert.

# Roadmap der Weiterentwicklung

Die Weiterentwicklung zum produktreifen Einzelprojekt umfasst insbesondere:

1. **Qualität:** härtere Eingabevalidierung (PLZ, USt-IdNr., E-Mail), Logging,
   Continuous Integration mit automatisierten Tests und statischer Analyse
2. **Architektur:** Dependency Injection mit Spring, Migration der Persistenz von
   JSON-Dateien auf SQLite (Spring JDBC, Flyway-Migrationen)
3. **Oberfläche:** Neuentwicklung der GUI mit JavaFX (FXML, moderne Themes,
   Live-Validierung in Formularen)
4. **Fachlichkeit:** konfigurierbares Firmenprofil (Briefkopf, Bankverbindung),
   E-Rechnung nach EN 16931 (ZUGFeRD / XRechnung), Datensicherung
5. **Auslieferung:** nativer Windows-Installer (jpackage), Releases über GitHub

# Risikomanagement

| ID   | Risiko                              | W/A | Gegenmaßnahme                            |
|------|-------------------------------------|-----|------------------------------------------|
| R-01 | Technische Komplexität unterschätzt | M/H | Frühzeitige Spikes, Scope-Reduktion      |
| R-02 | Anforderungsänderungen              | N/M | Anforderungen versioniert im Pflichtenheft |
| R-03 | Regressionsfehler bei Umbauten      | M/H | Automatisierte Testsuite als Sicherheitsnetz, CI |
