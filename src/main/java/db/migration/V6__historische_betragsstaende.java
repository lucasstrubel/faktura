package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Ergänzt Positionssummen und nachweisbare Steueraufschlüsselungen, ohne
 * bestehende Summen zu verändern (GR-02, A-F-25).
 *
 * <p>Die Rechenregeln sind hier bewusst eingefroren und importieren nichts aus
 * den Fachpaketen: Eine ausgelieferte Migration muss auf jedem später
 * aktualisierten Bestand dasselbe Ergebnis liefern, auch wenn sich
 * {@code Betragssnapshot} weiterentwickelt. Stand der Regeln: 3.1.0.
 *
 * <p>Je Beleg wird geprüft, ob die gespeicherten Summen exakt einer bekannten
 * Rundungsregel entsprechen — zuerst Steuer je Steuersatz gerundet
 * ({@code JE_STEUERSATZ}), dann Steuer je Position gerundet
 * ({@code ALTE_POSITIONSRUNDUNG}). Passt keine, bleibt der Beleg
 * {@code UNGEKLAERT} und erhält keine Aufschlüsselung.
 */
public class V6__historische_betragsstaende extends BaseJavaMigration {

    private record Position(BigDecimal preis, BigDecimal satz, BigDecimal netto) { }

    private record Steuerzeile(BigDecimal satz, BigDecimal netto, BigDecimal steuer) { }

    @Override
    public void migrate(Context context) throws Exception {
        Connection verbindung = context.getConnection();
        ergaenzePositionssummen(verbindung);
        ergaenzeAufschluesselungen(verbindung);
    }

    /** {@code einzelpreis_netto * menge}, Scale 2, HALF_UP — wie {@code Dokumentposition} (F-23). */
    private static void ergaenzePositionssummen(Connection verbindung) throws SQLException {
        try (Statement lesen = verbindung.createStatement();
             ResultSet werte = lesen.executeQuery("SELECT id, menge, einzelpreis_netto FROM dokumentposition");
             PreparedStatement schreiben = verbindung.prepareStatement(
                     "UPDATE dokumentposition SET positionssumme_netto = ? WHERE id = ?")) {
            while (werte.next()) {
                BigDecimal preis = betrag(werte.getString("einzelpreis_netto"));
                schreiben.setString(1, preis == null ? null : preis.multiply(BigDecimal.valueOf(werte.getInt("menge")))
                        .setScale(2, RoundingMode.HALF_UP).toPlainString());
                schreiben.setLong(2, werte.getLong("id"));
                schreiben.executeUpdate();
            }
        }
    }

    private static void ergaenzeAufschluesselungen(Connection verbindung) throws SQLException {
        try (Statement belege = verbindung.createStatement();
             ResultSet ergebnis = belege.executeQuery(
                     "SELECT belegnummer, summe_netto, summe_steuer, summe_brutto FROM dokument");
             PreparedStatement positionen = verbindung.prepareStatement(
                     "SELECT einzelpreis_netto, steuersatz, positionssumme_netto FROM dokumentposition"
                             + " WHERE belegnummer = ? ORDER BY position");
             PreparedStatement art = verbindung.prepareStatement(
                     "UPDATE dokument SET berechnungsart = ? WHERE belegnummer = ?");
             PreparedStatement steuer = verbindung.prepareStatement(
                     "INSERT INTO dokumentsteuer (belegnummer, steuersatz, netto, steuer) VALUES (?, ?, ?, ?)")) {
            while (ergebnis.next()) {
                String nummer = ergebnis.getString("belegnummer");
                positionen.setString(1, nummer);
                List<Position> liste = new ArrayList<>();
                try (ResultSet zeilen = positionen.executeQuery()) {
                    while (zeilen.next()) {
                        liste.add(new Position(betrag(zeilen.getString("einzelpreis_netto")),
                                betrag(zeilen.getString("steuersatz")),
                                betrag(zeilen.getString("positionssumme_netto"))));
                    }
                }
                BigDecimal netto = betrag(ergebnis.getString("summe_netto"));
                BigDecimal steuerSumme = betrag(ergebnis.getString("summe_steuer"));
                BigDecimal brutto = betrag(ergebnis.getString("summe_brutto"));

                String berechnungsart = "UNGEKLAERT";
                List<Steuerzeile> aufschluesselung = List.of();
                boolean vollstaendig = liste.stream()
                        .allMatch(p -> p.preis() != null && p.netto() != null && p.satz() != null);
                if (vollstaendig) {
                    for (boolean jePosition : new boolean[]{false, true}) {
                        List<Steuerzeile> probe = aufschluesselung(liste, jePosition);
                        BigDecimal probeNetto = summe(probe, Steuerzeile::netto);
                        BigDecimal probeSteuer = summe(probe, Steuerzeile::steuer);
                        if (gleich(probeNetto, netto) && gleich(probeSteuer, steuerSumme)
                                && gleich(probeNetto.add(probeSteuer), brutto)) {
                            berechnungsart = jePosition ? "ALTE_POSITIONSRUNDUNG" : "JE_STEUERSATZ";
                            aufschluesselung = probe;
                            break;
                        }
                    }
                }

                art.setString(1, berechnungsart);
                art.setString(2, nummer);
                art.executeUpdate();
                for (Steuerzeile zeile : aufschluesselung) {
                    steuer.setString(1, nummer);
                    steuer.setString(2, zeile.satz().toPlainString());
                    steuer.setString(3, zeile.netto().toPlainString());
                    steuer.setString(4, zeile.steuer().toPlainString());
                    steuer.executeUpdate();
                }
            }
        }
    }

    /** Netto je Steuersatz; Steuer je Satz gerundet oder (alte Regel) je Position gerundet und summiert. */
    private static List<Steuerzeile> aufschluesselung(List<Position> positionen, boolean jePosition) {
        Map<BigDecimal, BigDecimal> nettoJeSatz = new TreeMap<>();
        Map<BigDecimal, BigDecimal> steuerJeSatz = new TreeMap<>();
        for (Position position : positionen) {
            BigDecimal satz = position.satz().stripTrailingZeros();
            nettoJeSatz.merge(satz, position.netto(), BigDecimal::add);
            steuerJeSatz.merge(satz, position.netto().multiply(satz).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal::add);
        }
        List<Steuerzeile> zeilen = new ArrayList<>();
        nettoJeSatz.forEach((satz, netto) -> zeilen.add(new Steuerzeile(satz,
                netto.setScale(2, RoundingMode.HALF_UP),
                jePosition ? steuerJeSatz.get(satz) : netto.multiply(satz).setScale(2, RoundingMode.HALF_UP))));
        return zeilen;
    }

    private static BigDecimal summe(List<Steuerzeile> zeilen, Function<Steuerzeile, BigDecimal> wert) {
        return zeilen.stream().map(wert).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.UNNECESSARY);
    }

    private static boolean gleich(BigDecimal links, BigDecimal rechts) {
        return links != null && rechts != null && links.compareTo(rechts) == 0;
    }

    /** Leerer Text bedeutet unbekannt; unlesbare Werte brechen die Migration ab (Upgradeprobe). */
    private static BigDecimal betrag(String text) {
        return text == null || text.isBlank() ? null : new BigDecimal(text);
    }
}
