---
title: "Anforderungsabgleich & Traceability-Matrix"
subtitle: "Desktop-Fakturierungsanwendung"
author:
  - Lucas Strubel
version: "1.0"
lang: de-DE
---

# Anforderungsabgleich Lastenheft ↔ Implementierung

Dieses Dokument weist nach, dass die Implementierung alle Anforderungen des
[Lastenheft.md](Lastenheft.md) (v1.3) erfüllt. Es dient als Traceability-Matrix
(Anforderung ↔ Testfall ↔ Codebeleg) gemäß Abnahmebedingung Kap. 7.2 des Lastenhefts.

Status-Legende: ✅ erfüllt · 📋 organisatorischer Nachweis erforderlich

## 1. Fachliche Anforderungen (BA)

| Anf. | Inhalt | Codebeleg | Testfall | Status |
|---|---|---|---|---|
| BA-01 | Kunde anlegen (Pflicht-/Optionalfelder, eindeutige Nr.) | `KundenVerwaltungsService.legeAn`, `EinfacherKundennummernGenerator` | KundenVerwaltungTest TC-01 | ✅ |
| BA-02 | Kundendaten ändern | `KundenVerwaltungsService.aendere` | KundenVerwaltungTest TC-07 | ✅ |
| BA-03 | Kunden löschen (mit Löschsperre) | `KundenVerwaltungsService.loescheKunde` | KundenVerwaltungTest TC-09/TC-10 | ✅ |
| BA-04 | Kunden suchen/auflisten (Name + Nr.) | `JsonKundenRepository.suche/alleSortiertNachName` | KundenVerwaltungTest TC-12/TC-13 | ✅ |
| BA-05 | Produkt anlegen (Preis, Steuersatz, eindeutige Nr.) | `ProduktVerwaltungsService.legeAn` | ProduktVerwaltungTest TC-01 | ✅ |
| BA-06 | Produktdaten ändern (Snapshot in Belegen) | `ProduktVerwaltungsService.aendere` | ProduktVerwaltungTest TC-06, DokumentzyklusTest TC-11 | ✅ |
| BA-07 | Produkte löschen (mit Löschsperre) | `ProduktVerwaltungsService.loescheProdukt` | ProduktVerwaltungTest TC-08/TC-09 | ✅ |
| BA-08 | Produkte suchen/auflisten (Bezeichnung + Nr.) | `JsonProduktRepository.suche/alleSortiertNachBezeichnung` | ProduktVerwaltungTest TC-11/TC-12 | ✅ |
| BA-09 | Angebot erstellen + PDF | `StandardDokumentService.erstelleAngebot`, `PdfBoxPdfExporter` | DokumentzyklusTest TC-13 (analog) | ✅ |
| BA-10 | Auftragsbestätigung erstellen + PDF | `StandardDokumentService.erstelleAuftragsbestaetigung` | DokumentzyklusTest TC-10 | ✅ |
| BA-11 | Lieferschein erstellen + PDF | `StandardDokumentService.erstelleLieferschein` | DokumentzyklusTest TC-10 (Folgebeleg) | ✅ |
| BA-12 | Rechnung erstellen (Nr., §14 UStG, Summen, Ziel) | `StandardDokumentService.erstelleRechnung` | DokumentzyklusTest TC-13 | ✅ |
| BA-13 | Geführte Rechnungserstellung (Wizard) | `RechnungsWizardController/-Dialog/-Model` | OberflaechenControllerTest TC-01…TC-08 | ✅ |
| BA-14 | Rechnung stornieren (Datum **und Benutzer**) | `Rechnung.storniere(datum, benutzer)`, `StandardDokumentService.SYSTEM_BENUTZER` | DokumentzyklusTest TC-09, OberflaechenControllerTest TC-10/TC-11 | ✅ |

## 2. Geschäftsregeln (GR)

| Anf. | Inhalt | Codebeleg | Testfall | Status |
|---|---|---|---|---|
| GR-01 | Lückenlose Rechnungsnummern | `EinfacherBelegnummernGenerator.ausRepository` | DokumentzyklusTest TC-04/TC-05 | ✅ |
| GR-02 | Unveränderlichkeit versendeter Dokumente | `Dokument.pruefeAenderbar` | DokumentzyklusTest TC-08 | ✅ |
| GR-03 | Steuerberechnung (Snapshot) | `Dokumentposition`, `Dokument.berechneSummen` | DokumentzyklusTest TC-01…TC-03, TC-11 | ✅ |
| GR-04 | Referenzielle Integrität Kunden | `KundenVerwaltungsService.loescheKunde` | KundenVerwaltungTest TC-10 | ✅ |
| GR-05 | Dokumentenzyklus-Konsistenz (Rückreferenz) | `StandardDokumentService.erzeugeFolgebeleg` | DokumentzyklusTest TC-10 | ✅ |
| GR-06 | Standard-Zahlungsziel 14 Tage | `StandardDokumentService.STANDARD_ZAHLUNGSZIEL_TAGE` | DokumentzyklusTest TC-06/TC-07 | ✅ |

## 3. Qualitätsanforderungen (Q)

| Anf. | Inhalt | Codebeleg / Nachweis | Testfall | Status |
|---|---|---|---|---|
| Q-01 | Referenzgröße 5.000 Kunden/Produkte | In-Memory-Repositories, Seeding im Test | PerformanceTest (Seeding) | ✅ |
| Q-02 | Suche/Auflistung ≤ 1 s | `JsonKundenRepository`/`JsonProduktRepository.suche` | PerformanceTest `q02Suche` | ✅ |
| Q-03 | PDF-Erstellung ≤ 2 s (50 Positionen) | `PdfBoxPdfExporter` | PerformanceTest `q03PdfErstellung` | ✅ |
| Q-04 | Anwendungsstart ≤ 5 s | Laden der drei JSON-Repositories | PerformanceTest `q04Anwendungsstart` | ✅ |
| Q-05 | Ersterstellung Rechnung < 10 min (≥5 Personen) | Wizard `RechnungsWizardController` | Usability-Test (organisatorisch) | 📋 |
| Q-06 | 100 % lokale Datenhaltung | nur lokales Dateisystem; `Desktop.mail()` = optionale IF-03 | Code-Inspektion / Netzwerk-Monitoring | ✅ |
| Q-07 | Unveränderlichkeit versendeter Rechnungen | `Dokument.pruefeAenderbar` | DokumentzyklusTest TC-08 | ✅ |
| Q-08 | Vollexport Stamm- **und Bewegungsdaten** ≤ 30 s | `KundenCsvExport`, `ProduktCsvExport`, **`DokumentCsvExport`** | DokumentCsvExportTest, PerformanceTest `q08Datenexport` | ✅ |
| Q-09 | Pflichtfeldhinweis (≥80 % Korrektur) | `ValidierungsException`, `MeldungsAnzeige`, Wizard-Validierung | OberflaechenControllerTest TC-03…TC-05; Usability-Test | ✅ (Code) / 📋 (Usability) |

## 4. Akzeptanzkriterien (AC)

| Anf. | bezogen auf | Testfall | Status |
|---|---|---|---|
| AC-01 | BA-01, BA-04 | KundenVerwaltungTest TC-01, TC-12 | ✅ |
| AC-02 | BA-02, BA-03, GR-04 | KundenVerwaltungTest TC-07, TC-10 | ✅ |
| AC-03 | BA-05, BA-06, GR-02 | ProduktVerwaltungTest TC-06, DokumentzyklusTest TC-11 | ✅ |
| AC-04 | BA-07, BA-08 | ProduktVerwaltungTest TC-08/TC-09, TC-11/TC-12; PerformanceTest `q02Suche` | ✅ |
| AC-05 | BA-09, Q-03 | DokumentzyklusTest (Angebot), PerformanceTest `q03PdfErstellung` | ✅ |
| AC-06 | BA-10 | DokumentzyklusTest TC-10 | ✅ |
| AC-07 | BA-11 | DokumentzyklusTest TC-10 (Folgebeleg) | ✅ |
| AC-08 | BA-12, GR-01, GR-06 | DokumentzyklusTest TC-04, TC-06, TC-13 | ✅ |
| AC-09 | BA-13 | OberflaechenControllerTest TC-02, TC-07, TC-08 | ✅ |
| AC-10 | BA-14 | DokumentzyklusTest TC-09, OberflaechenControllerTest TC-10/TC-11 | ✅ |
| AC-11 | Q-09 | OberflaechenControllerTest TC-03…TC-05; Usability-Test | ✅ (Code) / 📋 (Usability) |

## 5. Offene organisatorische Punkte (Abnahmebedingung Kap. 7.2)

Folgende Punkte sind nicht durch Code/automatisierte Tests abdeckbar und vor der
Endabnahme organisatorisch durchzuführen bzw. zu dokumentieren:

- **Q-05 / AC-11 – Usability-Tests** mit mindestens 5 Testpersonen
  (Ersterstellung einer Rechnung < 10 min; Pflichtfeldkorrektur ≥ 80 %).
- **M-07 – Abschlusspräsentation** und Abnahme durch den Auftraggeber.

Alle übrigen Anforderungen (BA-01…BA-14, GR-01…GR-06, Q-01…Q-04, Q-06…Q-08
sowie AC-01…AC-10) sind durch Code und automatisierte JUnit-Tests belegt.
