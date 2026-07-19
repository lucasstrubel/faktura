package de.lucasstrubel.faktura;

import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.gui.StammdatenController;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenVerwaltungsService;
import de.lucasstrubel.faktura.produkte.ProduktVerwaltungsService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integrationstest der Spring-Verdrahtung: Der Container muss alle
 * Fachkomponenten (A–C), den EreignisBus und den GUI-freien Controller
 * auflösen; die Persistenz schreibt in das konfigurierte Datenverzeichnis.
 */
class FakturaApplicationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("KTX-01: Spring-Kontext startet und verdrahtet alle Fachkomponenten")
    void kontextStartetUndVerdrahtetAlleKomponenten() {
        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            assertNotNull(kontext.getBean(DokumentService.class));
            assertNotNull(kontext.getBean(KundenVerwaltungsService.class));
            assertNotNull(kontext.getBean(ProduktVerwaltungsService.class));
            assertNotNull(kontext.getBean(StammdatenController.class));
            assertNotNull(kontext.getBean(EreignisBus.class));
        }
    }

    @Test
    @DisplayName("KTX-02: Persistenz schreibt in das konfigurierte Datenverzeichnis (IF-01)")
    void persistenzNutztKonfiguriertesDatenverzeichnis() {
        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            KundenVerwaltungsService kundenService = kontext.getBean(KundenVerwaltungsService.class);
            Kunde gespeichert = kundenService.legeAn(
                    new Kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim"));

            assertEquals("K-000001", gespeichert.getKundennummer());
            assertTrue(Files.exists(tempDir.resolve("kunden.json")),
                    "kunden.json muss im konfigurierten Datenverzeichnis liegen");
        }
    }

    private ConfigurableApplicationContext neuerKontext() {
        // Als Kommandozeilenargument übergeben: höchste Präzedenz, damit das
        // Testverzeichnis den Wert aus application.yml sicher überschreibt.
        return new SpringApplicationBuilder(FakturaApplication.class)
                .headless(true)
                .run("--faktura.daten-verzeichnis=" + tempDir);
    }
}
