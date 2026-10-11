package de.lucasstrubel.faktura.gemeinsam;

import de.lucasstrubel.faktura.dokumente.DokumentCsvExport;
import de.lucasstrubel.faktura.kunden.KundenCsvExport;
import de.lucasstrubel.faktura.produkte.ProduktCsvExport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Portabler Gesamtbestand als ZIP mit CSV-Dateien und Formatbeschreibung
 * (Q-06, IF-01), vollständig innerhalb einer SQLite-Lesetransaktion. Ersetzt
 * keine wiederherstellbare Datensicherung ({@link Datensicherung}).
 */
@Component
public class GesamtExport {
    private final JdbcTemplate jdbc;
    private final KundenCsvExport kunden;
    private final ProduktCsvExport produkte;
    private final DokumentCsvExport dokumente;

    public GesamtExport(JdbcTemplate jdbc, KundenCsvExport kunden, ProduktCsvExport produkte,
                         DokumentCsvExport dokumente) {
        this.jdbc = jdbc;
        this.kunden = kunden;
        this.produkte = produkte;
        this.dokumente = dokumente;
    }

    @Transactional(readOnly = true)
    public void exportiere(Path ziel) {
        Path ordner = null;
        try {
            ordner = Files.createTempDirectory("faktura-export-");
            kunden.exportiereCsv(ordner.resolve("kunden.csv"));
            produkte.exportiereCsv(ordner.resolve("produkte.csv"));
            dokumente.exportiereCsv(ordner.resolve("dokumente.csv"));
            dokumente.exportiereSteuern(ordner.resolve("steuern.csv"));
            exportiereFirmenprofil(ordner.resolve("firmenprofil.csv"));
            tabelle(ordner.resolve("nummernkreise.csv"), "SELECT bereich, jahr, letzter_wert FROM nummernkreis ORDER BY bereich, jahr");
            // Feste Zeilenenden (LF) unabhängig von der Plattform; deshalb kein String.format mit %n
            Files.writeString(ordner.resolve("FORMAT.txt"), "Faktura Gesamt-Export, Formatversion 1, Anwendung "
                    + anwendungsversion() + "\n" + """
                    UTF-8 mit BOM, Semikolon, CRLF; Dezimalpunkt; ISO-8601-Daten.
                    CSV-Anführungszeichen werden verdoppelt. Formelverdächtige Texte erhalten ein Apostroph.
                    Leere Geldfelder bedeuten unbekannt, 0 bedeutet bekannte Null.
                    dokumente.csv: eine Zeile je Position; vorhandene 26 Spalten bleiben vorangestellt.
                    Ausstellerfelder sind historische Snapshots; firmenprofil.csv enthält das aktuelle Profil.
                    Einheiten ohne Code sind nicht XML-fähig; historische leere Einheiten bleiben unbekannt.
                    Neue Positionen aus Produkten ohne Einheit tragen Stück (C62).
                    berechnungsart: JE_STEUERSATZ, ALTE_POSITIONSRUNDUNG oder UNGEKLAERT.
                    steuern.csv enthält nur rekonstruierte oder gespeicherte Aufschlüsselungen.
                    nummernkreise.csv enthält sämtliche persistenten Zähler, einschließlich Jahreszuordnung.
                    Alle Dateien stammen aus derselben Datenbank-Lesetransaktion.
                    Dieser Export ersetzt keine wiederherstellbare Datenbanksicherung.
                    """);
            Path quelle = ordner;
            AtomareDatei.schreibe(ziel, temporaer -> {
                try (var zip = new ZipOutputStream(Files.newOutputStream(temporaer));
                     var dateien = Files.list(quelle)) {
                    for (Path datei : dateien.sorted().toList()) {
                        zip.putNextEntry(new ZipEntry(quelle.relativize(datei).toString()));
                        Files.copy(datei, zip);
                        zip.closeEntry();
                    }
                }
                Datensicherung.pruefeZip(temporaer);
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Gesamtexport fehlgeschlagen", e);
        } finally {
            if (ordner != null) {
                AtomareDatei.entferneTemporaerVerzeichnis(ordner);
            }
        }
    }

    /** Projektversion aus der von Maven gefilterten {@code faktura-version.properties}. */
    static String anwendungsversion() {
        try (InputStream strom = GesamtExport.class.getResourceAsStream("/faktura-version.properties")) {
            if (strom != null) {
                Properties werte = new Properties();
                werte.load(strom);
                String version = werte.getProperty("version");
                if (version != null && !version.isBlank() && !version.startsWith("${")) {
                    return version;
                }
            }
        } catch (IOException e) {
            // Fällt auf "unbekannt" zurück; die Versionsangabe ist rein informativ.
        }
        return "unbekannt";
    }

    @Transactional(readOnly = true)
    public void exportiereSteuern(Path ziel) {
        dokumente.exportiereSteuern(ziel);
    }

    @Transactional(readOnly = true)
    public void exportiereFirmenprofil(Path ziel) {
        tabelle(ziel, "SELECT name, strasse, plz, ort, ust_id_nr, steuernummer, telefon, e_mail, iban, bic, bank FROM firmenprofil ORDER BY id");
    }

    private void tabelle(Path ziel, String sql) {
        List<String> zeilen = jdbc.query(sql, rs -> {
            List<String> ergebnis = new ArrayList<>();
            var meta = rs.getMetaData();
            List<String> kopf = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) { kopf.add(meta.getColumnLabel(i)); }
            ergebnis.add(String.join(";", kopf));
            while (rs.next()) {
                List<String> werte = new ArrayList<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) { werte.add(Csv.feld(rs.getString(i))); }
                ergebnis.add(String.join(";", werte));
            }
            return ergebnis;
        });
        Csv.schreibe(ziel, zeilen);
    }
}
