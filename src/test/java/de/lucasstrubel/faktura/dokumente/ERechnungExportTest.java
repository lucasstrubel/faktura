package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.firma.Firmenprofil;
import de.lucasstrubel.faktura.firma.FirmenprofilService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.parsers.DocumentBuilderFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E-Rechnung nach EN 16931 (ERE-01/ERE-02): Das erzeugte CII-XML ist
 * wohlgeformt, trägt die EN-16931-Guideline und enthält Belegnummer,
 * Parteien und Summen aus den Beleg-Snapshots.
 */
class ERechnungExportTest {

    @TempDir
    Path tempDir;

    private static final Firmenprofil FIRMA = new Firmenprofil(
            "Faktura Software", "Musterstraße 1", "68163", "Mannheim",
            "DE123456789", null, null, "DE02120300000000202051", "BYLADEM1001", "Testbank");

    private ERechnungExport export() {
        return new ERechnungExport(new FirmenprofilService(null) {
            @Override
            public Firmenprofil lade() {
                return FIRMA;
            }
        });
    }

    private static Rechnung rechnung() {
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer("R-2026-000124");
        rechnung.setDatum(LocalDate.of(2026, 6, 9));
        rechnung.setzeKunde("K-000017", "Muster GmbH", "Hauptstr. 1, 68163 Mannheim");
        rechnung.setLeistungsdatum(LocalDate.of(2026, 6, 8));
        rechnung.setZahlungsziel(LocalDate.of(2026, 6, 23));
        rechnung.setzePositionen(List.of(
                new Dokumentposition("P-000042", "Beratungsstunde", 2,
                        new BigDecimal("80.00"), new BigDecimal("0.19")),
                new Dokumentposition("P-000043", "Fahrtkosten", 1,
                        new BigDecimal("40.00"), new BigDecimal("0.19"))));
        return rechnung;
    }

    @Test
    @DisplayName("ERE-01: EN-16931-XML ist wohlgeformt und trägt die Guideline-Kennung")
    void xmlIstWohlgeformtMitGuideline() throws Exception {
        Path ziel = tempDir.resolve("rechnung.xml");
        export().exportiereXml(rechnung(), ziel);

        DocumentBuilderFactory fabrik = DocumentBuilderFactory.newInstance();
        fabrik.setNamespaceAware(true);
        var dokument = fabrik.newDocumentBuilder().parse(ziel.toFile());
        assertEquals("CrossIndustryInvoice", dokument.getDocumentElement().getLocalName());
        String xml = Files.readString(ziel, StandardCharsets.UTF_8);
        assertTrue(xml.contains("urn:cen.eu:en16931:2017"),
                "EN-16931-Guideline-Kennung muss enthalten sein");
    }

    @Test
    @DisplayName("ERE-02: Belegnummer, Parteien und Bruttosumme stammen aus den Snapshots")
    void enthaeltBelegdatenUndSummen() throws Exception {
        Path ziel = tempDir.resolve("rechnung.xml");
        export().exportiereXml(rechnung(), ziel);

        String xml = Files.readString(ziel, StandardCharsets.UTF_8);
        assertTrue(xml.contains("R-2026-000124"), "Belegnummer");
        assertTrue(xml.contains("Faktura Software"), "Verkäufer aus dem Firmenprofil");
        assertTrue(xml.contains("Muster GmbH"), "Käufer aus dem Beleg-Snapshot");
        assertTrue(xml.contains("68163"), "PLZ aus der Snapshot-Anschrift");
        assertTrue(xml.contains("238.00"), "Bruttosumme 238,00");
        assertTrue(xml.contains("38.00"), "Steuerbetrag 38,00");
        assertTrue(xml.contains("DE02120300000000202051"), "IBAN der Bankverbindung");
    }
}
