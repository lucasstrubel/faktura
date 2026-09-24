package de.lucasstrubel.faktura.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Betragseingabe im deutschen Format (B-F-03): Der Punkt vor genau drei
 * Ziffern ist ein Tausendertrenner — "1.500" sind 1500 Euro, nicht 1,50.
 */
class TabellenFormatTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1500|1500",
            "1.500|1500",
            "1.500,25|1500.25",
            "1500,25|1500.25",
            "12.5|12.5",
            "12.50|12.50",
            "0,99|0.99",
            "1.234.567,89 €|1234567.89"})
    @DisplayName("TF-01: Übliche deutsche und englische Schreibweisen werden eindeutig gelesen")
    void gueltigeBetraege(String eingabe, String erwartet) {
        assertEquals(new BigDecimal(erwartet), TabellenFormat.parseBetrag(eingabe));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1e3", "1.2.3", "1,500.00", "-5", "abc", "", "12,", "1234.567"})
    @DisplayName("TF-02: Mehrdeutige oder ungültige Eingaben werden abgelehnt statt still falsch gelesen")
    void ungueltigeBetraege(String eingabe) {
        assertThrows(NumberFormatException.class, () -> TabellenFormat.parseBetrag(eingabe));
    }
}
