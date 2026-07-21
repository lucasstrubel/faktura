# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projekt

**Faktura** — Desktop-Fakturierungsanwendung (Einzelplatz) von Lucas Strubel; entstanden als Hochschulprojekt (SE1, TH Mannheim), wird als Portfolio-Einzelprojekt zu Produktqualität weiterentwickelt (Roadmap: `dokumentation/projekt/Projektuebersicht.md`). Java 21, JavaFX-GUI (FXML + AtlantaFX), Spring Boot, SQLite, Maven-Build. Alle Bezeichner, Kommentare und Spezifikationen sind auf **Deutsch** — neue Klassen, Methoden und Texte ebenfalls auf Deutsch benennen (README ist Englisch). Javadoc-Kommentare referenzieren Anforderungs-IDs aus dem Pflichtenheft (z. B. `F-12`, `GR-02`, `C-F-06`); dieses Muster beibehalten. IDs gelten je Komponente A–D; komponentenübergreifende Verweise tragen den Präfix (`A-F-12`, `C-F-06`).

## Build & Run

```bash
./mvnw compile                                        # Kompilieren
./mvnw test                                           # Alle Tests (JUnit 5)
./mvnw test -Dtest=KundenVerwaltungTest               # Einzelne Testklasse
./mvnw test -Dtest=KundenVerwaltungTest#testMethode   # Einzelne Testmethode
./mvnw verify                                         # Tests + JaCoCo + SpotBugs (wie CI)
./mvnw package                                        # Fat-JAR (spring-boot-maven-plugin repackage)
java -jar target/faktura-3.0.0.jar                    # Anwendung starten
```

## Architektur

Vier fachliche Komponenten unter `src/main/java/de/lucasstrubel/faktura/`, plus ein Querschnittspaket:

| Paket | Komponente | Verantwortung |
|-------|------------|---------------|
| `dokumente` | A | Dokumentenzyklus Angebot → Auftragsbestätigung → Lieferschein → Rechnung, Belegnummern, PDF-Export (PDFBox), E-Rechnung EN 16931 (`ERechnungExport`, Mustang) |
| `produkte` | B | Produktverwaltung (CRUD, Nummernvergabe, Löschsperre) |
| `kunden` | C | Kundenverwaltung (CRUD, Nummernvergabe, Löschsperre) |
| `gui` | D | JavaFX-Oberfläche (FXML-Ansichten unter `src/main/resources/fxml/`, Ansicht-Controller, modale Dialoge, RechnungsWizard, Seitennavigation, Übersicht) |
| `gemeinsam` | — | Querschnitt: EreignisBus, JdbcNummernkreis, JsonPersistenz, Csv, Validierung, Datensicherung, Exceptions |
| `firma` | — | Firmenprofil des Ausstellers (Einstellungen-Tab; Briefkopf, Zahlungshinweis, E-Rechnung) |

Das Wiring erfolgt über den Spring-IoC-Container: `FakturaApplication` (@SpringBootApplication) startet die JavaFX-Laufzeit `FxAnwendung` (Application; `init()` fährt den Container hoch, `start()` lädt die Oberfläche). FXML-Ansichten lädt der `FxmlLader` mit Spring-Controller-Factory (`createBean` — Ansicht-Controller sind KEINE registrierten Beans, bekommen aber Konstruktor-Injektion). `PersistenzKonfiguration` definiert Repositories/Generatoren als Beans, `FakturaEigenschaften` (@ConfigurationProperties `faktura`) das Datenverzeichnis. Services tragen `@Service` (bei mehreren Konstruktoren: `@Autowired` am vollständigen), Querschnittsklassen `@Component`.

### Wichtige Muster

- **Ereignisse** (Observer, synchron, FX-Thread): Services publizieren nach jeder schreibenden Operation `DatenGeaendertEreignis(DatenBereich.…)` über den `ApplicationEventPublisher`; der `EreignisBus` benachrichtigt die abonnierten Ansichten. Die Brücke ist `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)` — nach einem Rollback darf die Oberfläche keinen Datensatz zeigen, den es nicht gibt. Kein manueller Refresh zwischen Modulen.
- **Repository-Interfaces** (`KundenRepository`, `ProduktRepository`, `DokumentRepository`) mit `Jdbc*Repository`-Implementierungen (SQLite, primär) und `Json*Repository` (Import/Backup; schreibt atomar über `JsonPersistenz.schreibeAtomar()`).
- **NummernGeneratoren**: Im Datenbankbetrieb `Jdbc*NummernGenerator` über `JdbcNummernkreis` (Tabelle `nummernkreis`, Flyway V3) — die Vergabe läuft in **derselben Transaktion** wie das Speichern (`@Transactional` auf den Service-Methoden), damit ein Fehlschlag keine Nummer verbraucht (GR-01). Der Startwert wird beim ersten Zugriff träge aus dem Bestand abgeleitet. Die `Einfacher*NummernGenerator` (Speicherzähler) bleiben für Modultests und den JSON-Betrieb. Belegnummern haben das Format `<PRÄFIX>-<JAHR>-NNNNNN` (AN/AB/LS/R), je Typ und Jahr fortlaufend; das Format liegt allein in `Belegnummernformat`.
- **Hintergrundausführung**: Alles, was dauern kann (PDF, E-Rechnung, CSV, Datensicherung, Druck, Mail), läuft über `HintergrundAufgaben.starte(...)` — nie direkt im FX-Ereignis-Handler. Fehler laufen über `FxMeldung.zuMeldung(...)`, die einzige Stelle mit der Ausnahme-Zuordnung.
- **Oberflächen-Bausteine**: Statusabzeichen, Leerzustände und Spaltenformate kommen aus `Bausteine`; Erfolgsmeldungen über `Benachrichtigung` (nicht blockierend), Fehler und Rückfragen bleiben modale Dialoge. Farben ausschließlich über AtlantaFX-Variablen (`-color-…`), damit heller und dunkler Modus ohne Zusatzregeln tragen.
- **ReferenzPrüfung** (Löschsperre GR-04): `DokumentReferenzPruefung` (Komponente A) implementiert `KundenReferenzPruefung` und `ProduktReferenzPruefung`; Kunden/Produkte, die in Belegen referenziert sind, dürfen nicht gelöscht werden (`LoeschAbgelehntException`).

### Fachliche Invarianten

- Belege werden **nie gelöscht**, nur storniert — Rechnungsnummern müssen lückenlos bleiben (GR-01, F-12).
- Belege im Status `VERSENDET` oder `STORNIERT` sind unveränderlich; `Dokument.pruefeAenderbar()` wirft sonst `IllegalStateException` (GR-02, F-24). Statusfolge: `ENTWURF → OFFEN → VERSENDET / STORNIERT`.
- Belege speichern Kundenname/-anschrift und Positionspreise als **Snapshot** zum Erstellzeitpunkt; spätere Stammdatenänderungen dürfen bestehende Belege nicht verändern (C-F-06).
- Geldbeträge sind `BigDecimal` mit Scale 2, Rundung `HALF_UP`.
- Fachliche Validierungsfehler werfen `ValidierungsException` (Feldname + Meldung mit Anforderungs-ID).

### Persistenz

SQLite-Datenbank `daten/faktura.db` (nicht versioniert) über Spring JDBC (`Jdbc*Repository`); das Schema verwaltet Flyway (`src/main/resources/db/migration`, Migration läuft programmatisch im `dataSource`-Bean — Boot-4-Auto-Config greift hier nicht). Die Verbindung läuft mit WAL-Journalmodus, `busy_timeout` und aktiver Fremdschlüsselprüfung; die Datensicherung zieht den Abzug über `VACUUM INTO` (eine Dateikopie wäre bei offenen Verbindungen inkonsistent). Belege liegen als Single-Table-Vererbung mit Diskriminatorspalte `typ`; Beträge als TEXT (BigDecimal-verlustfrei). Beim Laden versendeter/stornierter Belege wird der Status **zuletzt** gesetzt (GR-02-Prüfung). Die `Json*Repository`-Klassen bleiben für die einmalige Übernahme (`JsonDatenUebernahme`, nur bei leerer DB) und als Backup-Format erhalten; `Dokument` nutzt dafür Jackson-Polymorphie (`@JsonTypeInfo` mit Property `typ`). Die Nummerngeneratoren werden als Beans **nach** der Übernahme initialisiert (Bean-Abhängigkeit in `PersistenzKonfiguration`).

## Dokumentation

Unter `dokumentation/`: `anforderungen/` (Lastenheft, konsolidiertes Pflichtenheft mit Teilen A–D, Anforderungsabgleich), `tests/` (Modultestplan, -bericht), `projekt/` (Projektübersicht mit Roadmap, Präsentation), `diagramme/` (PlantUML-Quellen, gerendert via PlantUML: `java -jar tools/plantuml.jar -Playout=smetana`, kein Graphviz nötig; das Jar ist nicht versioniert (Download: plantuml.com)). Die `*.md`-Spezifikationen können manuell mit pandoc + XeLaTeX zu PDF gebaut werden (pandoc ist lokal nicht installiert).
