package de.lucasstrubel.faktura.gemeinsam;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidierungTest {

    @Test
    @DisplayName("VAL-01: fehlendes Pflichtfeld wird abgelehnt und benannt (Q-09)")
    void pflichtfeldFehltWirdBenannt() {
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> Validierung.pruefePflichtfeld("  ", "Ort"));
        assertEquals("Ort", fehler.getFeldname());
    }

    @ParameterizedTest
    @ValueSource(strings = {"68163", "01067", "99998"})
    @DisplayName("VAL-02: gültige fünfstellige PLZ wird akzeptiert (C-F-16)")
    void gueltigePlzWirdAkzeptiert(String plz) {
        assertDoesNotThrow(() -> Validierung.pruefePlz(plz));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "123456", "6816a", "68 163"})
    @DisplayName("VAL-03: ungültige PLZ wird abgelehnt und benannt (C-F-16)")
    void ungueltigePlzWirdAbgelehnt(String plz) {
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> Validierung.pruefePlz(plz));
        assertEquals("PLZ", fehler.getFeldname());
    }

    @ParameterizedTest
    @ValueSource(strings = {"max@beispiel.de", "info@sub.domain.example", "a.b+c@firma-x.de"})
    @DisplayName("VAL-04: gültige E-Mail-Adressen werden akzeptiert (C-F-04)")
    void gueltigeEMailWirdAkzeptiert(String eMail) {
        assertDoesNotThrow(() -> Validierung.pruefeEMail(eMail));
    }

    @ParameterizedTest
    @ValueSource(strings = {"max.mustermann", "max@beispiel", "@beispiel.de", "max@.de"})
    @DisplayName("VAL-05: ungültige E-Mail-Adressen werden abgelehnt (C-F-04)")
    void ungueltigeEMailWirdAbgelehnt(String eMail) {
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> Validierung.pruefeEMail(eMail));
        assertEquals("E-Mail", fehler.getFeldname());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DE123456789", "DE 123 456 789"})
    @DisplayName("VAL-06: gültige USt-IdNr. (auch mit Leerzeichen) wird akzeptiert (C-F-17)")
    void gueltigeUstIdNrWirdAkzeptiert(String ustIdNr) {
        assertDoesNotThrow(() -> Validierung.pruefeUstIdNr(ustIdNr));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "DE12345678", "DE1234567890", "AT123456789", "DEABCDEFGHI"})
    @DisplayName("VAL-07: ungültige USt-IdNr. wird abgelehnt und benannt (C-F-17)")
    void ungueltigeUstIdNrWirdAbgelehnt(String ustIdNr) {
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> Validierung.pruefeUstIdNr(ustIdNr));
        assertEquals("USt-IdNr.", fehler.getFeldname());
    }

    @ParameterizedTest
    @ValueSource(strings = {"+49 621 123456", "0621/123456", "(0621) 12 34 56"})
    @DisplayName("VAL-08: gültige Telefonnummern werden akzeptiert (C-F-18)")
    void gueltigeTelefonnummerWirdAkzeptiert(String telefon) {
        assertDoesNotThrow(() -> Validierung.pruefeTelefon(telefon));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "telefonnummer", "0621-abc-123"})
    @DisplayName("VAL-09: zu kurze oder unzulässige Telefonnummern werden abgelehnt (C-F-18)")
    void ungueltigeTelefonnummerWirdAbgelehnt(String telefon) {
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> Validierung.pruefeTelefon(telefon));
        assertEquals("Telefon", fehler.getFeldname());
    }

    @Test
    @DisplayName("VAL-10: leere optionale Felder (E-Mail, Telefon, USt-IdNr., PLZ-Format) werfen nicht")
    void leereOptionaleFelderSindGueltig() {
        assertDoesNotThrow(() -> {
            Validierung.pruefeEMail(null);
            Validierung.pruefeEMail("");
            Validierung.pruefeTelefon(null);
            Validierung.pruefeUstIdNr(null);
            Validierung.pruefePlz(null);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATU12345678", "FR12345678901", "NL123456789B01", "de 123 456 789", "IT12345678901"})
    @DisplayName("VAL-11: USt-IdNr. anderer EU-Staaten und Kleinschreibung werden akzeptiert (C-F-17)")
    void euUstIdNrWirdAkzeptiert(String ustIdNr) {
        assertDoesNotThrow(() -> Validierung.pruefeUstIdNr(ustIdNr));
    }

    @Test
    @DisplayName("VAL-12: IBAN mit falscher Prüfziffer wird abgelehnt, gültige auch klein und mit Leerzeichen akzeptiert (C-F-20)")
    void ibanPruefsumme() {
        assertDoesNotThrow(() -> Validierung.pruefeIban("de02 1203 0000 0000 2020 51"));
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> Validierung.pruefeIban("DE03120300000000202051"));
        assertEquals("IBAN", fehler.getFeldname());
    }

    @Test
    @DisplayName("VAL-13: BIC mit 8 oder 11 Zeichen wird akzeptiert, andere Längen abgelehnt (C-F-20)")
    void bicFormat() {
        assertDoesNotThrow(() -> Validierung.pruefeBic("BYLADEM1"));
        assertDoesNotThrow(() -> Validierung.pruefeBic("byladem1001"));
        assertEquals("BIC", assertThrows(ValidierungsException.class,
                () -> Validierung.pruefeBic("BYLADE")).getFeldname());
    }

    @Test
    @DisplayName("VAL-14: Steuernummer mit 10 bis 13 Ziffern und Schrägstrichen (C-F-20)")
    void steuernummerFormat() {
        assertDoesNotThrow(() -> Validierung.pruefeSteuernummer("37/123/45678"));
        assertDoesNotThrow(() -> Validierung.pruefeSteuernummer("2893081508152"));
        assertEquals("Steuernummer", assertThrows(ValidierungsException.class,
                () -> Validierung.pruefeSteuernummer("12/345")).getFeldname());
        assertEquals("Steuernummer", assertThrows(ValidierungsException.class,
                () -> Validierung.pruefeSteuernummer("37-123-45678x")).getFeldname());
    }

    @Test
    @DisplayName("VAL-15: Normalisierung — leere Werte werden null, USt-IdNr./IBAN/BIC einheitlich (C-F-19)")
    void normalisierung() {
        assertNull(Validierung.bereinige("   "));
        assertEquals("Mannheim", Validierung.bereinige("  Mannheim "));
        assertEquals("DE123456789", Validierung.normalisiereUstIdNr(" de 123 456 789 "));
        assertEquals("DE02 1203 0000 0000 2020 51", Validierung.normalisiereIban("de02120300000000202051"));
        assertEquals("BYLADEM1001", Validierung.normalisiereBic("byla dem1001"));
        assertNull(Validierung.normalisiereIban(""));
    }
}
