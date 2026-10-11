package de.lucasstrubel.faktura.gemeinsam;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipFile;

/**
 * Schutz vor Schema-Upgrades (Q-06, IF-01): Stehen für einen vorhandenen
 * Bestand Migrationen aus, entsteht zuerst eine geprüfte Datensicherung unter
 * {@code sicherungen/}. Die Migration läuft anschließend probeweise auf einer
 * aus dieser Sicherung wiederhergestellten Kopie; erst wenn sie dort samt
 * Integritätsprüfung gelingt, migriert der Aufrufer den echten Bestand.
 */
public final class UpgradeSicherung {

    private static final Logger LOG = LoggerFactory.getLogger(UpgradeSicherung.class);

    private UpgradeSicherung() {
    }

    public static void pruefe(DataSource quelle, Path datenverzeichnis, Flyway flyway) {
        var stand = flyway.info();
        if (stand.applied().length == 0 || stand.pending().length == 0) {
            return;
        }
        Path sicherung = datenverzeichnis.resolve("sicherungen").resolve("vor-upgrade-" + UUID.randomUUID() + ".zip");
        new Datensicherung(new JdbcTemplate(quelle)).erstelle(datenverzeichnis, sicherung);
        Path probe = null;
        try {
            probe = Files.createTempDirectory("faktura-upgradeprobe-");
            Path datenbank = probe.resolve("faktura.db");
            try (ZipFile zip = new ZipFile(sicherung.toFile());
                 var strom = zip.getInputStream(zip.getEntry("faktura.db"))) {
                Files.copy(strom, datenbank);
            }
            SQLiteDataSource testquelle = new SQLiteDataSource();
            testquelle.setUrl("jdbc:sqlite:" + datenbank);
            JdbcTemplate jdbc = new JdbcTemplate(testquelle);
            if (!"ok".equals(jdbc.queryForObject("PRAGMA integrity_check", String.class))) {
                throw new IOException("Wiederhergestellte Sicherung besteht die Integritätsprüfung nicht.");
            }
            Flyway.configure().dataSource(testquelle).load().migrate();
            if (!"ok".equals(jdbc.queryForObject("PRAGMA integrity_check", String.class))) {
                throw new IOException("Die Upgradeprobe besteht die Integritätsprüfung nicht.");
            }
            LOG.info("Upgradeprobe bestanden; Sicherung vor dem Upgrade: {}", sicherung);
        } catch (IOException e) {
            throw new UncheckedIOException("Upgrade gestoppt; Sicherungsprüfung fehlgeschlagen", e);
        } finally {
            if (probe != null) {
                AtomareDatei.entferneTemporaerVerzeichnis(probe);
            }
        }
    }
}
