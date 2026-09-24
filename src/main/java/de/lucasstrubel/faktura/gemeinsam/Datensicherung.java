package de.lucasstrubel.faktura.gemeinsam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Datensicherung des lokalen Bestands (Q-06, IF-01): packt die
 * SQLite-Datenbank und die JSON-Sicherungsdateien des Datenverzeichnisses in
 * eine ZIP-Datei. Logdateien werden nicht gesichert.
 *
 * <p>Die Datenbank wird <em>nicht</em> als Datei kopiert: Bei laufender
 * Anwendung sind Verbindungen offen, im WAL-Modus stecken die jüngsten
 * Änderungen zudem in einer Seitendatei — eine Dateikopie wäre unvollständig
 * oder inkonsistent. Stattdessen erzeugt SQLite über {@code VACUUM INTO} einen
 * in sich geschlossenen Abzug, der garantiert einem gültigen Stand entspricht.
 */
@Component
public class Datensicherung {

    private static final Logger LOG = LoggerFactory.getLogger(Datensicherung.class);

    /** Zusätzlich gesicherte Dateien des Datenverzeichnisses (JSON-Bestände). */
    private static final String JSON_ENDUNG = ".json";

    /** Nach der Übernahme in die Datenbank umbenannte JSON-Altbestände (JsonDatenUebernahme). */
    private static final String JSON_UEBERNOMMEN_ENDUNG = ".json.uebernommen";

    /** Name der Datenbank innerhalb der Sicherung. */
    private static final String DATENBANK_EINTRAG = "faktura.db";

    private final JdbcTemplate jdbc;

    public Datensicherung(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Schreibt die Sicherung; vorhandene Zieldateien werden überschrieben. */
    public void erstelle(Path datenVerzeichnis, Path zielDatei) {
        Path abzug = null;
        try {
            abzug = erzeugeDatenbankAbzug();
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zielDatei))) {
                zip.putNextEntry(new ZipEntry(DATENBANK_EINTRAG));
                Files.copy(abzug, zip);
                zip.closeEntry();

                int anzahl = 1 + packeJsonBestaende(datenVerzeichnis, zip);
                LOG.info("Datensicherung mit {} Dateien erstellt: {}", anzahl, zielDatei);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Datensicherung fehlgeschlagen: " + zielDatei, e);
        } finally {
            loescheAbzug(abzug);
        }
    }

    /**
     * {@code VACUUM INTO} schreibt einen konsistenten, bereits verdichteten
     * Abzug der Datenbank. Das Ziel muss noch nicht existieren — SQLite legt
     * es an und bricht ab, falls es bereits vorhanden ist.
     */
    private Path erzeugeDatenbankAbzug() throws IOException {
        Path verzeichnis = Files.createTempDirectory("faktura-sicherung-");
        Path abzug = verzeichnis.resolve(DATENBANK_EINTRAG);
        jdbc.execute("VACUUM INTO '" + abzug.toString().replace("'", "''") + "'");
        return abzug;
    }

    private static int packeJsonBestaende(Path datenVerzeichnis, ZipOutputStream zip)
            throws IOException {
        if (!Files.isDirectory(datenVerzeichnis)) {
            return 0;
        }
        // Dateiname einmal auflösen: Path.getFileName() kann null liefern
        // (Wurzelverzeichnis), und ein zweiter Aufruf müsste erneut geprüft werden.
        Map<String, Path> bestaende = new TreeMap<>();
        try (var eintraege = Files.list(datenVerzeichnis)) {
            for (Path datei : eintraege.toList()) {
                Path dateiname = datei.getFileName();
                if (dateiname != null && (dateiname.toString().endsWith(JSON_ENDUNG)
                        || dateiname.toString().endsWith(JSON_UEBERNOMMEN_ENDUNG))) {
                    bestaende.put(dateiname.toString(), datei);
                }
            }
        }
        for (Map.Entry<String, Path> bestand : bestaende.entrySet()) {
            zip.putNextEntry(new ZipEntry(bestand.getKey()));
            Files.copy(bestand.getValue(), zip);
            zip.closeEntry();
        }
        return bestaende.size();
    }

    private static void loescheAbzug(Path abzug) {
        if (abzug == null) {
            return;
        }
        try {
            Files.deleteIfExists(abzug);
            Path verzeichnis = abzug.getParent();
            if (verzeichnis != null) {
                Files.deleteIfExists(verzeichnis);
            }
        } catch (IOException e) {
            LOG.warn("Temporärer Datenbankabzug {} konnte nicht gelöscht werden", abzug, e);
        }
    }
}
