package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.firma.Firmenprofil;

import java.math.BigDecimal;
import java.util.List;

/**
 * Testhelfer: erzeugt Belege in definierten Status für die Modultests
 * (auch der Komponente D), da der Statuswechsel im Produktivcode bewusst nur
 * über die Fachlogik möglich ist.
 */
public final class TestBelege {

    /** Vollständiges Firmenprofil (mit Steuerkennung) für Belege und Kontexttests. */
    public static final Firmenprofil FIRMA = new Firmenprofil(
            "Faktura Software", "Musterstraße 1", "68163", "Mannheim",
            "DE123456789", "37/123/45678", null, null,
            "DE02 1203 0000 0000 2020 51", "BYLADEM1001", "Testbank");

    private TestBelege() {
    }

    public static Rechnung rechnung(String belegnummer, DokumentStatus status) {
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer(belegnummer);
        rechnung.setzeAussteller(FIRMA);
        rechnung.setzePositionen(List.of(new Dokumentposition(
                "P-000001", "Testprodukt", 1, new BigDecimal("100.00"), new BigDecimal("0.19"))));
        rechnung.setzeStatus(status);
        return rechnung;
    }

    public static Angebot angebot(String belegnummer, DokumentStatus status) {
        Angebot angebot = new Angebot();
        angebot.setBelegnummer(belegnummer);
        angebot.setzeAussteller(FIRMA);
        angebot.setzeStatus(status);
        return angebot;
    }
}
