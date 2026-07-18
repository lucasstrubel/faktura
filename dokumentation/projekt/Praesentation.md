---
marp: true
theme: default
paginate: true
lang: de
header: 'Faktura · Desktop-Fakturierungsanwendung'
style: |
  @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;600;700&family=Lora:wght@400;500;700&display=swap');
  section {
    font-family: 'Lora', 'Georgia', serif;
    font-size: 26px;
    line-height: 1.6;
    padding: 64px 80px;
    color: #141413;
    background: #faf9f5;
  }
  h1, h2 {
    font-family: 'Poppins', 'Arial', sans-serif;
    letter-spacing: -0.02em;
    font-weight: 600;
  }
  h1 { color: #141413; }
  h2 {
    color: #141413;
    font-size: 36px;
    margin: 0 0 0.5em;
    padding: 0 0 0.35em;
    border: none;
    border-bottom: 2px solid #d97757;
  }
  h3 {
    font-family: 'Poppins', 'Arial', sans-serif;
    color: #d97757;
    font-size: 16px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin: 1.2em 0 0.3em;
  }
  strong { color: #141413; }
  a { color: #6a9bcc; text-decoration: none; }
  code {
    font-family: 'Consolas', 'Cascadia Code', monospace;
    background: #e8e6dc;
    color: #141413;
    padding: 2px 6px;
    border-radius: 3px;
    font-size: 0.9em;
  }
  table { font-size: 22px; border-collapse: collapse; width: 100%; }
  th {
    background: transparent;
    color: #b0aea5;
    font-family: 'Poppins', 'Arial', sans-serif;
    font-size: 14px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    padding: 8px 12px;
    text-align: left;
    border-bottom: 2px solid #d97757;
  }
  td { padding: 10px 12px; border-bottom: 1px solid #e8e6dc; }
  tr:nth-child(even) { background: transparent; }
  blockquote {
    background: transparent;
    color: #b0aea5;
    font-style: italic;
    border: none;
    border-left: 3px solid #d97757;
    padding: 8px 18px;
    margin: 1em 0;
    border-radius: 0;
  }
  section.lead {
    justify-content: center;
    text-align: center;
    background: #141413;
    color: #faf9f5;
  }
  section.lead h1 {
    color: #faf9f5;
    font-size: 52px;
    font-weight: 700;
    letter-spacing: -0.03em;
    margin-bottom: 0.2em;
    border: none;
  }
  section.lead h3 {
    color: #d97757;
    font-size: 14px;
    letter-spacing: 0.12em;
  }
  section.lead strong { color: #faf9f5; }
  section.divider {
    background: #faf9f5;
    color: #141413;
    justify-content: center;
    text-align: center;
  }
  section.divider h1 {
    color: #141413;
    font-size: 48px;
    font-weight: 700;
    border: none;
    margin-bottom: 0.2em;
  }
  section.divider h2 { color: #141413; border: none; }
  section.divider h3 { color: #d97757; }
  footer, header {
    color: #b0aea5;
    font-family: 'Poppins', 'Arial', sans-serif;
    font-size: 14px;
  }
---

<!-- _class: lead -->
<!-- _header: '' -->
<!-- _footer: '' -->
<!-- _paginate: false -->

# Desktop-Fakturierungsanwendung

### Schlanke, lokal betriebene Fakturierung für Kleinstunternehmen

---

## Agenda

1. **Einleitung** — Kontext & Ziel, Architektur
2. **Das Programm**
   - Komponente C — Kundenverwaltung
   - Komponente B — Produktverwaltung
   - Komponente A — Dokumentenzyklus
   - Komponente D — Programmoberfläche
3. **Das Entwicklungsprojekt**
   - Ergebnisse der Modultestpläne
   - Wo wurde KI eingesetzt – und wie gut?
   - Was würden wir nächstes Mal anders machen?

---

## Projektorganisation

Vier fachliche Komponenten, arbeitsteilig spezifiziert und implementiert:

| Komponente | Verantwortung |
|---|---|
| **A** | Prozess / Dokumentenzyklus |
| **B** | Produktverwaltung |
| **C** | Kundenverwaltung |
| **D** | Programmoberfläche |

Querschnitt (`gemeinsam`): EreignisBus, JSON-Persistenz, CSV-Hilfe – von allen Komponenten genutzt.

---

## Kontext & Ziel

**Problem:** SaaS-Fakturierung kostet laufend Lizenzgebühren; Open-Source-Alternativen (z. B. *Fakturama*) sind im Betrieb anspruchsvoll.

**Unsere Lösung:** schlanke Desktop-Anwendung mit voller Datensouveränität – ohne Cloud.

| Ziel | Inhalt |
|---|---|
| **PZ-01** | Digitale Verwaltung von Kunden- & Produktstammdaten (CRUD) |
| **PZ-02** | Vollständiger Dokumentenzyklus: Angebot → AB → Lieferschein → Rechnung |
| **PZ-03** | Bedienbar ohne Vorkenntnisse (Rechnung in < 10 Min) |
| **PZ-04** | 100 % lokale, DSGVO-konforme Datenhaltung |

*Nichtziele:* Mehrbenutzer/Netzwerk, vollständige Buchhaltung, E-Rechnung (ZUGFeRD/XRechnung).

---

## Architektur im Überblick

**Vier Fachmodule + gemeinsame Infrastruktur**, manuelle Dependency Injection in `Main.java` (kein Framework).

- **Repository-Interfaces** je Domäne mit JSON-Implementierungen; atomare Schreibvorgänge (`JsonPersistenz.schreibeAtomar`)
- **EreignisBus** (Observer): Services melden Datenänderungen, GUI-Panels aktualisieren sich selbst
- **Snapshot-Prinzip** in Belegen + **Löschsperren** (referenzielle Integrität)

---

<!-- _class: divider -->
<!-- _header: '' -->
<!-- _paginate: false -->

# Das Programm
### Vier Komponenten

---

## Komponente C — Kundenverwaltung

### Ziel *(Lasten-/Pflichtenheft)*
Kundenstammdaten anlegen, ändern, suchen, löschen *(BA-01–04)* · eindeutige Kundennummer · **keine Löschung bei verknüpften Dokumenten** *(GR-04)*

### Technische Umsetzung
- **Referenzielle Integrität** ohne Datenbank: Löschsperre über eine Referenzprüfung
- Automatische Nummernvergabe & Pflichtfeld-/E-Mail-Validierung
  <small>`KundenRepository` · `EinfacherKundennummernGenerator` · `KundenReferenzPruefung`</small>

### Live-Demo
Kunde anlegen → automatische Nummer · suchen / sortieren · ändern · löschen

---

## Komponente B — Produktverwaltung

### Ziel *(Lasten-/Pflichtenheft)*
Produkte anlegen, ändern, suchen, löschen *(BA-05–08)* · Nettopreis + Steuersatz · **nur gültige Eingaben**, Löschsperre

### Technische Umsetzung
- **Validierung** lehnt negative Preise & unzulässige Steuersätze ab; Produktnummer unveränderlich
- Gleiches Repository-/Generator-Muster wie Komponente C
  <small>`ProduktRepository` · `EinfacherProduktnummernGenerator` · `ProduktReferenzPruefung`</small>

### Live-Demo
Produkt anlegen → `P-000042` · neg. Preis & Steuersatz `0.15` **abgelehnt** · ändern · suchen · Löschsperre

---

## Komponente A — Prozess / Dokumentenzyklus

### Ziel *(Lasten-/Pflichtenheft)*
Angebot → Auftragsbestätigung → Lieferschein → Rechnung *(BA-09–12)* · lückenlose Belegnummern *(GR-01)* · Steuer & Zahlungsziel · § 14 UStG · Storno *(BA-14)*

### Technische Umsetzung
- **Snapshot-Prinzip**: alte Rechnungen bleiben preisstabil, auch wenn sich Produktpreise ändern *(GR-03)*
- **Lückenlose** Belegnummern (GoBD) · Folgebeleg übernimmt Daten + Rückreferenz *(GR-05)*
  <small>`DokumentRepository` · `BelegnummernGenerator` · `PdfBoxPdfExporter`</small>

### Live-Demo
Angebot erstellen → Folgebeleg zu Rechnung → **PDF-Export**

---

## Komponente D — Programmoberfläche

### Ziel *(Lasten-/Pflichtenheft)*
Geführte Rechnungserstellung *(BA-13)* · Navigation, Pflichtfeldhinweise *(Q-09)*, Statusanzeige · bedienbar **ohne Vorkenntnisse** *(PZ-03)*

### Technische Umsetzung
- **EreignisBus (Observer)**: Panels aktualisieren sich automatisch nach Datenänderung
- Fachlogik strikt getrennt — GUI ruft nur die Services der Komponenten A–C
  <small>Swing + FlatLaf · `HauptFenster` · `RechnungsWizardController`</small>

### Live-Demo
**Wizard**: Kunde → Positionen → Bestätigen → Zusammenfassung → Speichern · Statusfilter „offen" · Pflichtfeldhinweis · PDF-/CSV-Export

---

<!-- _class: divider -->
<!-- _header: '' -->
<!-- _paginate: false -->

# Das Entwicklungsprojekt
### Modultests · KI-Einsatz · Lessons Learned

---

## Ergebnisse der Modultestpläne

### 71 / 71 Testfälle bestanden — 100 %, 0 Fehler

| Bereich | Testfälle | Ergebnis |
|---|---|---|
| A — Dokumentenzyklus | 18 | ✅ bestanden |
| B — Produktverwaltung | 14 | ✅ bestanden |
| C — Kundenverwaltung | 14 | ✅ bestanden |
| D — Programmoberfläche | 15 | ✅ bestanden |
| Infrastruktur | 6 | ✅ bestanden |
| Performance | 4 | ✅ Schranken eingehalten |
| **Gesamt** | **71** | **100 % bestanden** |

<small>Deterministische JUnit-5-Tests, Nachbarkomponenten als Stubs/Mocks. Traceability **Anforderung → Code (`@DisplayName`) → Testfall**; spezifiziert in `Modultestplan.md`, nachgewiesen in `Anforderungsabgleich.md`.</small>

> Alle durch Code/Tests belegbaren Anforderungen ✅ — offen bleiben nur organisatorische Usability-Tests (Q-05 / AC-11).

---

## Wo wurde KI eingesetzt – und wie gut?

**Eingesetzt für**
- **Dokumentation:** Entwürfe für Lasten-/Pflichtenhefte, Modultestplan, Traceability-Matrix
- **Code:** Boilerplate der Repository-/Service-Schicht, JSON-Persistenz, CSV-Export, JUnit-Testgerüste, Verwendung von Design-Patterns, Optimierungen
- **Diagramme & Tooling:** PlantUML-Quellen, Maven-Konfiguration, pandoc-/PDF-Workflow

**Bewertung**
- 👍 Hohes Tempo bei Produktion, Tests & Doku; konsistente deutsche Fachsprache; schnelle Iteration nach Feedback
- 👎 Vorschläge teils zu generisch

---

## Was würden wir nächstes Mal anders machen?

- **Früher integrieren:** modulübergreifende Tests nicht erst spät im V-Modell
- **Schnittstellen-Verträge** zwischen den Komponenten A–D verbindlich zu Projektbeginn festlegen
- **Kleinere, häufigere Commits** + einheitliche Anforderungen an Commits
- **KI-Output gezielter prüfen** statt übernehmen – besonders bei Fachlogik
- Aufgaben-/Lastverteilung über die Git-History früh sichtbar machen

---

<!-- _class: lead -->
<!-- _header: '' -->
<!-- _paginate: false -->

# Vielen Dank!
