package de.lucasstrubel.faktura.gemeinsam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Datensicherung des lokalen Bestands (Q-06, IF-01): packt die
 * SQLite-Datenbank und die JSON-Sicherungsdateien des Datenverzeichnisses
 * in eine ZIP-Datei. Logdateien werden nicht gesichert.
 */
public final class Datensicherung {

    private static final Logger LOG = LoggerFactory.getLogger(Datensicherung.class);

    private static final List<String> ENDUNGEN = List.of(".db", ".json");

    private Datensicherung() {
    }

    /** Schreibt die Sicherung; vorhandene Zieldateien werden überschrieben. */
    public static void erstelle(Path datenVerzeichnis, Path zielDatei) {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zielDatei));
             var dateien = Files.list(datenVerzeichnis)) {
            int anzahl = 0;
            for (Path datei : dateien.sorted().toList()) {
                Path dateiname = datei.getFileName();
                if (dateiname == null) {
                    continue;
                }
                String name = dateiname.toString();
                if (ENDUNGEN.stream().noneMatch(name::endsWith)) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(name));
                Files.copy(datei, zip);
                zip.closeEntry();
                anzahl++;
            }
            LOG.info("Datensicherung mit {} Dateien erstellt: {}", anzahl, zielDatei);
        } catch (IOException e) {
            throw new UncheckedIOException("Datensicherung fehlgeschlagen: " + zielDatei, e);
        }
    }
}
