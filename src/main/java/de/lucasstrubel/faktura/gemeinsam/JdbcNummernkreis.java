package de.lucasstrubel.faktura.gemeinsam;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.IntSupplier;

/**
 * Transaktionssichere Vergabe fortlaufender Nummern (GR-01): Der Zähler liegt
 * in der Tabelle {@code nummernkreis} und wird in derselben Transaktion erhöht,
 * in der der zugehörige Datensatz gespeichert wird. Scheitert das Speichern,
 * rollt die Nummernvergabe mit zurück — es entsteht keine Lücke.
 *
 * <p>Die Zeile eines Bereichs wird beim ersten Zugriff angelegt und dabei aus
 * dem vorhandenen Bestand abgeleitet ({@code startwert}); laufende
 * Installationen übernehmen so ohne Datenmigration. Danach ist die Tabelle
 * allein maßgeblich.
 *
 * <p>Die Methode läuft mit {@code Propagation.REQUIRED}: Aufrufe aus einer
 * transaktionalen Service-Methode treten deren Transaktion bei (der
 * eigentliche Zweck), Aufrufe ohne laufende Transaktion erhalten eine eigene.
 */
@Component
public class JdbcNummernkreis {

    /** Kennzeichnet jahresunabhängige Nummernkreise (Kunden, Produkte). */
    public static final int OHNE_JAHR = 0;

    private final JdbcTemplate jdbc;

    public JdbcNummernkreis(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Liefert die nächste laufende Nummer des Bereichs und schreibt sie fort.
     *
     * @param bereich   Nummernpräfix, z. B. {@code "R"} oder {@code "K"}
     * @param jahr      Jahr des Kreises oder {@link #OHNE_JAHR}
     * @param startwert erste zu vergebende Nummer, falls der Kreis noch nicht
     *                  existiert; wird nur dann ausgewertet
     */
    @Transactional
    public int naechsterWert(String bereich, int jahr, IntSupplier startwert) {
        int geaendert = jdbc.update("""
                UPDATE nummernkreis SET letzter_wert = letzter_wert + 1
                WHERE bereich = ? AND jahr = ?
                """, bereich, jahr);
        if (geaendert == 0) {
            jdbc.update("""
                    INSERT INTO nummernkreis (bereich, jahr, letzter_wert) VALUES (?, ?, ?)
                    """, bereich, jahr, startwert.getAsInt());
        }
        Integer wert = jdbc.queryForObject("""
                SELECT letzter_wert FROM nummernkreis WHERE bereich = ? AND jahr = ?
                """, Integer.class, bereich, jahr);
        if (wert == null) {
            throw new IllegalStateException(
                    "Der Nummernkreis " + bereich + "/" + jahr + " konnte nicht gelesen werden.");
        }
        return wert;
    }
}
