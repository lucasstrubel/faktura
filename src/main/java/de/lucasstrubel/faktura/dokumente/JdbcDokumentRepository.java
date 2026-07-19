package de.lucasstrubel.faktura.dokumente;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * SQLite-Persistenz der Belege über Spring JDBC (IF-01). Die
 * Jackson-Polymorphie der JSON-Ablage wird als Single-Table-Vererbung mit
 * Diskriminatorspalte {@code typ} abgebildet; Positionen liegen in einer
 * eigenen Tabelle und werden beim Speichern vollständig ersetzt
 * (Beleg und Positionen in einer Transaktion).
 *
 * <p>Beim Laden werden die Felder gesetzt, solange der Beleg im
 * Initialstatus {@code ENTWURF} ist; der persistierte Status wird zuletzt
 * gesetzt, damit die Unveränderlichkeitsprüfung (GR-02, F-24) das
 * Wiederherstellen versendeter oder stornierter Belege nicht blockiert.
 * Positionen ohne Preis-Snapshot (Altdaten, IF-01) werden mit 0,00
 * übernommen — dieselbe Semantik wie {@code Dokument.berechneSummen()}.
 */
@Repository
public class JdbcDokumentRepository implements DokumentRepository {

    private final JdbcTemplate jdbc;

    public JdbcDokumentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Dokument speichere(Dokument dokument) {
        LocalDate storniertAm = dokument instanceof Rechnung rechnung
                ? rechnung.getStorniertAm() : null;
        String storniertVon = dokument instanceof Rechnung rechnung
                ? rechnung.getStorniertVon() : null;
        jdbc.update("""
                INSERT INTO dokument (belegnummer, typ, datum, kunden_referenz, kunde_name,
                                      kunde_anschrift, status, vorgaenger_nr, summe_netto,
                                      summe_steuer, summe_brutto, gueltig_bis, lieferdatum,
                                      leistungsdatum, zahlungsziel, storniert_am, storniert_von)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (belegnummer) DO UPDATE SET
                    datum = excluded.datum, kunden_referenz = excluded.kunden_referenz,
                    kunde_name = excluded.kunde_name, kunde_anschrift = excluded.kunde_anschrift,
                    status = excluded.status, vorgaenger_nr = excluded.vorgaenger_nr,
                    summe_netto = excluded.summe_netto, summe_steuer = excluded.summe_steuer,
                    summe_brutto = excluded.summe_brutto, gueltig_bis = excluded.gueltig_bis,
                    lieferdatum = excluded.lieferdatum, leistungsdatum = excluded.leistungsdatum,
                    zahlungsziel = excluded.zahlungsziel, storniert_am = excluded.storniert_am,
                    storniert_von = excluded.storniert_von
                """,
                dokument.getBelegnummer(), dokument.belegtyp().name(), text(dokument.getDatum()),
                dokument.getKundenReferenz(), dokument.getKundeName(), dokument.getKundeAnschrift(),
                dokument.getStatus().name(), dokument.getVorgaengerNr(),
                text(dokument.getSummeNetto()), text(dokument.getSummeSteuer()),
                text(dokument.getSummeBrutto()),
                dokument instanceof Angebot angebot ? text(angebot.getGueltigBis()) : null,
                dokument instanceof Lieferschein lieferschein ? text(lieferschein.getLieferdatum()) : null,
                dokument instanceof Rechnung rechnung ? text(rechnung.getLeistungsdatum()) : null,
                dokument instanceof Rechnung rechnung ? text(rechnung.getZahlungsziel()) : null,
                text(storniertAm), storniertVon);

        jdbc.update("DELETE FROM dokumentposition WHERE belegnummer = ?", dokument.getBelegnummer());
        int position = 1;
        for (Dokumentposition p : dokument.getPositionen()) {
            jdbc.update("""
                    INSERT INTO dokumentposition (belegnummer, position, produkt_referenz,
                                                  bezeichnung, menge, einzelpreis_netto, steuersatz)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    dokument.getBelegnummer(), position++, p.getProduktReferenz(), p.getBezeichnung(),
                    p.getMenge(), text(p.getEinzelpreisNetto()), text(p.getSteuersatz()));
        }
        return dokument;
    }

    @Override
    public Dokument findeNachNummer(String belegnummer) {
        List<Dokument> treffer = jdbc.query(
                "SELECT * FROM dokument WHERE belegnummer = ?",
                (rs, zeilenNr) -> baueDokument(rs), belegnummer);
        return treffer.isEmpty() ? null : treffer.get(0);
    }

    @Override
    public List<Dokument> alle() {
        return jdbc.query("SELECT * FROM dokument ORDER BY belegnummer",
                (rs, zeilenNr) -> baueDokument(rs));
    }

    /** Stellt den Beleg samt Positionen wieder her; Status zuletzt (GR-02). */
    private Dokument baueDokument(ResultSet rs) throws SQLException {
        String belegnummer = rs.getString("belegnummer");
        Dokument dokument = switch (Belegtyp.valueOf(rs.getString("typ"))) {
            case ANGEBOT -> {
                Angebot angebot = new Angebot();
                angebot.setGueltigBis(datum(rs.getString("gueltig_bis")));
                yield angebot;
            }
            case AUFTRAGSBESTAETIGUNG -> new Auftragsbestaetigung();
            case LIEFERSCHEIN -> {
                Lieferschein lieferschein = new Lieferschein();
                lieferschein.setLieferdatum(datum(rs.getString("lieferdatum")));
                yield lieferschein;
            }
            case RECHNUNG -> {
                Rechnung rechnung = new Rechnung();
                rechnung.setLeistungsdatum(datum(rs.getString("leistungsdatum")));
                rechnung.setZahlungsziel(datum(rs.getString("zahlungsziel")));
                yield rechnung;
            }
        };
        dokument.setBelegnummer(belegnummer);
        dokument.setDatum(datum(rs.getString("datum")));
        dokument.setzeKunde(rs.getString("kunden_referenz"), rs.getString("kunde_name"),
                rs.getString("kunde_anschrift"));
        dokument.setVorgaengerNr(rs.getString("vorgaenger_nr"));
        dokument.setzePositionen(ladePositionen(belegnummer));

        DokumentStatus status = DokumentStatus.valueOf(rs.getString("status"));
        if (status == DokumentStatus.STORNIERT && dokument instanceof Rechnung rechnung) {
            rechnung.setzeStatus(DokumentStatus.OFFEN);
            rechnung.storniere(datum(rs.getString("storniert_am")), rs.getString("storniert_von"));
        } else {
            dokument.setzeStatus(status);
        }
        return dokument;
    }

    private List<Dokumentposition> ladePositionen(String belegnummer) {
        return jdbc.query("""
                SELECT * FROM dokumentposition WHERE belegnummer = ? ORDER BY position
                """,
                (rs, zeilenNr) -> new Dokumentposition(
                        rs.getString("produkt_referenz"), rs.getString("bezeichnung"),
                        rs.getInt("menge"), betrag(rs.getString("einzelpreis_netto")),
                        betrag(rs.getString("steuersatz"))),
                belegnummer);
    }

    private static String text(LocalDate datum) {
        return datum == null ? null : datum.toString();
    }

    private static String text(BigDecimal betrag) {
        return betrag == null ? null : betrag.toPlainString();
    }

    private static LocalDate datum(String wert) {
        return wert == null ? null : LocalDate.parse(wert);
    }

    /** Altdaten ohne Preis-Snapshot zählen als 0,00 (wie {@code berechneSummen}). */
    private static BigDecimal betrag(String wert) {
        return wert == null ? BigDecimal.ZERO : new BigDecimal(wert);
    }
}
