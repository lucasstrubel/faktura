package de.lucasstrubel.faktura.gui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Anzeige- und Eingabeformate der Listen und Dialoge (D-F-06): Beträge
 * kaufmännisch mit deutschem Zahlenformat, Datumswerte als TT.MM.JJJJ.
 */
public final class TabellenFormat {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Ganze Zahl ohne Trennzeichen, z. B. {@code 1500}. */
    private static final Pattern GANZZAHL = Pattern.compile("\\d+");
    /** Deutsch mit Tausenderpunkten, optional Dezimalkomma: {@code 1.500} oder {@code 1.500,25}. */
    private static final Pattern MIT_TAUSENDERPUNKT = Pattern.compile("\\d{1,3}(\\.\\d{3})+(,\\d+)?");
    /** Deutsch mit Dezimalkomma: {@code 1500,25}. */
    private static final Pattern MIT_KOMMA = Pattern.compile("\\d+,\\d+");
    /** Dezimalpunkt mit höchstens zwei Stellen: {@code 12.5}, {@code 12.50}. */
    private static final Pattern MIT_DEZIMALPUNKT = Pattern.compile("\\d+\\.\\d{1,2}");

    private TabellenFormat() {
    }

    /**
     * Liest einen eingegebenen Geldbetrag (B-F-03). Ein Punkt gefolgt von
     * genau drei Ziffern ist im Deutschen ein Tausendertrenner: {@code 1.500}
     * sind 1500 Euro, nicht 1,50 Euro — früher wurde der Punkt als
     * Dezimaltrenner gelesen und der Preis still um den Faktor 1000 zu klein
     * gespeichert. Alles, was keinem der Formate eindeutig entspricht (etwa
     * {@code 1e3} oder {@code 1.2.3}), wird abgelehnt. Gerundet wird nicht:
     * Mehr als zwei Nachkommastellen lehnt die Fachlogik mit Hinweis ab.
     *
     * @throws NumberFormatException wenn der Text kein eindeutiger Betrag ist
     */
    public static BigDecimal parseBetrag(String text) {
        String wert = text == null ? "" : text.strip().replace("€", "").replace(" ", "");
        if (GANZZAHL.matcher(wert).matches()) {
            return new BigDecimal(wert);
        }
        if (MIT_TAUSENDERPUNKT.matcher(wert).matches() || MIT_KOMMA.matcher(wert).matches()) {
            return new BigDecimal(wert.replace(".", "").replace(',', '.'));
        }
        if (MIT_DEZIMALPUNKT.matcher(wert).matches()) {
            return new BigDecimal(wert);
        }
        throw new NumberFormatException("Kein eindeutiger Betrag: " + text);
    }

    /** Betrag mit deutschem Zahlenformat und Währung, z. B. {@code 1.234,56 €}. */
    public static String betrag(BigDecimal wert) {
        if (wert == null) {
            return "";
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        return format.format(wert);
    }

    /** Datum als {@code TT.MM.JJJJ}; leer für {@code null}. */
    public static String datum(LocalDate wert) {
        return wert == null ? "" : DATUM.format(wert);
    }
}
