package de.lucasstrubel.faktura.firma;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SQLite-Persistenz des Firmenprofils (genau eine Zeile, id = 1).
 */
@Repository
public class JdbcFirmenprofilRepository {

    private final JdbcTemplate jdbc;

    public JdbcFirmenprofilRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Liefert das gespeicherte Profil oder {@code null}, wenn keines existiert. */
    public Firmenprofil lade() {
        List<Firmenprofil> treffer = jdbc.query("SELECT * FROM firmenprofil WHERE id = 1",
                (rs, zeilenNr) -> new Firmenprofil(
                        rs.getString("name"), rs.getString("strasse"), rs.getString("plz"),
                        rs.getString("ort"), rs.getString("ust_id_nr"), rs.getString("telefon"),
                        rs.getString("e_mail"), rs.getString("iban"), rs.getString("bic"),
                        rs.getString("bank")));
        return treffer.isEmpty() ? null : treffer.get(0);
    }

    public void speichere(Firmenprofil profil) {
        jdbc.update("""
                INSERT INTO firmenprofil (id, name, strasse, plz, ort, ust_id_nr,
                                          telefon, e_mail, iban, bic, bank)
                VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    name = excluded.name, strasse = excluded.strasse, plz = excluded.plz,
                    ort = excluded.ort, ust_id_nr = excluded.ust_id_nr,
                    telefon = excluded.telefon, e_mail = excluded.e_mail,
                    iban = excluded.iban, bic = excluded.bic, bank = excluded.bank
                """,
                profil.name(), profil.strasse(), profil.plz(), profil.ort(), profil.ustIdNr(),
                profil.telefon(), profil.eMail(), profil.iban(), profil.bic(), profil.bank());
    }
}
