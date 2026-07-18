---
title: "Lastenheft"
subtitle: "Desktop-Fakturierungsanwendung"
author:
  - Lucas Strubel
version: "1.3"
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
  \fancyhead[C]{Lastenheft}
  \fancyhead[R]{Version 1.3}
  \fancyfoot[C]{\thepage\ /\ \pageref{LastPage}}
  \renewcommand{\headrulewidth}{0.4pt}
  \renewcommand{\footrulewidth}{0pt}
---

\newpage

+-------------------------+
| Autor                   |
+=========================+
| Strubel, Lucas          |
+-------------------------+
| Entwickler              |
+-------------------------+
| 09.06.2026              |
+-------------------------+

## Dokumentenhistorie

| Version | Datum      | Grund der Änderung  |
|---------|------------|---------------------|
| 1.0     | 11.05.2026 | Initiale Erstellung |
| 1.1     | 11.05.2026 | Ergänzung Anforderungen Komponente D |
| 1.2     | 14.05.2026 | Ergänzung + Überarbeitung Anforderungen Komponenten A–C |
| 1.3     | 09.06.2026 | Überarbeitung nach Feedback |

## 1. Einleitung und Zielbestimmung

### 1.1 Zweck des Dokuments
Dieses Lastenheft beschreibt aus Sicht des Auftraggebers, **was** die zu entwickelnde Fakturierungsanwendung leisten soll. Es ist technologie- und lösungsneutral formuliert und dient als zentrale Eingabe für den nachfolgenden Systementwurf, das Pflichtenheft sowie die Definition der Akzeptanztests im V-Modell.

### 1.2 Hintergrund
Kleinstunternehmen, Freiberufler und Selbstständige stehen vor wachsenden Anforderungen an die digitale Rechnungsstellung. Marktübliche SaaS-Lösungen sind häufig mit laufenden Lizenzkosten verbunden, etablierte Open-Source-Alternativen (z. B. **Fakturama**) gelten als technisch anspruchsvoll in Installation und Betrieb. Mit der Anwendung wird eine schlanke, lokal betriebene Alternative geschaffen, die dem Anwender vollständige Datensouveränität bietet und ohne Cloud-Anbindung auskommt.

### 1.3 Projektziele
Die folgenden Projektziele beschreiben den angestrebten Nutzen der Anwendung und bilden die Grundlage für die fachlichen Anforderungen in Kapitel 4. Sie sind gleichrangig und ergänzen sich gegenseitig: Erst das Zusammenspiel aus Stammdatenverwaltung (PZ-01), vollständigem Dokumentenzyklus (PZ-02), einfacher Bedienbarkeit (PZ-03) und lokaler Datenhaltung (PZ-04) ergibt eine praxistaugliche Fakturierungslösung.

| Nr. | Ziel | Erfolgskriterium |
|---|---|---|
| PZ-01 | Digitale Verwaltung von Kunden- und Produktstammdaten | CRUD-Operationen für beide Module vollständig und funktionsfähig |
| PZ-02 | Vollständige Abbildung des kaufmännischen Dokumentenzyklus | Alle 4 Dokumenttypen (Angebot, Auftragsbestätigung, Lieferschein, Rechnung) erstellbar und untereinander verknüpfbar |
| PZ-03 | Bedienbarkeit ohne technische Vorkenntnisse | Erstmaliger Anwender erstellt eine Rechnung im ersten Versuch ohne externe Hilfe in unter 10 Minuten |
| PZ-04 | Lokale, datenschutzkonforme Datenhaltung | Keine Datenübertragung an externe Dienste; Speicherung ausschließlich auf dem Anwender-PC |

### 1.4 Nichtziele
Die folgenden Punkte sind ausdrücklich **nicht** Gegenstand dieses Projekts:

- Mehrbenutzer- oder Netzwerkfähigkeit (kein gleichzeitiger Zugriff)
- Vollständiges Buchhaltungsmodul (keine Bilanzierung, kein Mahnwesen)
- Webshop-Anbindung (z. B. WooCommerce, Gambio)
- Mobile Clients oder Web-Applikation
- Unterstützung von E-Rechnungsformaten (ZUGFeRD, XRechnung) – als bekannte Anforderung dokumentiert, jedoch außerhalb des Scopes
- Garantierter kommerzieller Support oder Service Level Agreements (SLAs)

---

## 2. Systemkontext und Rahmenbedingungen

### 2.1 Systemkontext
Die Anwendung weist folgende Merkmale auf:

- läuft als **Desktop-Anwendung** auf einem lokalen Arbeitsplatz-PC des Anwenders.
- nutzt eine **lokale Datenhaltung** (kein externer Server, keine Cloud).
- erzeugt **PDF-Dokumente** im lokalen Dateisystem.
- ist optional an einen **lokal installierten Drucker** anbindbar.
- ist optional an den **Standard-E-Mail-Client** des Betriebssystems anbindbar (Dokument-Anhang).

### 2.2 Rahmenbedingungen
- **Plattform:** Desktop-Betriebssystem (technologieneutrale Spezifikation in diesem Dokument)
- **Betrieb:** Einzelplatz, ohne Mehrbenutzerfähigkeit
- **Datenhaltung:** ausschließlich lokal, kein Cloud-Zwang
- **Datenschutz:** DSGVO-konform; Kundendaten werden nicht an Dritte übertragen
- **Regulatorisch (bekannt, dokumentiert, nicht vollständig umgesetzt):**
  - **GoBD** – Versendete Rechnungen dürfen nachträglich nicht mehr inhaltlich verändert werden; alle Geschäftsvorfälle sind lückenlos zu erfassen.
  - **E-Rechnungspflicht (B2B) ab 01.01.2025** – strukturierte Formate (ZUGFeRD, XRechnung) gesetzlich vorgeschrieben; Umsetzung in diesem Projekt explizit Nichtziel.
- **Lehrkontext:** Studentisches Projekt im Modul Software Engineering 1 (SoSe 2026), Teamgröße 12 Personen, ca. 2 Stunden/Woche pro Person.

---

## 3. Stakeholder und Benutzergruppen

### 3.1 Stakeholder
| ID | Stakeholder | Interesse |
|---|---|---|
| SH-01 | Endanwender (Selbstständige, Freiberufler, Kleinstunternehmer) | Schlanke, lokal nutzbare Lösung ohne laufende Lizenzkosten |
| SH-02 | Auftraggeber (Modulverantwortlicher, TH Mannheim) | Anforderungen, Abnahme, Bewertung |
| SH-03 | Projektteam | Lehrziele, gleichmäßige Beitragsverteilung, Erfüllung des V-Modells |

### 3.2 Benutzerrolle
Im operativen Betrieb wird **eine konsolidierte Benutzerrolle** geführt:

- **Anwender:in** – natürliche Person, die als Selbstständige:r, Freiberufler:in oder Kleinstunternehmer:in den vollständigen Dokumentenzyklus eigenverantwortlich pflegt (Stammdaten, Erstellung, Versand).

Diese Konsolidierung spiegelt die Einzelnutzer-Charakteristik der Anwendung wider (kein Mehrbenutzerbetrieb, vgl. Nichtziel).

---

## 4. Fachliche Anforderungen (Funktionale Anforderungen)

### 4.1 Modul Kundenverwaltung

**BA-01 – Kunden anlegen**
Als Anwender:in muss ich einen neuen Kunden mit den geschäftsrelevanten Stammdaten anlegen können,
um Kunden anschließend in Dokumenten referenzieren zu können.
Die Anforderung gilt, wenn die Anwendung gestartet ist und das Modul Kundenverwaltung geöffnet wurde.
Die Anforderung gilt als erfüllt, wenn ich Pflichtfelder (Name, Anschrift) erfassen, optionale Felder (E-Mail, Telefon, USt-IdNr.) ergänzen und den Datensatz speichern kann und das System eine eindeutige Kundennummer anzeigt.

**BA-02 – Kundendaten ändern**
Als Anwender:in muss ich bestehende Kundendaten ändern können,
um auf Adress- oder Kontaktänderungen reagieren zu können.
Die Anforderung gilt, wenn mindestens ein Kunde im System existiert.
Die Anforderung gilt als erfüllt, wenn ich einen Kunden auswählen, Felder bearbeiten und die Änderungen persistent speichern kann; bereits versendete Dokumente bleiben dabei unverändert (siehe GR-02).

**BA-03 – Kunden löschen**
Als Anwender:in muss ich einen Kunden löschen können,
um nicht mehr benötigte Stammdaten zu entfernen.
Die Anforderung gilt, wenn der gewählte Kunde keine aktiven oder archivierten Dokumente referenziert.
Die Anforderung gilt als erfüllt, wenn der Datensatz nach Bestätigung dauerhaft entfernt ist und das System bei verknüpften Dokumenten den Löschvorgang ablehnt und einen klaren Hinweis anzeigt (siehe GR-04).

**BA-04 – Kunden suchen und auflisten**
Als Anwender:in muss ich Kunden in einer Liste anzeigen und nach Name oder Kundennummer suchen können,
um bei der Dokumenterstellung schnell den richtigen Kunden auswählen zu können.
Die Anforderung gilt, wenn mindestens ein Kunde im System existiert.
Die Anforderung gilt als erfüllt, wenn ich eine sortierte Kundenliste sehe, eine Volltextsuche auf Name und Kundennummer ausführen und das gefilterte Ergebnis innerhalb der Vorgabe aus Q-02 angezeigt bekomme.

### 4.2 Modul Produktverwaltung

**BA-05 – Produkte anlegen**
Als Anwender:in muss ich ein neues Produkt mit Bezeichnung, Einzelpreis (netto), Steuersatz und optionaler Beschreibung anlegen können,
um Produkte in Dokumentpositionen wiederverwenden zu können.
Die Anforderung gilt, wenn das Modul Produktverwaltung geöffnet wurde.
Die Anforderung gilt als erfüllt, wenn der Datensatz persistent gespeichert ist und das System eine eindeutige Produktnummer vergibt.

**BA-06 – Produktdaten ändern**
Als Anwender:in muss ich Produktdaten (z. B. Preis, Bezeichnung) ändern können,
um auf Preisänderungen oder Sortimentsanpassungen reagieren zu können.
Die Anforderung gilt, wenn mindestens ein Produkt im System existiert.
Die Anforderung gilt als erfüllt, wenn die Änderung gespeichert ist und ausschließlich **neue** Dokumente den geänderten Wert verwenden; bereits erstellte Dokumente bleiben unverändert (siehe GR-02).

**BA-07 – Produkte löschen**
Als Anwender:in muss ich ein Produkt löschen können,
um auslaufende oder fehlerhafte Einträge aus dem aktiven Sortiment zu entfernen.
Die Anforderung gilt, wenn das Produkt keine aktiven Dokumentpositionen referenziert.
Die Anforderung gilt als erfüllt, wenn der Datensatz nach Bestätigung entfernt ist; ist das Produkt verknüpft, wird der Löschvorgang abgelehnt und ein Hinweis angezeigt.

**BA-08 – Produkte suchen und auflisten**
Als Anwender:in muss ich Produkte in einer Liste anzeigen und nach Bezeichnung oder Produktnummer suchen können,
um bei der Dokumenterstellung schnell die richtige Position auswählen zu können.
Die Anforderung gilt, wenn mindestens ein Produkt im System existiert.
Die Anforderung gilt als erfüllt, wenn die Liste sortiert dargestellt wird und Suchergebnisse innerhalb der Vorgabe aus Q-02 angezeigt werden.

### 4.3 Prozess: Angebot, Auftragsbestätigung, Lieferschein, Rechnung

**BA-09 – Angebot erstellen**
Als Anwender:in muss ich ein Angebot für einen Kunden mit Positionen aus dem Produktkatalog erstellen können,
um einem Interessenten ein verbindliches Preisangebot vorlegen zu können.
Die Anforderung gilt, wenn der Kunde und mindestens ein Produkt im System existieren.
Die Anforderung gilt als erfüllt, wenn das Angebot mit Angebotsnummer, Datum, Gültigkeit, Positionen, Netto-/Steuer-/Bruttobeträgen gespeichert ist und als PDF lokal exportiert werden kann.

**BA-10 – Auftragsbestätigung erstellen**
Als Anwender:in muss ich eine Auftragsbestätigung erstellen können,
um die Annahme eines Auftrags gegenüber dem Kunden verbindlich zu dokumentieren.
Die Anforderung gilt, wenn die zugrunde liegenden Kunden- und Positionsdaten vorliegen.
Die Anforderung gilt als erfüllt, wenn das Dokument mit eindeutiger Nummer und vollständigen Positionsdaten erstellt, gespeichert und als PDF exportiert werden kann.

**BA-11 – Lieferschein erstellen**
Als Anwender:in muss ich einen Lieferschein erstellen können,
um die Lieferung von Waren oder Leistungen an den Kunden zu dokumentieren.
Die Anforderung gilt, wenn ein zugrundeliegender Geschäftsvorfall (idealerweise Auftragsbestätigung) vorliegt.
Die Anforderung gilt als erfüllt, wenn der Lieferschein mit Lieferdatum, Positionen und Liefermengen gespeichert und als PDF exportiert werden kann.

**BA-12 – Rechnung erstellen**
Als Anwender:in muss ich eine Rechnung erstellen können,
um eine Zahlungsforderung an den Kunden zu stellen.
Die Anforderung gilt, wenn Kundendaten und mindestens eine Position vorliegen.
Die Anforderung gilt als erfüllt, wenn die Rechnung mit fortlaufender Rechnungsnummer (vgl. GR-01), Rechnungsdatum, Leistungsdatum, Pflichtangaben gem. § 14 UStG, Positionen, Netto-/Steuer-/Bruttosummen und Zahlungsziel gespeichert und als PDF exportiert werden kann.

### 4.4 Programmoberfläche

**BA-13 – Geführte Rechnungserstellung**
Als Anwender:in muss ich die Erstellung einer Rechnung schrittweise durchführen können,
um ohne technische Vorkenntnisse eine vollständige Rechnung erfassen zu können.
Die Anforderung gilt, wenn mindestens ein Kunde und mindestens ein Produkt im System existieren.
Die Anforderung gilt als erfüllt, wenn ich Kunde, mindestens eine Produktposition mit Menge, Rechnungsdatum und Zahlungsziel erfassen oder bestätigen kann und vor dem Speichern eine Zusammenfassung der Rechnungsdaten angezeigt bekomme.

**BA-14 – Rechnung stornieren**
Als Anwender:in muss ich eine gespeicherte Rechnung stornieren können,
um fehlerhafte oder nicht mehr gültige Rechnungen aus dem System zu entnehmen zu können.
Die Anforderung gilt, wenn eine Rechnung im Status „offen“ im System gespeichert ist.
Die Anforderung gilt als erfüllt, wenn die Rechnung den Status „storniert“ erhält, nicht mehr als offen gelistet wird und der Vorgang mit Datum und Benutzer protokolliert ist.

---

## 5. Qualitätsanforderungen (nicht-funktionale Anforderungen)

**Q-01 – Datenbestand: Referenzgröße**
Das System soll bei einem Datenbestand von bis zu **5.000 Kunden und 5.000 Produkten** auf einem typischen Endanwender-PC alle Performanceanforderungen (Q-02 bis Q-04) erfüllen.

**Q-02 – Performance: Suche und Auflistung**
Das System soll Such- und Auflistungsergebnisse in den Modulen Kunden- und Produktverwaltung in **maximal 1 Sekunde** anzeigen, bei einem Datenbestand gemäß Q-01.

**Q-03 – Performance: PDF-Erstellung**
Das System soll die Erstellung eines beliebigen Dokumenttyps (Angebot, Auftragsbestätigung, Lieferschein, Rechnung) in **maximal 2 Sekunden** abschließen, bei Dokumenten mit bis zu **50 Positionen**.

**Q-04 – Performance: Anwendungsstart**
Das System soll nach dem Programmstart in **maximal 5 Sekunden** vollständig bedienbereit sein, bei einem Datenbestand gemäß Q-01.

**Q-05 – Benutzbarkeit: Ersterstellung Rechnung**
Eine Person, die die Anwendung erstmals nutzt, soll eine vollständige Rechnung an einen neu angelegten Kunden in **unter 10 Minuten** im ersten Versuch ohne externe Hilfe erstellen können. Überprüfung durch Usability-Test mit mindestens **5 Testpersonen**.

**Q-06 – Datensicherheit: Lokale Speicherung**
Das System soll **100 %** der personenbezogenen und geschäftlichen Daten ausschließlich lokal auf dem Anwender-PC ablegen, ohne jede Datenübertragung an externe Dienste. Überprüfung durch Netzwerk-Monitoring während eines repräsentativen Nutzungslaufs.

**Q-07 – Datenintegrität: Unveränderlichkeit versendeter Rechnungen**
Das System soll nach dem Versandstatus „versendet“ einer Rechnung **jede inhaltliche Änderung ablehnen** und ausschließlich Korrekturen über Storno- oder Korrekturrechnungen ermöglichen, gemäß GoBD.

**Q-08 – Wiederherstellbarkeit: Datenexport**
Das System soll dem Anwender einen vollständigen Export aller Stamm- und Bewegungsdaten in einem offenen, dokumentierten Format ermöglichen, mit einer Exportdauer von **maximal 30 Sekunden** bei einem Datenbestand gemäß Q-01.

**Q-09 – Benutzbarkeit: Korrektur ungültiger Eingaben**
Das System soll fehlende Pflichtangaben in Formularen der Kunden-, Produkt- und Dokumentenerstellung so markieren und benennen, dass mindestens **80 %** der Testpersonen die betroffenen Eingaben ohne externe Hilfe im ersten Korrekturversuch erfolgreich ergänzen können. Überprüfung durch Usability-Test mit mindestens **5 Testpersonen**.

---

## 6. Daten, Schnittstellen und Geschäftsregeln

### 6.1 Datenobjekte (fachliche Sicht)
| Objekt | Kernattribute |
|---|---|
| Kunde | Kundennummer, Name, Anschrift, USt-IdNr., Kontaktdaten |
| Produkt | Produktnummer, Bezeichnung, Beschreibung, Netto-Einzelpreis, Steuersatz, Einheit |
| Dokumentposition | Produktreferenz, Menge, Einzelpreis (Snapshot), Steuersatz (Snapshot), Positionssumme |
| Angebot | Angebotsnummer, Datum, Kunde, Gültigkeit, Positionen, Summen |
| Auftragsbestätigung | AB-Nummer, Datum, Kunde, Referenz Angebot, Positionen, Summen |
| Lieferschein | Lieferscheinnummer, Lieferdatum, Kunde, Referenz Auftragsbestätigung, Positionen |
| Rechnung | Rechnungsnummer, Rechnungsdatum, Leistungsdatum, Kunde, Referenz Lieferschein, Positionen, Summen, Zahlungsziel, Status |

> Hinweis: Eine UML-basierte Detaillierung (Klassendiagramm, ER-Diagramm) erfolgt im Pflichtenheft.

### 6.2 Schnittstellen
| ID | Schnittstelle | Zweck |
|---|---|---|
| IF-01 | Lokales Dateisystem | Persistenz Datenbestand, Speicherung exportierter PDF-Dokumente |
| IF-02 | Druckersystem (lokal, optional) | Direkter Druck von Dokumenten |
| IF-03 | Standard-E-Mail-Client (optional) | Versand erstellter PDF-Dokumente als Anhang |
| IF-04 | Datenexport-Schnittstelle | Export aller Daten in offenem Format (vgl. Q-08) |

Eine Anbindung an externe Online-Dienste, Cloud-Speicher oder Buchhaltungssysteme ist **nicht** Bestandteil dieses Lastenhefts (vgl. Nichtziele).

### 6.3 Geschäftsregeln

**GR-01 – Lückenlose Rechnungsnummern**
Wenn eine neue Rechnung erzeugt wird, dann vergibt das System eine fortlaufende, lückenlose Rechnungsnummer auf Basis der höchsten bisher vergebenen Nummer (GoBD).

**GR-02 – Unveränderlichkeit versendeter Dokumente**
Wenn ein Dokument den Status „versendet“ hat, dann sind sämtliche inhaltlichen Änderungen ausgeschlossen; Korrekturen erfolgen ausschließlich über neue Dokumente (Storno, Korrekturrechnung).

**GR-03 – Steuerberechnung**
Wenn eine Dokumentposition gespeichert wird, dann berechnet das System Netto-, Steuer- und Bruttobetrag automatisch auf Basis des dem Produkt zum Zeitpunkt der Dokumenterstellung zugeordneten Steuersatzes (Snapshot-Prinzip).

**GR-04 – Referenzielle Integrität Kunden**
Wenn ein Kunde mit aktiven oder archivierten Dokumenten existiert, dann lehnt das System das Löschen des Kunden ab und zeigt einen Hinweis mit Verweis auf die abhängigen Dokumente.

**GR-05 – Dokumentenzyklus-Konsistenz**
Wenn ein Dokument aus einem Vorgängerdokument erzeugt wird, dann übernimmt das System Kunde, Positionen und Mengen aus dem Vorgänger und speichert eine eindeutige Rückreferenz.

**GR-06 – Standard-Zahlungsziel**
Wenn eine neue Rechnung erstellt wird und kein abweichendes Zahlungsziel angegeben ist, dann setzt das System ein Standard-Zahlungsziel von **14 Kalendertagen** ab Rechnungsdatum.

---

## 7. Akzeptanzkriterien und Abnahmebedingungen

### 7.1 Akzeptanzkriterien zu den fachlichen Anforderungen

**AC-01 (zu BA-01, BA-04)** – *Kunde anlegen und auffinden*
Vorbedingung: Anwendung gestartet, Modul Kundenverwaltung geöffnet.
Aktion: Die Anwenderin bzw. der Anwender erfasst einen neuen Kunden mit Pflichtfeldern und speichert.
Erwartet: Das System vergibt eine eindeutige Kundennummer, der Kunde erscheint in der Suchergebnisliste innerhalb von ≤ 1 Sekunde (gemäß Q-02).

**AC-02 (zu BA-02, BA-03, GR-04)** – *Kunde ändern und Löschsperre*
Vorbedingung: Ein Kunde mit mindestens einer verknüpften Rechnung existiert.
Aktion: Die Anwenderin bzw. der Anwender ändert einen Adressbestandteil und speichert; anschließend wird versucht, den Kunden zu löschen.
Erwartet: Das System speichert die Änderung erfolgreich, lehnt das Löschen ab und zeigt einen Hinweis mit der Anzahl verknüpfter Dokumente.

**AC-03 (zu BA-05, BA-06, GR-02)** – *Produkt anlegen, ändern, Snapshot-Verhalten*
Vorbedingung: Ein Produkt ist bereits in einer früheren Rechnung erfasst.
Aktion: Die Anwenderin bzw. der Anwender ändert den Einzelpreis des Produkts und erstellt anschließend eine neue Rechnung mit diesem Produkt.
Erwartet: Die alte Rechnung behält den ursprünglichen Preis, die neue Rechnung übernimmt den geänderten Preis.

**AC-04 (zu BA-07, BA-08)** – *Produkt löschen und suchen*
Vorbedingung: Mindestens 100 Produkte sind im System.
Aktion: Die Anwenderin bzw. der Anwender sucht ein Produkt anhand der Bezeichnung und löscht es (sofern unverknüpft).
Erwartet: Die Suchergebnisse erscheinen in ≤ 1 Sekunde (gemäß Q-02); das gelöschte Produkt erscheint anschließend nicht mehr in der Liste.

**AC-05 (zu BA-09, Q-03)** – *Angebot erstellen und exportieren*
Vorbedingung: Mindestens ein Kunde und 5 Produkte sind erfasst.
Aktion: Die Anwenderin bzw. der Anwender erstellt ein Angebot mit 5 Positionen und exportiert es als PDF.
Erwartet: Das Angebot ist mit Angebotsnummer und korrekten Summen gespeichert; der PDF-Export ist in ≤ 2 Sekunden abgeschlossen (gemäß Q-03).

**AC-06 (zu BA-10)** – *Auftragsbestätigung erstellen*
Vorbedingung: Ein Angebot liegt vor.
Aktion: Die Anwenderin bzw. der Anwender erstellt eine Auftragsbestätigung mit Übernahme aller Positionen.
Erwartet: Die Auftragsbestätigung ist mit eindeutiger Nummer gespeichert und als PDF exportierbar.

**AC-07 (zu BA-11)** – *Lieferschein erstellen*
Vorbedingung: Eine Auftragsbestätigung liegt vor.
Aktion: Die Anwenderin bzw. der Anwender erstellt einen Lieferschein mit Lieferdatum.
Erwartet: Der Lieferschein ist mit eindeutiger Nummer und allen Positionsdaten gespeichert und als PDF exportierbar.

**AC-08 (zu BA-12, GR-01, GR-06)** – *Rechnung erstellen mit Pflichtangaben*
Vorbedingung: Kunde und mindestens eine Position liegen vor; letzte Rechnungsnummer = R-000123.
Aktion: Die Anwenderin bzw. der Anwender erstellt eine Rechnung ohne abweichendes Zahlungsziel.
Erwartet: Die neue Rechnung trägt die Nummer R-000124, ein Zahlungsziel von 14 Tagen und alle Pflichtangaben gemäß § 14 UStG.

**AC-09 (zu BA-13)** – *Geführte Rechnungserstellung*
Vorbedingung: Mindestens ein Kunde und ein Produkt sind im System vorhanden.
Aktion: Die Anwenderin bzw. der Anwender startet die Rechnungserstellung, wählt einen Kunden aus, erfasst eine Produktposition mit Menge, prüft Rechnungsdatum und Zahlungsziel und speichert nach Anzeige der Zusammenfassung.
Erwartet: Die Rechnung wird gespeichert; die Zusammenfassung enthält Kunde, Produktposition, Menge, Summen, Rechnungsdatum und Zahlungsziel.

**AC-10 (zu BA-14)** – *Rechnung stornieren*
Vorbedingung: Eine Rechnung im Status „offen“ existiert im System.
Aktion: Die Anwenderin bzw. der Anwender wählt die Rechnung aus und führt die Stornierung durch.
Erwartet: Die Rechnung erhält den Status „storniert“, erscheint nicht mehr in der Liste offener Rechnungen, und der Vorgang ist mit Datum protokolliert.

**AC-11 (zu Q-09)** – *Pflichtfeldhinweis korrigieren*
Vorbedingung: Die Formulare „Kunde anlegen“, „Produkt anlegen“ und „Rechnung erstellen“ sind erreichbar.
Aktion: Testpersonen versuchen in jedem Formular ohne jeweils ein Pflichtfeld zu speichern; anschließend ergänzen sie die fehlende Angabe und speichern erneut.
Erwartet: Das System verhindert jeweils zuerst das Speichern und zeigt einen Hinweis mit dem Namen des fehlenden Pflichtfelds; in mindestens 80 % der Testdurchläufe gelingt die Korrektur ohne externe Hilfe im ersten Korrekturversuch.

### 7.2 Abnahmebedingungen (Gesamtprojekt)
Das Projekt gilt als abgenommen, wenn:

- alle Akzeptanzkriterien **AC-01 bis AC-11** erfolgreich durchlaufen wurden,
- die Qualitätsanforderungen **Q-01 bis Q-09** durch entsprechende Tests bestätigt sind,
- die Abschlusspräsentation gemäß Project Charter (Meilenstein **M-07**) durch den Auftraggeber abgenommen wurde,
- die Traceability-Matrix (Anforderung ↔ Testfall) vollständig vorliegt.

---

## 8. Anhänge

### 8.1 Glossar
| Begriff | Definition |
|---|---|
| Angebot | Verbindlicher Vorschlag des Anbieters über Leistungen und Preise an einen potenziellen Kunden. |
| Auftragsbestätigung | Dokument, mit dem der Anbieter die Annahme eines Auftrags verbindlich bestätigt. |
| Lieferschein | Dokument, das die Lieferung von Waren oder Leistungen an den Kunden dokumentiert. |
| Rechnung | Dokument zur Zahlungsforderung gegenüber dem Kunden, gesetzlich geregelt durch § 14 UStG. |
| Dokumentenzyklus | Abfolge der vier Geschäftsdokumente Angebot → Auftragsbestätigung → Lieferschein → Rechnung. |
| Fakturierung | Vorgang der Rechnungsstellung. |
| GoBD | Grundsätze zur ordnungsmäßigen Führung und Aufbewahrung von Büchern, Aufzeichnungen und Unterlagen in elektronischer Form. |
| DSGVO | Datenschutz-Grundverordnung der Europäischen Union. |
| CRUD | Create, Read, Update, Delete – die vier Grundoperationen auf einem Datensatz. |
| Lastenheft | Customer Requirements Specification – beschreibt aus Sicht des Auftraggebers, **was** das System leisten soll. |

### 8.2 Referenzen
- Projektübersicht (ursprünglich Project Charter), Version 1.3
- Vorlesungsunterlagen Software Engineering 1 (SoSe 2026)
- Open-Source-Referenzsystem **Fakturama**
- § 14 UStG (Pflichtangaben einer Rechnung)
- DSGVO (EU-Verordnung 2016/679)

### 8.3 Abkürzungen
| Abkürzung | Bedeutung |
|---|---|
| BA | Benutzeranforderung (funktional) |
| Q | Qualitätsanforderung (nicht-funktional) |
| GR | Geschäftsregel |
| AC | Akzeptanzkriterium |
| IF | Schnittstelle (Interface) |
| PZ | Projektziel |
| SH | Stakeholder |
| CRS | Customer Requirements Specification (Lastenheft) |
| SRS | System Requirements Specification (Pflichtenheft) |
