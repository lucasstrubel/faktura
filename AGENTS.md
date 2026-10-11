# Repository Guidelines

## Projektstruktur und Architektur

Faktura ist eine lokale Desktop-Anwendung mit Java 21, JavaFX, Spring Boot und SQLite.

- `src/main/java/de/lucasstrubel/faktura/`: Fachpakete `dokumente`, `kunden`, `produkte`, `firma`, Oberfläche `gui` und gemeinsame Infrastruktur `gemeinsam`.
- `src/main/resources/`: FXML, CSS, Symbole, Schriften und `application.yml`; Flyway-SQL-Migrationen unter `db/migration/`, Java-Migrationen unter `src/main/java/db/migration/`.
- `src/test/java/`: Tests spiegeln die Quellpakete; `src/test/resources/` enthält Testkonfiguration.
- `dokumentation/`: Anforderungen, Testpläne, Diagramme und PDFs; `tools/`: Prüf- und Dokumentationsskripte; `installer/`: Installationsgrafiken. Build-Ergebnisse liegen in `target/`.

Fachlogik gehört in Services und GUI-unabhängige Controller; Datenzugriffe laufen über Repository-Schnittstellen.

## Build, Tests und lokale Entwicklung

JDK 21 verwenden; die Wrapper laden Maven selbst. In PowerShell:

```powershell
.\mvnw.cmd compile                         # Quellcode kompilieren
.\mvnw.cmd test                            # Standard-Testlauf
.\mvnw.cmd test "-Dtest=KundenVerwaltungTest" # Einzelne Testklasse
.\mvnw.cmd verify                          # Tests, JaCoCo und SpotBugs
.\mvnw.cmd package                         # Ausführbares JAR bauen
java -jar target/faktura-3.1.0-rc.1.jar      # Anwendung starten
```

Unter Unix/Git Bash `./mvnw` verwenden. Den JAR-Dateinamen bei Versionsänderungen an `pom.xml` anpassen.

## Codestil und Benennung

Vier Leerzeichen einrücken; öffnende Java-Klammern stehen auf derselben Zeile. Klassen verwenden `PascalCase`, Methoden und Felder `camelCase`, Konstanten `UPPER_SNAKE_CASE`. Bezeichner, Kommentare, Oberflächentexte und Dokumentation auf Deutsch verfassen. Anforderungs-IDs wie `A-F-12` in Javadoc beibehalten. Dateien als UTF-8 speichern und `.gitattributes` beachten. Kein automatischer Formatter ist konfiguriert; bestehenden Stil übernehmen. SpotBugs-Ausnahmen stehen in `spotbugs-exclude.xml`.

## Testregeln

JUnit Jupiter verwenden, Testklassen `*Test.java` nennen und Testfall-IDs in `@DisplayName` aufnehmen. Services möglichst direkt mit Test-Repositories und `@TempDir` prüfen. Neue Fälle in `dokumentation/tests/Modultestplan.md` ergänzen.

`verify` verlangt mindestens 85 % Instruktions- und 70 % Zweigabdeckung; ausgewählte JavaFX-Klassen sind ausgenommen. Berichte stehen unter `target/site/jacoco/`. GUI-Regressionen separat mit `.\mvnw.cmd test "-Dfaktura.gui.tests=true"` bei verfügbarer grafischer Umgebung aktivieren; Produktions-Performance-Tests mit `.\mvnw.cmd test "-Dfaktura.performance=true"`.

## Commits und Pull Requests

Die Historie nutzt überwiegend kurze deutsche Beschreibungen, gelegentlich mit Präfixen wie `Build:` oder `Oberflaeche:`. Änderungen thematisch bündeln. Pull Requests sollen Verhaltensänderungen, relevante Issues/Anforderungs-IDs und Prüfergebnisse beschreiben; bei Oberflächenänderungen Screenshots ergänzen. Vor Einreichung `verify` ausführen. Bei Änderungen an Dokumentationsquellen zugehörige versionierte PDFs mit `tools/dokumentation-bauen.ps1` aktualisieren.

## Persistenz und Konfiguration

Bestehende Flyway-Migrationen unverändert lassen; neue als `V<n>__beschreibung.sql` ergänzen. Unveränderliche Belege, historische Snapshots und transaktionale Nummernvergabe bewahren. Entwicklungsdaten über `--faktura.daten-verzeichnis=<pfad>` isolieren. Datenbanken, Logs und echte Kundendaten niemals einchecken.
