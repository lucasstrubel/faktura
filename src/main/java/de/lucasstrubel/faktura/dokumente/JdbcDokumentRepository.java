package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.firma.Firmenprofil;

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
 * Deren Summen werden aus der Datenbank übernommen, nicht neu berechnet —
 * ein ausgestellter Betrag bleibt, wie er ausgestellt wurde (GR-02).
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
        Rechnung rechnung = dokument instanceof Rechnung r ? r : null;
        Firmenprofil aussteller = dokument.getAussteller();
        jdbc.update("""
                INSERT INTO dokument (belegnummer, typ, datum, kunden_referenz, kunde_name,
                                      kunde_anschrift, status, vorgaenger_nr, summe_netto,
                                      summe_steuer, summe_brutto, gueltig_bis, lieferdatum,
                                      leistungsdatum, zahlungsziel, storniert_am, storniert_von,
                                      bezahlt_am, storno_zu,
                                      aussteller_name, aussteller_strasse, aussteller_plz,
                                      aussteller_ort, aussteller_ust_id_nr, aussteller_steuernummer,
                                      aussteller_telefon, aussteller_e_mail, aussteller_iban,
                                      aussteller_bic, aussteller_bank)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (belegnummer) DO UPDATE SET
                    datum = excluded.datum, kunden_referenz = excluded.kunden_referenz,
                    kunde_name = excluded.kunde_name, kunde_anschrift = excluded.kunde_anschrift,
                    status = excluded.status, vorgaenger_nr = excluded.vorgaenger_nr,
                    summe_netto = excluded.summe_netto, summe_steuer = excluded.summe_steuer,
                    summe_brutto = excluded.summe_brutto, gueltig_bis = excluded.gueltig_bis,
                    lieferdatum = excluded.lieferdatum, leistungsdatum = excluded.leistungsdatum,
                    zahlungsziel = excluded.zahlungsziel, storniert_am = excluded.storniert_am,
                    storniert_von = excluded.storniert_von, bezahlt_am = excluded.bezahlt_am,
                    storno_zu = excluded.storno_zu,
                    aussteller_name = excluded.aussteller_name,
                    aussteller_strasse = excluded.aussteller_strasse,
                    aussteller_plz = excluded.aussteller_plz,
                    aussteller_ort = excluded.aussteller_ort,
                    aussteller_ust_id_nr = excluded.aussteller_ust_id_nr,
                    aussteller_steuernummer = excluded.aussteller_steuernummer,
                    aussteller_telefon = excluded.aussteller_telefon,
                    aussteller_e_mail = excluded.aussteller_e_mail,
                    aussteller_iban = excluded.aussteller_iban,
                    aussteller_bic = excluded.aussteller_bic,
                    aussteller_bank = excluded.aussteller_bank
                """,
                dokument.getBelegnummer(), dokument.belegtyp().name(), text(dokument.getDatum()),
                dokument.getKundenReferenz(), dokument.getKundeName(), dokument.getKundeAnschrift(),
                dokument.getStatus().name(), dokument.getVorgaengerNr(),
                text(dokument.getSummeNetto()), text(dokument.getSummeSteuer()),
                text(dokument.getSummeBrutto()),
                dokument instanceof Angebot angebot ? text(angebot.getGueltigBis()) : null,
                dokument instanceof Lieferschein lieferschein ? text(lieferschein.getLieferdatum()) : null,
                rechnung == null ? null : text(rechnung.getLeistungsdatum()),
                rechnung == null ? null : text(rechnung.getZahlungsziel()),
                rechnung == null ? null : text(rechnung.getStorniertAm()),
                rechnung == null ? null : rechnung.getStorniertVon(),
                rechnung == null ? null : text(rechnung.getBezahltAm()),
                rechnung == null ? null : rechnung.getStornoZu(),
                aussteller == null ? null : aussteller.name(),
                aussteller == null ? null : aussteller.strasse(),
                aussteller == null ? null : aussteller.plz(),
                aussteller == null ? null : aussteller.ort(),
                aussteller == null ? null : aussteller.ustIdNr(),
                aussteller == null ? null : aussteller.steuernummer(),
                aussteller == null ? null : aussteller.telefon(),
                aussteller == null ? null : aussteller.eMail(),
                aussteller == null ? null : aussteller.iban(),
                aussteller == null ? null : aussteller.bic(),
                aussteller == null ? null : aussteller.bank());

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
                rechnung.setStornoZu(rs.getString("storno_zu"));
                yield rechnung;
            }
        };
        dokument.setBelegnummer(belegnummer);
        dokument.setDatum(datum(rs.getString("datum")));
        dokument.setzeKunde(rs.getString("kunden_referenz"), rs.getString("kunde_name"),
                rs.getString("kunde_anschrift"));
        dokument.setzeAussteller(aussteller(rs));
        dokument.setVorgaengerNr(rs.getString("vorgaenger_nr"));
        dokument.setzePositionen(ladePositionen(belegnummer));

        DokumentStatus status = DokumentStatus.valueOf(rs.getString("status"));
        if (status == DokumentStatus.VERSENDET || status == DokumentStatus.STORNIERT) {
            // Ausgestellte Beträge bleiben, wie sie gespeichert wurden (GR-02)
            dokument.stelleSummenWiederHer(betragOderNull(rs.getString("summe_netto")),
                    betragOderNull(rs.getString("summe_steuer")),
                    betragOderNull(rs.getString("summe_brutto")));
        }
        LocalDate bezahltAm = datum(rs.getString("bezahlt_am"));
        if (status == DokumentStatus.STORNIERT && dokument instanceof Rechnung rechnung) {
            rechnung.setzeStatus(DokumentStatus.OFFEN);
            rechnung.storniere(datum(rs.getString("storniert_am")), rs.getString("storniert_von"));
        } else {
            dokument.setzeStatus(status);
            if (bezahltAm != null && dokument instanceof Rechnung rechnung) {
                rechnung.markiereBezahlt(bezahltAm);
            }
        }
        return dokument;
    }

    /** Aussteller-Snapshot; Belege vor v3.0 haben keinen (A-F-27). */
    private static Firmenprofil aussteller(ResultSet rs) throws SQLException {
        String name = rs.getString("aussteller_name");
        if (name == null) {
            return null;
        }
        return new Firmenprofil(name, rs.getString("aussteller_strasse"),
                rs.getString("aussteller_plz"), rs.getString("aussteller_ort"),
                rs.getString("aussteller_ust_id_nr"), rs.getString("aussteller_steuernummer"),
                rs.getString("aussteller_telefon"), rs.getString("aussteller_e_mail"),
                rs.getString("aussteller_iban"), rs.getString("aussteller_bic"),
                rs.getString("aussteller_bank"));
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

    private static BigDecimal betragOderNull(String wert) {
        return wert == null ? null : new BigDecimal(wert);
    }

    /** Altdaten ohne Preis-Snapshot zählen als 0,00 (wie {@code berechneSummen}). */
    private static BigDecimal betrag(String wert) {
        return wert == null ? BigDecimal.ZERO : new BigDecimal(wert);
    }
}
