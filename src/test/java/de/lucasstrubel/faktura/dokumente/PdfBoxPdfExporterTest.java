package de.lucasstrubel.faktura.dokumente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDF-Export (A-F-04): muss auch Altdaten ohne Produkt-Snapshot verkraften —
 * Positionen aus früheren Datenbeständen können {@code null} in
 * Produktreferenz, Einzelpreis und Positionssumme enthalten (IF-01).
 */
class PdfBoxPdfExporterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("TC-15: Vollständiger Beleg wird als PDF-Datei exportiert")
    void exportiertVollstaendigenBeleg() {
        Path ziel = tempDir.resolve("rechnung.pdf");
        Rechnung rechnung = TestBelege.rechnung("R-2026-000001", DokumentStatus.OFFEN);

        new PdfBoxPdfExporter().exportiere(rechnung, ziel);

        assertTrue(Files.exists(ziel));
    }

    @Test
    @DisplayName("TC-16: Altdaten mit null-Positionsfeldern werfen beim Export keinen Fehler")
    void exportiertBelegMitNullPositionsfeldern() {
        Path ziel = tempDir.resolve("altdaten.pdf");
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer("R-2026-000099");
        // Default-Konstruktor wie beim Laden unvollständiger JSON-Altdaten:
        // produktReferenz, einzelpreisNetto und positionssummeNetto sind null
        rechnung.setzePositionen(List.of(new Dokumentposition()));

        assertDoesNotThrow(() -> new PdfBoxPdfExporter().exportiere(rechnung, ziel));
        assertTrue(Files.exists(ziel));
    }

    @Test
    @DisplayName("TC-17: Summenberechnung behandelt null-Positionssummen als 0")
    void summenberechnungToleriertNullPositionen() {
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer("R-2026-000098");

        rechnung.setzePositionen(List.of(new Dokumentposition()));

        assertEquals(new BigDecimal("0.00"), rechnung.getSummeNetto());
        assertEquals(new BigDecimal("0.00"), rechnung.getSummeSteuer());
        assertEquals(new BigDecimal("0.00"), rechnung.getSummeBrutto());
    }
}
