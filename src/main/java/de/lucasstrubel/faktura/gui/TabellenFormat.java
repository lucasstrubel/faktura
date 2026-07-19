package de.lucasstrubel.faktura.gui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Anzeigeformate der Listen und Dialoge (D-F-06): Beträge kaufmännisch mit
 * deutschem Zahlenformat, Datumswerte als TT.MM.JJJJ.
 */
public final class TabellenFormat {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private TabellenFormat() {
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
