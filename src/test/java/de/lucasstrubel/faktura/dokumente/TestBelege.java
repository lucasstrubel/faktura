package de.lucasstrubel.faktura.dokumente;

import java.math.BigDecimal;
import java.util.List;

/**
 * Testhelfer: erzeugt Belege in definierten Status für die Modultests
 * (auch der Komponente D), da der Statuswechsel im Produktivcode bewusst nur
 * über die Fachlogik möglich ist.
 */
public final class TestBelege {

    private TestBelege() {
    }

    public static Rechnung rechnung(String belegnummer, DokumentStatus status) {
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer(belegnummer);
        rechnung.setzePositionen(List.of(new Dokumentposition(
                "P-000001", "Testprodukt", 1, new BigDecimal("100.00"), new BigDecimal("0.19"))));
        rechnung.setzeStatus(status);
        return rechnung;
    }

    public static Angebot angebot(String belegnummer, DokumentStatus status) {
        Angebot angebot = new Angebot();
        angebot.setBelegnummer(belegnummer);
        angebot.setzeStatus(status);
        return angebot;
    }
}
