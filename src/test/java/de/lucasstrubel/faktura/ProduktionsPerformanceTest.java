package de.lucasstrubel.faktura;

import de.lucasstrubel.faktura.dokumente.*;
import de.lucasstrubel.faktura.gemeinsam.GesamtExport;
import de.lucasstrubel.faktura.kunden.*;
import de.lucasstrubel.faktura.produkte.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

/** Separater Messlauf auf produktivem Spring/SQLite-Pfad, ohne JaCoCo-Instrumentierung. */
@EnabledIfSystemProperty(named = "faktura.performance", matches = "true")
class ProduktionsPerformanceTest {
    @Test @DisplayName("PERF-01: SQLite 5000/5000/1000 – Suche, Dashboard, PDF und Gesamtexport")
    void produktionspfade() throws Exception {
        Path basis = Path.of("target/performance").toAbsolutePath(); Files.createDirectories(basis);
        Path daten = basis.resolve("daten-" + java.util.UUID.randomUUID());
        try (var k = new SpringApplicationBuilder(FakturaApplication.class).headless(true)
                .run("--faktura.daten-verzeichnis=" + daten, "--spring.main.banner-mode=off")) {
            var kunden = k.getBean(KundenRepository.class);
            var produkte = k.getBean(ProduktRepository.class);
            var dokumente = k.getBean(DokumentRepository.class);
            new TransactionTemplate(k.getBean(PlatformTransactionManager.class)).executeWithoutResult(status -> {
                for (int i = 1; i <= 5000; i++) {
                    Kunde kunde = new Kunde("Kunde " + i, "Weg 1", "68163", "Mannheim");
                    kunde.setKundennummer("K-" + String.format("%06d", i)); kunden.speichere(kunde);
                    Produkt p = new Produkt("Produkt " + i, new BigDecimal("49.99"), new BigDecimal("0.19"));
                    p.setProduktnummer("P-" + String.format("%06d", i)); p.setEinheit("Stück"); produkte.speichere(p);
                }
                for (int i = 1; i <= 1000; i++) { dokumente.speichere(rechnung(i, 5)); }
            });
            Files.writeString(basis.resolve("datenpfad.txt"), daten.toString());
            Files.writeString(basis.resolve("messungen.csv"), "operation;millisekunden;grenze\n");
            Files.writeString(basis.resolve("runner.txt"), System.getProperty("os.name") + " "
                    + System.getProperty("os.arch") + " Java " + System.getProperty("java.version")
                    + " CPUs=" + Runtime.getRuntime().availableProcessors() + " MaxHeap=" + Runtime.getRuntime().maxMemory());
            messe(basis, "kundensuche", 1000, () -> assertFalse(k.getBean(KundenVerwaltungsService.class).suche("4999").isEmpty()));
            messe(basis, "produktsuche", 1000, () -> assertFalse(k.getBean(ProduktVerwaltungsService.class).suche("4999").isEmpty()));
            messe(basis, "belegliste", 1000, () -> assertEquals(1000, k.getBean(DokumentService.class).alleDokumente().size()));
            messe(basis, "dashboard", 1000, () -> k.getBean(KennzahlenDienst.class).ermittle(LocalDate.now()));
            messe(basis, "pdf-50-positionen", 2000, () -> k.getBean(PdfExporter.class).exportiere(rechnung(9999, 50), basis.resolve("rechnung.pdf")));
            messe(basis, "gesamtexport", 30000, () -> k.getBean(GesamtExport.class).exportiere(basis.resolve("export.zip")));
        }
    }

    private static Rechnung rechnung(int nr, int positionen) {
        Rechnung r = new Rechnung(); r.setBelegnummer("R-2026-" + String.format("%06d", nr));
        r.setDatum(LocalDate.of(2026, 10, 7)); r.setLeistungsdatum(r.getDatum());
        r.setZahlungsziel(r.getDatum().plusDays(14)); r.setzeAussteller(TestBelege.FIRMA);
        r.setzeKunde("K-000001", "Kunde 1", "Weg 1, 68163 Mannheim");
        r.setzePositionen(Collections.nCopies(positionen, new Dokumentposition("P-000001", "Produkt 1", 1,
                new BigDecimal("49.99"), new BigDecimal("0.19"), "Stück", "C62")));
        return r;
    }

    private static void messe(Path basis, String name, long grenze, Runnable arbeit) throws Exception {
        long start = System.nanoTime(); arbeit.run(); long millis = (System.nanoTime() - start) / 1_000_000;
        Files.writeString(basis.resolve("messungen.csv"), name + ";" + millis + ";" + grenze + "\n", StandardOpenOption.APPEND);
        assertTrue(millis <= grenze, name + ": " + millis + " ms > " + grenze + " ms");
    }
}
