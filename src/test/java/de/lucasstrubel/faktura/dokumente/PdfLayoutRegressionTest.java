package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.firma.Firmenprofil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class PdfLayoutRegressionTest {
    @Test @DisplayName("PDF-REM-01: Große Beträge, Stapellayout, Langtexte und Folgeseiten ohne Überlappung")
    void layoutUndPruefbilder() throws Exception {
        Path ordner = Path.of("target/pdf-pruefung"); Files.createDirectories(ordner);
        for (String variante : List.of("normal", "millionen", "gestapelt")) {
            Rechnung r = new Rechnung(); r.setBelegnummer("R-2026-000001"); r.setDatum(LocalDate.of(2026, 10, 7));
            r.setzeKunde("K-000001", "Muster GmbH", "Hauptstraße 5, Hinterhaus, 68163 Mannheim");
            r.setzeAussteller(variante.equals("normal") ? TestBelege.FIRMA : new Firmenprofil(
                    "Sehr lange Gesellschaft für Beratung und technische Dienstleistungen ".repeat(3),
                    "Lange Hauptstraße mit ausführlicher Gebäudebeschreibung 100", "68163", "Mannheim",
                    "DE123456789", null, null, null, null, null, null));
            String preis = switch (variante) { case "normal" -> "80.00"; case "millionen" -> "12500.00"; default -> "123456789012345678901234567890.00"; };
            r.setzePositionen(Collections.nCopies(variante.equals("normal") ? 3 : 25, new Dokumentposition("P-000001",
                    "Beratungsleistung mit vollständiger Beschreibung ".repeat(variante.equals("normal") ? 1 : 10),
                    100, new BigDecimal(preis), new BigDecimal("0.19"), "Pauschale nach individueller Vereinbarung", null)));
            if (variante.equals("millionen")) {
                r.stelleBetragssnapshotWiederHer(new Betragssnapshot(r.getSummeNetto(), r.getSummeSteuer(), r.getSummeBrutto(), List.of(), Berechnungsart.UNGEKLAERT));
            }
            Path datei = ordner.resolve(variante + ".pdf"); new PdfBoxPdfExporter().exportiere(r, datei);
            try (var pdf = Loader.loadPDF(datei.toFile())) {
                String text = new PDFTextStripper().getText(pdf);
                assertTrue(text.replaceAll("\\s+", " ").contains("Pauschale nach individueller Vereinbarung"));
                if (variante.equals("millionen")) {
                    assertTrue(text.contains("1.250.000,00")); assertTrue(text.contains("Aufschlüsselung nach Steuersätzen nicht verfügbar"));
                }
                if (!variante.equals("normal")) { assertTrue(pdf.getNumberOfPages() > 1); }
                PDFRenderer renderer = new PDFRenderer(pdf);
                for (int i = 0; i < pdf.getNumberOfPages(); i++) {
                    ImageIO.write(renderer.renderImageWithDPI(i, 75), "png", ordner.resolve(variante + "-" + (i + 1) + ".png").toFile());
                }
                new PDFTextStripper() {
                    private final Map<String, List<TextPosition>> zeilen = new HashMap<>();
                    @Override protected void processTextPosition(TextPosition position) {
                        assertTrue(position.getXDirAdj() >= 49 && position.getXDirAdj() + position.getWidthDirAdj() <= 547,
                                "Text außerhalb des Satzspiegels: " + position.getUnicode());
                        zeilen.computeIfAbsent(getCurrentPageNo() + ":" + Math.round(position.getYDirAdj() * 10), key -> new ArrayList<>()).add(position);
                        super.processTextPosition(position);
                    }
                    @Override protected void endDocument(org.apache.pdfbox.pdmodel.PDDocument dokument) {
                        for (var zeile : zeilen.values()) {
                            zeile.sort(Comparator.comparing(TextPosition::getXDirAdj));
                            for (int i = 1; i < zeile.size(); i++) {
                                var vorher = zeile.get(i - 1); var danach = zeile.get(i);
                                assertTrue(vorher.getXDirAdj() + vorher.getWidthDirAdj() <= danach.getXDirAdj() + 0.7,
                                        "Überlappung auf Seite " + getCurrentPageNo());
                            }
                        }
                    }
                }.getText(pdf);
            }
        }
    }

    @Test @DisplayName("PDF-REM-02: Altpositionen ohne Hinweistext; Positionen werden nicht über Seitenwechsel getrennt (GR-02, F-03)")
    void altbestandUndZusammenhalt() throws Exception {
        Path ordner = Path.of("target/pdf-pruefung"); Files.createDirectories(ordner);
        Rechnung alt = new Rechnung(); alt.setBelegnummer("R-2026-000002"); alt.setDatum(LocalDate.of(2026, 10, 7));
        alt.setzeKunde("K-000001", "Muster GmbH", "Hauptstraße 5, 68163 Mannheim"); alt.setzeAussteller(TestBelege.FIRMA);
        alt.setzePositionen(List.of(
                Dokumentposition.wiederhergestellt("P-000001", "Altleistung", 1, new BigDecimal("10.00"),
                        new BigDecimal("0.19"), null, null, new BigDecimal("10.00")),
                Dokumentposition.wiederhergestellt(null, "Ohne Produktnummer", 1, new BigDecimal("5.00"),
                        new BigDecimal("0.19"), null, null, new BigDecimal("5.00"))));
        Path datei = ordner.resolve("altbestand.pdf"); new PdfBoxPdfExporter().exportiere(alt, datei);
        try (var pdf = Loader.loadPDF(datei.toFile())) {
            String text = new PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
            assertTrue(text.contains("P-000001 · Altleistung"), text);
            assertTrue(text.contains("Ohne Produktnummer"));
            assertFalse(text.contains("Altbestand"));
            assertFalse(text.contains("null"));
        }

        for (String preis : List.of("80.00", "123456789012345678901234567890.00")) {
            List<Dokumentposition> positionen = new ArrayList<>();
            for (int i = 1; i <= 40; i++) {
                String nr = String.format("%02d", i);
                positionen.add(new Dokumentposition("P-0000" + nr, "ANFANGx" + nr + "x "
                        + "Beratungsleistung mit vollständiger Beschreibung ".repeat(3) + "ENDEx" + nr + "x",
                        1, new BigDecimal(preis), new BigDecimal("0.19"), "Stück", "C62"));
            }
            Rechnung r = new Rechnung(); r.setBelegnummer("R-2026-000003"); r.setDatum(LocalDate.of(2026, 10, 7));
            r.setzeKunde("K-000001", "Muster GmbH", "Hauptstraße 5, 68163 Mannheim"); r.setzeAussteller(TestBelege.FIRMA);
            r.setzePositionen(positionen);
            Path mehrseitig = ordner.resolve("zusammenhalt-" + preis.length() + ".pdf");
            new PdfBoxPdfExporter().exportiere(r, mehrseitig);
            try (var pdf = Loader.loadPDF(mehrseitig.toFile())) {
                assertTrue(pdf.getNumberOfPages() > 1);
                Map<String, Integer> anfang = new HashMap<>(), ende = new HashMap<>();
                for (int seite = 1; seite <= pdf.getNumberOfPages(); seite++) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setStartPage(seite); stripper.setEndPage(seite);
                    String text = stripper.getText(pdf);
                    for (int i = 1; i <= 40; i++) {
                        String nr = String.format("%02d", i);
                        if (text.contains("ANFANGx" + nr + "x")) { anfang.put(nr, seite); }
                        if (text.contains("ENDEx" + nr + "x")) { ende.put(nr, seite); }
                    }
                }
                assertEquals(40, anfang.size());
                assertEquals(anfang, ende, "Position über Seitenwechsel getrennt");
            }
        }
    }
}
