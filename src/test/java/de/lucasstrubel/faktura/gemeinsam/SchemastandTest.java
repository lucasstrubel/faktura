package de.lucasstrubel.faktura.gemeinsam;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemastandTest {

    @TempDir
    Path tempDir;

    private SQLiteDataSource quelle;

    @BeforeEach
    void setUp() {
        quelle = new SQLiteDataSource();
        quelle.setUrl("jdbc:sqlite:" + tempDir.resolve("faktura.db"));
    }

    @Test
    @DisplayName("SST-01: Die ausgelieferte Version entspricht der neuesten Migration im Klassenpfad (Q-04)")
    void ausgelieferteVersionIstNeuesteMigration() {
        Flyway flyway = Flyway.configure().dataSource(quelle).load();
        flyway.migrate();

        assertEquals(String.valueOf(Schemastand.AUSGELIEFERTE_VERSION),
                flyway.info().current().getVersion().getVersion());
    }

    @Test
    @DisplayName("SST-02: Ein neuer Bestand ist nicht aktuell, nach der Migration schon (Q-04)")
    void neuerBestandErstNachMigrationAktuell() {
        assertFalse(Schemastand.istAktuell(quelle));

        Flyway.configure().dataSource(quelle).load().migrate();

        assertTrue(Schemastand.istAktuell(quelle));
    }

    @Test
    @DisplayName("SST-03: Ausstehende Migrationen laufen weiter über Flyway und die Upgradeprobe (Q-06)")
    void ausstehendeMigrationNichtAktuell() {
        Flyway.configure().dataSource(quelle).target("4").load().migrate();

        assertFalse(Schemastand.istAktuell(quelle));
    }

    @Test
    @DisplayName("SST-04: Eine fehlgeschlagene Migration in der Historie ist nicht aktuell")
    void fehlgeschlageneMigrationNichtAktuell() {
        Flyway.configure().dataSource(quelle).load().migrate();
        new JdbcTemplate(quelle).update("UPDATE flyway_schema_history SET success = 0 WHERE version = '6'");

        assertFalse(Schemastand.istAktuell(quelle));
    }

    @Test
    @DisplayName("SST-05: Ein Bestand aus einer neueren Version ist nicht aktuell; Flyway meldet ihn")
    void neuererBestandNichtAktuell() {
        Flyway.configure().dataSource(quelle).load().migrate();
        new JdbcTemplate(quelle).update("INSERT INTO flyway_schema_history"
                + " (installed_rank, version, description, type, script, checksum, installed_by,"
                + " installed_on, execution_time, success)"
                + " VALUES (99, '7', 'neuer', 'SQL', 'V7__neuer.sql', 0, 'test', CURRENT_TIMESTAMP, 0, 1)");

        assertFalse(Schemastand.istAktuell(quelle));
    }
}
