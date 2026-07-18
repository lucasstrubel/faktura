# Faktura

**A lightweight desktop invoicing application for freelancers and micro-businesses — 100 % local, no cloud, no subscription.**

Faktura manages customers and products and covers the full German commercial document
cycle — *Angebot* (quote) → *Auftragsbestätigung* (order confirmation) → *Lieferschein*
(delivery note) → *Rechnung* (invoice) — including PDF export, gapless invoice
numbering (GoBD), and immutability of sent documents.

> **Origin:** This project started as a software engineering course project at
> TH Mannheim (full V-model process: requirements specification, design, implementation,
> module testing, acceptance). I am now redesigning and extending it solo into a
> production-quality application — see the [roadmap](#roadmap) below.

## Features (v1.0)

- **Customer & product management** — CRUD with system-assigned numbers
  (`K-000017`, `P-000042`), full-text search, and delete protection: master data
  referenced by documents cannot be deleted (referential integrity without a database)
- **Document cycle** — each document can be derived from its predecessor (data
  carry-over + back-reference); prices and customer address are stored as immutable
  snapshots, so later master-data changes never alter existing documents
- **Guided invoice creation** — 5-step wizard (customer → positions → dates →
  summary → save)
- **Compliance by design** — gapless invoice numbers per year (`R-2026-000124`),
  documents are never deleted (only cancelled), sent documents reject every
  modification (GoBD), § 14 UStG mandatory invoice fields, all money handled as
  `BigDecimal` scale 2
- **PDF export** (Apache PDFBox) and **CSV export** (UTF-8, open format)
- **Local-only persistence** — JSON files with atomic writes; no network access (DSGVO)

## Tech stack

Java 21 · Swing + FlatLaf · Maven · Jackson (JSON) · Apache PDFBox · JUnit 5 (71 tests)

## Build & run

```bash
./mvnw test                       # run all tests (JUnit 5)
./mvnw package                    # build fat JAR (maven-shade-plugin)
java -jar target/faktura-1.0.0.jar
```

Application data is stored locally as JSON under `daten/` (git-ignored).

## Architecture

Four domain components plus a shared cross-cutting package, wired via a composition
root in `Main.java`:

| Package | Component | Responsibility |
|---------|-----------|----------------|
| `dokumente` | A | Document cycle, document numbers, PDF export |
| `produkte`  | B | Product management (CRUD, numbering, delete protection) |
| `kunden`    | C | Customer management (CRUD, numbering, delete protection) |
| `gui`       | D | Swing UI (main window, panels, dialogs, invoice wizard) |
| `gemeinsam` | — | Event bus (observer), JSON persistence, CSV helper, exceptions |

Key patterns: repository interfaces with JSON implementations and atomic file writes,
number generators derived from the persisted stock, an event bus decoupling services
from UI refresh, and cross-component delete protection via reference-check interfaces.

Source: [`src/main/java/de/lucasstrubel/faktura/`](src/main/java/de/lucasstrubel/faktura/) ·
Tests: [`src/test/java/de/lucasstrubel/faktura/`](src/test/java/de/lucasstrubel/faktura/)

## Roadmap

Turning the course project into a customer-ready product, step by step:

- [ ] **Quality baseline** — stricter input validation (postal code, VAT ID, email),
      logging (SLF4J/Logback), CI with GitHub Actions, coverage & static analysis
- [ ] **Spring Boot** — IoC container replacing manual wiring, configuration via
      `application.yml`, Spring application events
- [ ] **SQLite persistence** — Spring JDBC + Flyway migrations behind the existing
      repository interfaces (JSON kept as import/export format)
- [ ] **JavaFX UI** — complete rewrite (FXML, AtlantaFX theme, FxWeaver) with live
      inline form validation
- [ ] **Company profile & settings** — configurable letterhead, bank details, logo
- [ ] **E-invoicing** — ZUGFeRD / XRechnung (EN 16931) export, mandatory for German
      B2B invoicing since 2025
- [ ] **Native installer** — Windows installer via jpackage, published as GitHub
      Releases

## Documentation

Full German software-engineering documentation under [`dokumentation/`](dokumentation/)
— written and maintained as a showcase of a complete specification-driven process:

- [`anforderungen/`](dokumentation/anforderungen/) — *Lastenheft* (customer
  requirements), consolidated *Pflichtenheft* (system requirements specification,
  parts A–D), requirements traceability matrix
- [`tests/`](dokumentation/tests/) — module test plan and test report (71/71 passed)
- [`projekt/`](dokumentation/projekt/) — project overview with roadmap, final
  presentation slides
- [`diagramme/`](dokumentation/diagramme/) — UML class and sequence diagrams
  (PlantUML sources + rendered PNGs)

The Markdown specifications can be rendered to PDF with pandoc + XeLaTeX; diagrams
are rendered with [PlantUML](https://plantuml.com/download) (`java -jar plantuml.jar -Playout=smetana`, no Graphviz needed); place the jar under `tools/` (git-ignored).

## License & author

Developed by **Lucas Strubel**. License: to be added before publication.
