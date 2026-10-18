package de.lucasstrubel.faktura.gemeinsam;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Schneller Startpfad (Q-04): Steht ein Bestand bereits auf der
 * ausgelieferten Schemaversion, braucht der Start Flyway nicht. Dessen
 * Konfiguration, Klassenpfadsuche und Validierung kosten bei jedem Start
 * rund eine halbe Sekunde, obwohl nichts zu migrieren ist.
 *
 * <p>Jede Abweichung — neuer Bestand, ausstehende oder fehlgeschlagene
 * Migration, Bestand aus einer neueren Version — läuft wie bisher über
 * Flyway samt Upgradeprobe ({@link UpgradeSicherung}).
 */
public final class Schemastand {

    /**
     * Höchste ausgelieferte Migration ({@code V<n>__…}); mit jeder neuen
     * Migration erhöhen — {@code SchemastandTest} (SST-01) schlägt sonst fehl.
     */
    public static final int AUSGELIEFERTE_VERSION = 6;

    private Schemastand() {
    }

    /**
     * {@code true}, wenn genau die Migrationen 1 bis {@link #AUSGELIEFERTE_VERSION}
     * erfolgreich angewendet sind und keine weitere.
     */
    public static boolean istAktuell(DataSource quelle) {
        JdbcTemplate jdbc = new JdbcTemplate(quelle);
        Integer historie = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'flyway_schema_history'",
                Integer.class);
        if (historie == null || historie == 0) {
            return false;
        }
        // Über null Zeilen liefern MAX und SUM NULL, der Vergleich damit ebenfalls
        Integer aktuell = jdbc.queryForObject(
                "SELECT COUNT(*) = ? AND MAX(CAST(version AS INTEGER)) = ? AND SUM(success = 0) = 0"
                        + " FROM flyway_schema_history WHERE version IS NOT NULL",
                Integer.class, AUSGELIEFERTE_VERSION, AUSGELIEFERTE_VERSION);
        return aktuell != null && aktuell == 1;
    }
}
