package de.lucasstrubel.faktura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartphasenTest {

    @Test
    @DisplayName("SP-01: Startphasen erscheinen in Reihenfolge als Millisekunden seit Prozessstart (Q-04)")
    void phasenInReihenfolge() {
        Startphasen phasen = new Startphasen(10_000);
        phasen.markiere("main", 10_300);
        phasen.markiere("spring", 12_480);
        phasen.markiere("bereit", 14_120);

        assertEquals("FAKTURA_STARTPHASEN main=300 spring=2480 bereit=4120", phasen.protokollzeile());
    }

    @Test
    @DisplayName("SP-02: Der laufende Prozess misst ab seinem Start, nicht ab dem ersten Aufruf (Q-04)")
    void prozessMisstAbStart() {
        Startphasen phasen = Startphasen.PROZESS;
        phasen.markiere("test");

        String zeile = phasen.protokollzeile();
        assertTrue(zeile.startsWith(Startphasen.MARKER + " "), zeile);
        long millis = Long.parseLong(zeile.replaceAll(".*test=(\\d+).*", "$1"));
        assertTrue(millis >= 0, zeile);
    }
}
