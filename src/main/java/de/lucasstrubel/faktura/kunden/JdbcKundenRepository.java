package de.lucasstrubel.faktura.kunden;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * SQLite-Persistenz der Kunden über Spring JDBC (IF-01). Sortierung und
 * Suche erfolgen wie bei der bisherigen JSON-Ablage in Java, damit die
 * Semantik (case-insensitive, Teilstrings) unverändert bleibt; der Bestand
 * ist auf 5.000 Kunden ausgelegt (Q-01/Q-02).
 */
@Repository
public class JdbcKundenRepository implements KundenRepository {

    private static final RowMapper<Kunde> ZEILE = (rs, zeilenNr) -> {
        Kunde kunde = new Kunde(rs.getString("name"), rs.getString("strasse"),
                rs.getString("plz"), rs.getString("ort"));
        kunde.setKundennummer(rs.getString("kundennummer"));
        kunde.setEMail(rs.getString("e_mail"));
        kunde.setTelefon(rs.getString("telefon"));
        kunde.setUstIdNr(rs.getString("ust_id_nr"));
        return kunde;
    };

    private final JdbcTemplate jdbc;

    public JdbcKundenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Kunde speichere(Kunde kunde) {
        jdbc.update("""
                INSERT INTO kunde (kundennummer, name, strasse, plz, ort, e_mail, telefon, ust_id_nr)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (kundennummer) DO UPDATE SET
                    name = excluded.name, strasse = excluded.strasse, plz = excluded.plz,
                    ort = excluded.ort, e_mail = excluded.e_mail, telefon = excluded.telefon,
                    ust_id_nr = excluded.ust_id_nr
                """,
                kunde.getKundennummer(), kunde.getName(), kunde.getStrasse(), kunde.getPlz(),
                kunde.getOrt(), kunde.getEMail(), kunde.getTelefon(), kunde.getUstIdNr());
        return kunde;
    }

    @Override
    public void loesche(String kundennummer) {
        jdbc.update("DELETE FROM kunde WHERE kundennummer = ?", kundennummer);
    }

    @Override
    public Kunde findeNachNummer(String kundennummer) {
        List<Kunde> treffer = jdbc.query(
                "SELECT * FROM kunde WHERE kundennummer = ?", ZEILE, kundennummer);
        return treffer.isEmpty() ? null : treffer.get(0);
    }

    @Override
    public List<Kunde> alleSortiertNachName() {
        return jdbc.query("SELECT * FROM kunde", ZEILE).stream()
                .sorted(Comparator.comparing(Kunde::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<Kunde> suche(String suchbegriff) {
        String begriff = suchbegriff == null ? "" : suchbegriff.toLowerCase(Locale.ROOT);
        return jdbc.query("SELECT * FROM kunde", ZEILE).stream()
                .filter(k -> k.getName().toLowerCase(Locale.ROOT).contains(begriff)
                        || k.getKundennummer().toLowerCase(Locale.ROOT).contains(begriff))
                .sorted(Comparator.comparing(Kunde::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
