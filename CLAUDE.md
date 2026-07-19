# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projekt

**Faktura** — Desktop-Fakturierungsanwendung (Einzelplatz) von Lucas Strubel; entstanden als Hochschulprojekt (SE1, TH Mannheim), wird als Portfolio-Einzelprojekt zu Produktqualität weiterentwickelt (Roadmap: `dokumentation/projekt/Projektuebersicht.md`). Java 21, Swing-GUI mit FlatLaf, Maven-Build. Alle Bezeichner, Kommentare und Spezifikationen sind auf **Deutsch** — neue Klassen, Methoden und Texte ebenfalls auf Deutsch benennen (README ist Englisch). Javadoc-Kommentare referenzieren Anforderungs-IDs aus dem Pflichtenheft (z. B. `F-12`, `GR-02`, `C-F-06`); dieses Muster beibehalten. IDs gelten je Komponente A–D; komponentenübergreifende Verweise tragen den Präfix (`A-F-12`, `C-F-06`).

## Build & Run

```bash
./mvnw compile                                        # Kompilieren
./mvnw test                                           # Alle Tests (JUnit 5)
./mvnw test -Dtest=KundenVerwaltungTest               # Einzelne Testklasse
./mvnw test -Dtest=KundenVerwaltungTest#testMethode   # Einzelne Testmethode
./mvnw verify                                         # Tests + JaCoCo + SpotBugs (wie CI)
./mvnw package                                        # Fat-JAR (spring-boot-maven-plugin repackage)
java -jar target/faktura-1.0.0.jar                    # Anwendung starten
```

## Architektur

Vier fachliche Komponenten unter `src/main/java/de/lucasstrubel/faktura/`, plus ein Querschnittspaket:

| Paket | Komponente | Verantwortung |
|-------|------------|---------------|
| `dokumente` | A | Dokumentenzyklus Angebot → Auftragsbestätigung → Lieferschein → Rechnung, Belegnummern, PDF-Export (PDFBox) |
| `produkte` | B | Produktverwaltung (CRUD, Nummernvergabe, Löschsperre) |
| `kunden` | C | Kundenverwaltung (CRUD, Nummernvergabe, Löschsperre) |
| `gui` | D | Swing-Oberfläche (HauptFenster, Panels, Dialoge, RechnungsWizard) |
| `gemeinsam` | — | Querschnitt: EreignisBus, JsonPersistenz, Csv, Exceptions |

Das Wiring erfolgt über den Spring-IoC-Container: `FakturaApplication` (@SpringBootApplication, Einstiegspunkt; baut die Swing-GUI auf dem EDT aus den Beans auf), `PersistenzKonfiguration` (@Bean-Definitionen für Repositories und Nummerngeneratoren), `FakturaEigenschaften` (@ConfigurationProperties, Präfix `faktura`, Datenverzeichnis aus `application.yml`). Services tragen `@Service` (bei mehreren Konstruktoren: `@Autowired` am vollständigen), Querschnittsklassen `@Component`. Die GUI-Panels sind (noch) keine Beans.

### Wichtige Muster

- **EreignisBus** (Observer, synchron, nur EDT): Services melden nach jeder schreibenden Operation `ereignisBus.melde(DatenBereich.KUNDEN/PRODUKTE/DOKUMENTE)`; GUI-Panels abonnieren und aktualisieren sich selbst. Kein manueller Refresh zwischen Modulen.
- **Repository-Interfaces** (`KundenRepository`, `ProduktRepository`, `DokumentRepository`) mit `Json*Repository`-Implementierungen. Schreiben erfolgt atomar über `JsonPersistenz.schreibeAtomar()` (Temp-Datei + Move).
- **NummernGeneratoren**: `Einfacher*NummernGenerator.ausRepository(...)` leitet den nächsten Zähler beim Start aus dem Bestand ab. Belegnummern haben das Format `<PRÄFIX>-<JAHR>-NNNNNN` (AN/AB/LS/R), je Typ und Jahr fortlaufend.
- **ReferenzPrüfung** (Löschsperre GR-04): `DokumentReferenzPruefung` (Komponente A) implementiert `KundenReferenzPruefung` und `ProduktReferenzPruefung`; Kunden/Produkte, die in Belegen referenziert sind, dürfen nicht gelöscht werden (`LoeschAbgelehntException`).

### Fachliche Invarianten

- Belege werden **nie gelöscht**, nur storniert — Rechnungsnummern müssen lückenlos bleiben (GR-01, F-12).
- Belege im Status `VERSENDET` oder `STORNIERT` sind unveränderlich; `Dokument.pruefeAenderbar()` wirft sonst `IllegalStateException` (GR-02, F-24). Statusfolge: `ENTWURF → OFFEN → VERSENDET / STORNIERT`.
- Belege speichern Kundenname/-anschrift und Positionspreise als **Snapshot** zum Erstellzeitpunkt; spätere Stammdatenänderungen dürfen bestehende Belege nicht verändern (C-F-06).
- Geldbeträge sind `BigDecimal` mit Scale 2, Rundung `HALF_UP`.
- Fachliche Validierungsfehler werfen `ValidierungsException` (Feldname + Meldung mit Anforderungs-ID).

### Persistenz

JSON-Dateien im Verzeichnis `daten/` (kunden.json, produkte.json, dokumente.json), nicht versioniert. `Dokument` nutzt Jackson-Polymorphie (`@JsonTypeInfo` mit Property `typ`) und Field-Visibility statt Gettern/Settern.

## Dokumentation

Unter `dokumentation/`: `anforderungen/` (Lastenheft, konsolidiertes Pflichtenheft mit Teilen A–D, Anforderungsabgleich), `tests/` (Modultestplan, -bericht), `projekt/` (Projektübersicht mit Roadmap, Präsentation), `diagramme/` (PlantUML-Quellen, gerendert via PlantUML: `java -jar tools/plantuml.jar -Playout=smetana`, kein Graphviz nötig; das Jar ist nicht versioniert (Download: plantuml.com)). Die `*.md`-Spezifikationen können manuell mit pandoc + XeLaTeX zu PDF gebaut werden (pandoc ist lokal nicht installiert).
