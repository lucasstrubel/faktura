package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDF-Export (A-F-04): muss auch Altdaten ohne Produkt-Snapshot verkraften —
 * Positionen aus früheren Datenbeständen können {@code null} in
 * Produktreferenz, Einzelpreis und Positionssumme enthalten (IF-01). Dazu
 * die Pflichtangaben und Darstellungsregeln ab 3.0 (PDF-01 bis PDF-06).
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
        rechnung.setzeAussteller(TestBelege.FIRMA);
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

    @Test
    @DisplayName("PDF-01: Namen außerhalb von WinAnsi (Ł, ő, Ş), Emoji und Tabulatoren brechen den Export nicht ab")
    void sonderzeichenWerdenDargestellt() throws IOException {
        Rechnung rechnung = TestBelege.rechnung("R-2026-000002", DokumentStatus.OFFEN);
        rechnung.setzeKunde("K-000001", "Łukasz Wójcik\tKovács Ödön Şahin 😀", "Hauptstr. 1, 68163 Mannheim");

        String text = exportiereUndLies(rechnung);

        assertTrue(text.contains("Łukasz Wójcik"), text);
        assertTrue(text.contains("Kovács Ödön Şahin"), text);
    }

    @Test
    @DisplayName("PDF-02: Lange Bezeichnungen werden umbrochen statt abgeschnitten (§ 14 Abs. 4 Nr. 5 UStG)")
    void langeBezeichnungWirdUmbrochen() throws IOException {
        String bezeichnung = "WARTUNGSVERTRAG FÜR SERVERINFRASTRUKTUR INKLUSIVE ÜBERWACHUNG "
                + "RUFBEREITSCHAFT UND QUARTALSBERICHT";
        Rechnung rechnung = TestBelege.rechnung("R-2026-000003", DokumentStatus.OFFEN);
        rechnung.setzePositionen(List.of(new Dokumentposition("P-000001", bezeichnung, 1,
                new BigDecimal("100.00"), new BigDecimal("0.19"))));

        String text = exportiereUndLies(rechnung).replaceAll("\\s+", " ");

        // Die erste Zeile teilt sich die Tabellenzeile mit Menge und Beträgen,
        // der Rest folgt in eigenen Zeilen — kein Wort geht verloren
        assertTrue(text.contains("WARTUNGSVERTRAG FÜR"), text);
        assertTrue(text.contains(bezeichnung.substring("WARTUNGSVERTRAG FÜR ".length())),
                "vollständige Bezeichnung: " + text);
    }

    @Test
    @DisplayName("PDF-03: Umsatzsteuer wird je Steuersatz mit Bemessungsgrundlage ausgewiesen (A-F-25)")
    void steuerJeSatz() throws IOException {
        Rechnung rechnung = TestBelege.rechnung("R-2026-000004", DokumentStatus.OFFEN);
        rechnung.setzePositionen(List.of(
                new Dokumentposition("P-1", "Buch", 1, new BigDecimal("50.00"), new BigDecimal("0.07")),
                new Dokumentposition("P-2", "Beratung", 1, new BigDecimal("150.00"), new BigDecimal("0.19"))));

        String text = exportiereUndLies(rechnung);

        assertTrue(text.contains("Umsatzsteuer 7 % auf 50,00 EUR"), text);
        assertTrue(text.contains("3,50 EUR"), text);
        assertTrue(text.contains("Umsatzsteuer 19 % auf 150,00 EUR"), text);
        assertTrue(text.contains("28,50 EUR"), text);
        assertTrue(text.contains("Steuernummer 37/123/45678"), "Steuernummer im Briefkopf: " + text);
    }

    @Test
    @DisplayName("PDF-04: Die Stornorechnung trägt ihren Titel, den Verweis und keinen Zahlungshinweis (A-F-29)")
    void stornorechnung() throws IOException {
        Rechnung storno = TestBelege.rechnung("R-2026-000005", DokumentStatus.OFFEN);
        storno.setzeStatus(DokumentStatus.ENTWURF);
        storno.setzePositionen(storno.getPositionen().stream().map(Dokumentposition::negiert).toList());
        storno.setStornoZu("R-2026-000001");
        storno.setzeStatus(DokumentStatus.OFFEN);

        String text = exportiereUndLies(storno);

        assertTrue(text.contains("Stornorechnung R-2026-000005"), text);
        assertTrue(text.contains("Storniert Rechnung: R-2026-000001"), text);
        assertTrue(text.contains("-119,00 EUR"), text);
        assertTrue(!text.contains("Bitte überweisen"), text);
    }

    @Test
    @DisplayName("PDF-05: Ohne Aussteller-Snapshot und ohne Firmenprofil wird kein PDF erzeugt (A-F-26)")
    void ohneProfilKeinPdf() {
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer("R-2026-000006");
        rechnung.setDatum(LocalDate.of(2026, 6, 9));

        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> new PdfBoxPdfExporter().exportiere(rechnung, tempDir.resolve("x.pdf")));
        assertEquals("Firmenprofil", fehler.getFeldname());
    }

    @Test
    @DisplayName("PDF-06: Die Anschrift wird an der letzten Trennstelle in Straße und PLZ/Ort geteilt")
    void anschriftAnLetzterTrennstelle() {
        assertEquals(List.of("Hauptstr. 5, Hinterhaus", "68163 Mannheim"),
                PdfBoxPdfExporter.anschriftZeilen("Hauptstr. 5, Hinterhaus, 68163 Mannheim"));
        assertEquals(List.of("Postfach 12"), PdfBoxPdfExporter.anschriftZeilen("Postfach 12"));
        assertEquals(List.of(), PdfBoxPdfExporter.anschriftZeilen(null));
    }

    private String exportiereUndLies(Dokument dokument) throws IOException {
        Path ziel = tempDir.resolve(dokument.getBelegnummer() + ".pdf");
        new PdfBoxPdfExporter().exportiere(dokument, ziel);
        try (PDDocument pdf = Loader.loadPDF(ziel.toFile())) {
            return new PDFTextStripper().getText(pdf);
        }
    }
}
