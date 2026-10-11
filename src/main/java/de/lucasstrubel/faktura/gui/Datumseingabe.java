package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.List;

/** Kalenderstrenge, oberflächenunabhängige Datumseingabe (D-F-25, A-F-32). */
public final class Datumseingabe {
    private static final List<DateTimeFormatter> FORMATE = List.of(
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT),
            new DateTimeFormatterBuilder().appendPattern("d.M.")
                    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000).toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT));

    private Datumseingabe() { }

    public static LocalDate parse(String text, String feldname) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (DateTimeFormatter format : FORMATE) {
            try {
                return LocalDate.parse(text.strip(), format);
            } catch (DateTimeParseException ignored) {
                // Nächstes ausdrücklich unterstütztes Format versuchen.
            }
        }
        throw new ValidierungsException(feldname, "Das Feld '" + feldname
                + "' enthält kein gültiges Datum (Format TT.MM.JJJJ): " + text);
    }
}
