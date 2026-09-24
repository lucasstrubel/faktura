package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.firma.Firmenprofil;
import de.lucasstrubel.faktura.firma.FirmenprofilService;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.parsers.DocumentBuilderFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E-Rechnung nach EN 16931 (ERE-01 bis ERE-08): Das erzeugte CII-XML ist
 * wohlgeformt, trägt die EN-16931-Guideline und enthält Belegnummer,
 * Parteien und Summen aus den Beleg-Snapshots — mit denselben Beträgen wie
 * das PDF (A-F-25) und der Stornorechnung als korrigierter Rechnung (A-F-29).
 */
class ERechnungExportTest {

    @TempDir
    Path tempDir;

    static final Firmenprofil FIRMA = new Firmenprofil(
            "Faktura Software", "Musterstraße 1", "68163", "Mannheim",
            "DE123456789", "37/123/45678", null, null,
            "DE02 1203 0000 0000 2020 51", "BYLADEM1001", "Testbank");

    /** Export mit dem Profil als "aktuellem" Firmenprofil (für Belege ohne Snapshot). */
    private ERechnungExport export(Firmenprofil aktuell) {
        return new ERechnungExport(new FirmenprofilService(null) {
            @Override
            public Optional<Firmenprofil> lade() {
                return Optional.ofNullable(aktuell);
            }
        });
    }

    private static Rechnung rechnung() {
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer("R-2026-000124");
        rechnung.setDatum(LocalDate.of(2026, 6, 9));
        rechnung.setzeKunde("K-000017", "Muster GmbH", "Hauptstr. 1, 68163 Mannheim");
        rechnung.setzeAussteller(FIRMA);
        rechnung.setLeistungsdatum(LocalDate.of(2026, 6, 8));
        rechnung.setZahlungsziel(LocalDate.of(2026, 6, 23));
        rechnung.setzePositionen(List.of(
                new Dokumentposition("P-000042", "Beratungsstunde", 2,
                        new BigDecimal("80.00"), new BigDecimal("0.19")),
                new Dokumentposition("P-000043", "Fahrtkosten", 1,
                        new BigDecimal("40.00"), new BigDecimal("0.19"))));
        return rechnung;
    }

    private String exportiere(Rechnung rechnung) throws Exception {
        Path ziel = tempDir.resolve(rechnung.getBelegnummer() + ".xml");
        export(null).exportiereXml(rechnung, ziel);
        return Files.readString(ziel, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("ERE-01: EN-16931-XML ist wohlgeformt und trägt die Guideline-Kennung")
    void xmlIstWohlgeformtMitGuideline() throws Exception {
        Path ziel = tempDir.resolve("rechnung.xml");
        export(null).exportiereXml(rechnung(), ziel);

        DocumentBuilderFactory fabrik = DocumentBuilderFactory.newInstance();
        fabrik.setNamespaceAware(true);
        var dokument = fabrik.newDocumentBuilder().parse(ziel.toFile());
        assertEquals("CrossIndustryInvoice", dokument.getDocumentElement().getLocalName());
        String xml = Files.readString(ziel, StandardCharsets.UTF_8);
        assertTrue(xml.contains("urn:cen.eu:en16931:2017"),
                "EN-16931-Guideline-Kennung muss enthalten sein");
    }

    @Test
    @DisplayName("ERE-02: Belegnummer, Parteien, Steuerkennungen und Bruttosumme stammen aus den Snapshots")
    void enthaeltBelegdatenUndSummen() throws Exception {
        String xml = exportiere(rechnung());
        assertTrue(xml.contains("R-2026-000124"), "Belegnummer");
        assertTrue(xml.contains("Faktura Software"), "Verkäufer aus dem Aussteller-Snapshot");
        assertTrue(xml.contains("Muster GmbH"), "Käufer aus dem Beleg-Snapshot");
        assertTrue(xml.contains("68163"), "PLZ aus der Snapshot-Anschrift");
        assertTrue(xml.contains("238.00"), "Bruttosumme 238,00");
        assertTrue(xml.contains("38.00"), "Steuerbetrag 38,00");
        assertTrue(xml.contains("DE02120300000000202051"), "IBAN der Bankverbindung, ohne Leerzeichen");
        assertTrue(xml.contains("DE123456789"), "USt-IdNr. des Verkäufers");
        assertTrue(xml.contains("37/123/45678"), "Steuernummer des Verkäufers (A-F-26)");
    }

    @Test
    @DisplayName("ERE-03: Umsatzsteuer je Steuersatz gerundet — XML und Belegsumme stimmen überein (A-F-25)")
    void steuerJeSatzWieBeleg() throws Exception {
        Rechnung rechnung = rechnung();
        rechnung.setzePositionen(Collections.nCopies(10, new Dokumentposition(
                "P-000001", "Kleinteil", 1, new BigDecimal("1.10"), new BigDecimal("0.19"))));
        // Je Position gerundet wären es 10 × 0,21 = 2,10 EUR
        assertEquals(new BigDecimal("2.09"), rechnung.getSummeSteuer());
        assertEquals(new BigDecimal("13.09"), rechnung.getSummeBrutto());

        String xml = exportiere(rechnung);
        assertTrue(xml.contains(">2.09<"), "Steuerbetrag 2,09 im XML");
        assertTrue(xml.contains(">13.09<"), "Bruttosumme 13,09 im XML");
    }

    @Test
    @DisplayName("ERE-04: Die Stornorechnung wird als korrigierte Rechnung (384) mit Verweis ausgegeben (A-F-29)")
    void stornorechnungAlsKorrektur() throws Exception {
        Rechnung storno = rechnung();
        storno.setzePositionen(rechnung().getPositionen().stream()
                .map(Dokumentposition::negiert).toList());
        storno.setStornoZu("R-2026-000100");
        String xml = exportiere(storno);

        assertTrue(xml.contains(">384<"), "Dokumentart 384 (korrigierte Rechnung)");
        assertTrue(xml.contains("R-2026-000100"), "Verweis auf die stornierte Rechnung");
        assertTrue(xml.contains("-238.00"), "negative Bruttosumme");
    }

    @Test
    @DisplayName("ERE-05: Eine stornierte Rechnung wird nicht mehr als E-Rechnung ausgestellt")
    void stornierteRechnungWirdAbgelehnt() {
        Rechnung rechnung = rechnung();
        rechnung.setzeStatus(DokumentStatus.OFFEN);
        rechnung.storniere(LocalDate.of(2026, 6, 10), "Test");
        assertThrows(ValidierungsException.class,
                () -> export(null).exportiereXml(rechnung, tempDir.resolve("x.xml")));
    }

    @Test
    @DisplayName("ERE-06: Ein Komma in der Straßenangabe verschiebt die PLZ nicht")
    void kommaInDerStrasse() throws Exception {
        Rechnung rechnung = rechnung();
        rechnung.setzeKunde("K-000017", "Muster GmbH", "Hauptstr. 5, Hinterhaus, 68163 Mannheim");
        String xml = exportiere(rechnung);

        assertTrue(xml.contains("Hauptstr. 5, Hinterhaus"), "Straße vollständig");
        assertTrue(xml.contains(">68163<"), "PLZ als eigenes Element");
        assertTrue(xml.contains(">Mannheim<"), "Ort als eigenes Element");
    }

    @Test
    @DisplayName("ERE-07: Der Aussteller-Snapshot hat Vorrang vor dem aktuellen Firmenprofil (A-F-27)")
    void snapshotHatVorrang() throws Exception {
        Firmenprofil umgezogen = new Firmenprofil("Faktura Software", "Neuweg 9", "69115",
                "Heidelberg", "DE123456789", null, null, null, null, null, null);
        Path ziel = tempDir.resolve("snapshot.xml");
        export(umgezogen).exportiereXml(rechnung(), ziel);
        String xml = Files.readString(ziel, StandardCharsets.UTF_8);

        assertTrue(xml.contains("Musterstraße 1"), "Anschrift zum Erstellzeitpunkt");
        assertFalse(xml.contains("Neuweg 9"), "nicht die spätere Anschrift");
    }

    @Test
    @DisplayName("ERE-08: Ohne Steuernummer und USt-IdNr. wird keine E-Rechnung erzeugt (BR-CO-26)")
    void ohneSteuerkennungAbgelehnt() {
        Rechnung rechnung = rechnung();
        rechnung.setzeAussteller(new Firmenprofil("Faktura Software", "Musterstraße 1", "68163",
                "Mannheim", null, null, null, null, null, null, null));
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> export(null).exportiereXml(rechnung, tempDir.resolve("x.xml")));
        assertEquals("Steuernummer", fehler.getFeldname());
    }
}
