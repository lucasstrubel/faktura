package de.lucasstrubel.faktura.produkte;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * SQLite-Persistenz der Produkte über Spring JDBC (IF-01). Beträge werden
 * als TEXT gespeichert und verlustfrei als {@link BigDecimal} gelesen;
 * Sortierung und Suche erfolgen wie bei der bisherigen JSON-Ablage in Java
 * (Q-01/Q-02).
 */
@Repository
public class JdbcProduktRepository implements ProduktRepository {

    private static final RowMapper<Produkt> ZEILE = (rs, zeilenNr) -> {
        Produkt produkt = new Produkt(rs.getString("bezeichnung"),
                new BigDecimal(rs.getString("einzelpreis_netto")),
                new BigDecimal(rs.getString("steuersatz")));
        produkt.setProduktnummer(rs.getString("produktnummer"));
        produkt.setBeschreibung(rs.getString("beschreibung"));
        produkt.setEinheit(rs.getString("einheit"));
        return produkt;
    };

    private final JdbcTemplate jdbc;

    public JdbcProduktRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Produkt speichere(Produkt produkt) {
        jdbc.update("""
                INSERT INTO produkt (produktnummer, bezeichnung, beschreibung,
                                     einzelpreis_netto, steuersatz, einheit)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (produktnummer) DO UPDATE SET
                    bezeichnung = excluded.bezeichnung, beschreibung = excluded.beschreibung,
                    einzelpreis_netto = excluded.einzelpreis_netto,
                    steuersatz = excluded.steuersatz, einheit = excluded.einheit
                """,
                produkt.getProduktnummer(), produkt.getBezeichnung(), produkt.getBeschreibung(),
                produkt.getEinzelpreisNetto().toPlainString(),
                produkt.getSteuersatz().toPlainString(), produkt.getEinheit());
        return produkt;
    }

    @Override
    public void loesche(String produktnummer) {
        jdbc.update("DELETE FROM produkt WHERE produktnummer = ?", produktnummer);
    }

    @Override
    public Produkt findeNachNummer(String produktnummer) {
        List<Produkt> treffer = jdbc.query(
                "SELECT * FROM produkt WHERE produktnummer = ?", ZEILE, produktnummer);
        return treffer.isEmpty() ? null : treffer.get(0);
    }

    @Override
    public List<Produkt> alleSortiertNachBezeichnung() {
        return jdbc.query("SELECT * FROM produkt", ZEILE).stream()
                .sorted(Comparator.comparing(Produkt::getBezeichnung, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<Produkt> suche(String suchbegriff) {
        String begriff = suchbegriff == null ? "" : suchbegriff.toLowerCase(Locale.ROOT);
        return jdbc.query("SELECT * FROM produkt", ZEILE).stream()
                .filter(p -> p.getBezeichnung().toLowerCase(Locale.ROOT).contains(begriff)
                        || p.getProduktnummer().toLowerCase(Locale.ROOT).contains(begriff))
                .sorted(Comparator.comparing(Produkt::getBezeichnung, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
