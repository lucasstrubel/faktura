package de.lucasstrubel.faktura;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentCsvExport;
import de.lucasstrubel.faktura.dokumente.Dokumentposition;
import de.lucasstrubel.faktura.dokumente.JsonDokumentRepository;
import de.lucasstrubel.faktura.dokumente.PdfBoxPdfExporter;
import de.lucasstrubel.faktura.dokumente.Rechnung;
import de.lucasstrubel.faktura.gemeinsam.JsonPersistenz;
import de.lucasstrubel.faktura.kunden.JsonKundenRepository;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenCsvExport;
import de.lucasstrubel.faktura.produkte.JsonProduktRepository;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktCsvExport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Performance-Nachweise (Q-01 bis Q-04, Q-08) gemäß Lastenheft.
 *
 * <p>Referenzgröße Q-01: 5.000 Kunden und 5.000 Produkte. Die Grenzwerte sind
 * laufzeitabhängig; gemessen werden ausschließlich die Fachoperationen, das
 * Befüllen der Testdateien erfolgt vorab in {@link #seed(Path)} und fließt nicht
 * in die Messung ein.
 */
class PerformanceTest {

    private static final int ANZAHL_STAMMDATEN = 5_000;
    private static final int ANZAHL_DOKUMENTE = 1_000;

    @TempDir
    static Path datenVerzeichnis;

    private static Path kundenDatei;
    private static Path produkteDatei;
    private static Path dokumenteDatei;

    @BeforeAll
    static void seed() throws IOException {
        kundenDatei = datenVerzeichnis.resolve("kunden.json");
        produkteDatei = datenVerzeichnis.resolve("produkte.json");
        dokumenteDatei = datenVerzeichnis.resolve("dokumente.json");
        ObjectMapper mapper = JsonPersistenz.mapper();

        List<Kunde> kunden = new ArrayList<>(ANZAHL_STAMMDATEN);
        List<Produkt> produkte = new ArrayList<>(ANZAHL_STAMMDATEN);
        for (int i = 1; i <= ANZAHL_STAMMDATEN; i++) {
            Kunde kunde = new Kunde("Kunde " + i + " GmbH", "Hauptstr. " + i, "68163", "Mannheim");
            kunde.setKundennummer(String.format("K-%06d", i));
            kunden.add(kunde);

            Produkt produkt = new Produkt("Produkt " + i, new BigDecimal("49.99"), new BigDecimal("0.19"));
            produkt.setProduktnummer(String.format("P-%06d", i));
            produkte.add(produkt);
        }

        List<Dokument> dokumente = new ArrayList<>(ANZAHL_DOKUMENTE);
        for (int i = 1; i <= ANZAHL_DOKUMENTE; i++) {
            dokumente.add(rechnungMitPositionen(String.format("R-2026-%06d", i), 5));
        }

        mapper.writeValue(kundenDatei.toFile(), kunden);
        mapper.writeValue(produkteDatei.toFile(), produkte);
        mapper.writerFor(new TypeReference<List<Dokument>>() { }).writeValue(dokumenteDatei.toFile(), dokumente);
    }

    @Test
    @DisplayName("Q-04: Laden der drei Repositories (5.000/5.000/1.000) in ≤ 5 s")
    void q04Anwendungsstart() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            new JsonKundenRepository(kundenDatei);
            new JsonProduktRepository(produkteDatei);
            new JsonDokumentRepository(dokumenteDatei);
        });
    }

    @Test
    @DisplayName("Q-02: Suche/Auflistung in Kunden- und Produktverwaltung in ≤ 1 s")
    void q02Suche() {
        JsonKundenRepository kundenRepository = new JsonKundenRepository(kundenDatei);
        JsonProduktRepository produktRepository = new JsonProduktRepository(produkteDatei);

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            assertFalse(kundenRepository.suche("Kunde 4999").isEmpty());
            assertFalse(kundenRepository.alleSortiertNachName().isEmpty());
            assertFalse(produktRepository.suche("Produkt 4999").isEmpty());
            assertFalse(produktRepository.alleSortiertNachBezeichnung().isEmpty());
        });
    }

    @Test
    @DisplayName("Q-03: PDF-Export einer Rechnung mit 50 Positionen in ≤ 2 s")
    void q03PdfErstellung() {
        Rechnung rechnung = rechnungMitPositionen("R-2026-999999", 50);
        PdfBoxPdfExporter exporter = new PdfBoxPdfExporter();
        Path ziel = datenVerzeichnis.resolve("pdf/R-2026-999999.pdf");

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> exporter.exportiere(rechnung, ziel));
    }

    @Test
    @DisplayName("Q-08: Vollexport (Kunden, Produkte, Belege) in ≤ 30 s")
    void q08Datenexport() {
        JsonKundenRepository kundenRepository = new JsonKundenRepository(kundenDatei);
        JsonProduktRepository produktRepository = new JsonProduktRepository(produkteDatei);
        JsonDokumentRepository dokumentRepository = new JsonDokumentRepository(dokumenteDatei);

        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            new KundenCsvExport(kundenRepository).exportiereCsv(datenVerzeichnis.resolve("export/kunden.csv"));
            new ProduktCsvExport(produktRepository).exportiereCsv(datenVerzeichnis.resolve("export/produkte.csv"));
            new DokumentCsvExport(dokumentRepository).exportiereCsv(datenVerzeichnis.resolve("export/dokumente.csv"));
        });
    }

    private static Rechnung rechnungMitPositionen(String belegnummer, int anzahlPositionen) {
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer(belegnummer);
        rechnung.setDatum(LocalDate.of(2026, 6, 10));
        rechnung.setLeistungsdatum(LocalDate.of(2026, 6, 10));
        rechnung.setZahlungsziel(LocalDate.of(2026, 6, 24));
        rechnung.setzeKunde("K-000001", "Muster GmbH", "Hauptstr. 1, 68163 Mannheim");
        List<Dokumentposition> positionen = new ArrayList<>(anzahlPositionen);
        for (int i = 1; i <= anzahlPositionen; i++) {
            positionen.add(new Dokumentposition("P-" + String.format("%06d", i), "Produkt " + i,
                    i, new BigDecimal("49.99"), new BigDecimal("0.19")));
        }
        rechnung.setzePositionen(positionen);
        return rechnung;
    }
}
