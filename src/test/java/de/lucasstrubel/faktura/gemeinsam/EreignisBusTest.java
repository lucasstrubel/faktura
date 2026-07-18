package de.lucasstrubel.faktura.gemeinsam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EreignisBusTest {

    @Test
    @DisplayName("INF-01: melde benachrichtigt alle Beobachter des Bereichs")
    void benachrichtigtAlleBeobachter() {
        EreignisBus bus = new EreignisBus();
        AtomicInteger ersterBeobachter = new AtomicInteger();
        AtomicInteger zweiterBeobachter = new AtomicInteger();
        bus.abonniere(DatenBereich.KUNDEN, ersterBeobachter::incrementAndGet);
        bus.abonniere(DatenBereich.KUNDEN, zweiterBeobachter::incrementAndGet);

        bus.melde(DatenBereich.KUNDEN);
        bus.melde(DatenBereich.KUNDEN);

        assertEquals(2, ersterBeobachter.get());
        assertEquals(2, zweiterBeobachter.get());
    }

    @Test
    @DisplayName("INF-02: melde benachrichtigt nur Beobachter des betroffenen Bereichs")
    void benachrichtigtNurBetroffenenBereich() {
        EreignisBus bus = new EreignisBus();
        AtomicInteger kundenBeobachter = new AtomicInteger();
        AtomicInteger dokumentBeobachter = new AtomicInteger();
        bus.abonniere(DatenBereich.KUNDEN, kundenBeobachter::incrementAndGet);
        bus.abonniere(DatenBereich.DOKUMENTE, dokumentBeobachter::incrementAndGet);

        bus.melde(DatenBereich.DOKUMENTE);

        assertEquals(0, kundenBeobachter.get());
        assertEquals(1, dokumentBeobachter.get());
    }

    @Test
    @DisplayName("INF-03: melde ohne Beobachter ist wirkungslos und wirft nicht")
    void meldenOhneBeobachterIstWirkungslos() {
        new EreignisBus().melde(DatenBereich.PRODUKTE);
    }
}
