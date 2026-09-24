package de.lucasstrubel.faktura;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.gui.StammdatenController;
import de.lucasstrubel.faktura.kunden.JsonKundenRepository;
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
 * Integrationstests der Spring-Verdrahtung: Kontextstart, konfigurierbares
 * Datenverzeichnis mit SQLite-Datenbank (IF-01) und einmalige Übernahme
 * eines vorhandenen JSON-Bestands inklusive korrekt fortgesetzter
 * Nummernvergabe (GR-01).
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
    @DisplayName("KTX-02: SQLite-Datenbank liegt im konfigurierten Datenverzeichnis (IF-01)")
    void persistenzNutztKonfiguriertesDatenverzeichnis() {
        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            KundenVerwaltungsService kundenService = kontext.getBean(KundenVerwaltungsService.class);
            Kunde gespeichert = kundenService.legeAn(
                    new Kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim"));

            assertEquals("K-000001", gespeichert.getKundennummer());
            assertTrue(Files.exists(tempDir.resolve("faktura.db")),
                    "faktura.db muss im konfigurierten Datenverzeichnis liegen");
        }
    }

    @Test
    @DisplayName("KTX-03: JSON-Bestand wird einmalig übernommen, Nummernvergabe läuft weiter")
    void jsonBestandWirdUebernommen() {
        Kunde bestand = new Kunde("Alt GmbH", "Altweg 2", "01067", "Dresden");
        bestand.setKundennummer("K-000009");
        new JsonKundenRepository(tempDir.resolve("kunden.json")).speichere(bestand);

        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            KundenVerwaltungsService kundenService = kontext.getBean(KundenVerwaltungsService.class);

            assertNotNull(kundenService.findeKunde("K-000009"),
                    "übernommener Kunde muss in der Datenbank auffindbar sein");
            Kunde neu = kundenService.legeAn(
                    new Kunde("Neu GmbH", "Neuweg 3", "68163", "Mannheim"));
            assertEquals("K-000010", neu.getKundennummer(),
                    "Zähler muss aus dem übernommenen Bestand abgeleitet werden (GR-01)");
        }

        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            assertNotNull(kontext.getBean(KundenVerwaltungsService.class).findeKunde("K-000010"),
                    "zweiter Start: Übernahme darf nicht erneut laufen, Bestand bleibt erhalten");
        }
    }

    private ConfigurableApplicationContext neuerKontext() {
        // Als Kommandozeilenargument übergeben: höchste Präzedenz, damit das
        // Testverzeichnis den Wert aus application.yml sicher überschreibt.
        return new SpringApplicationBuilder(FakturaApplication.class)
                .headless(true)
                .run("--faktura.daten-verzeichnis=" + tempDir);
    }

    @Test
    @DisplayName("KTX-04: Übernommene JSON-Dateien werden umbenannt und nie erneut eingelesen (GR-01)")
    void uebernommeneJsonDateienWerdenUmbenannt() throws Exception {
        Kunde bestand = new Kunde("Alt GmbH", "Altweg 2", "01067", "Dresden");
        bestand.setKundennummer("K-000009");
        new JsonKundenRepository(tempDir.resolve("kunden.json")).speichere(bestand);

        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            assertNotNull(kontext.getBean(KundenVerwaltungsService.class).findeKunde("K-000009"));
        }
        assertFalse(Files.exists(tempDir.resolve("kunden.json")));
        assertTrue(Files.exists(tempDir.resolve("kunden.json" + JsonDatenUebernahme.ENDUNG_UEBERNOMMEN)));
    }

    @Test
    @DisplayName("KTX-05: Eine fehlerhafte JSON-Datei bricht die Übernahme vollständig ab; der nächste Start holt sie nach")
    void fehlerhafteUebernahmeIstAllesOderNichts() throws Exception {
        Kunde bestand = new Kunde("Alt GmbH", "Altweg 2", "01067", "Dresden");
        bestand.setKundennummer("K-000009");
        new JsonKundenRepository(tempDir.resolve("kunden.json")).speichere(bestand);
        Files.writeString(tempDir.resolve("dokumente.json"), "{ kaputt");

        assertThrows(RuntimeException.class, this::neuerKontext);

        Files.writeString(tempDir.resolve("dokumente.json"), "[]");
        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            assertNotNull(kontext.getBean(KundenVerwaltungsService.class).findeKunde("K-000009"),
                    "nach dem Abbruch muss die Übernahme vollständig nachgeholt werden");
        }
    }

    @Test
    @DisplayName("KTX-06: Das Datenverzeichnis kommt aus dem Startargument, sonst aus dem Benutzerverzeichnis")
    void datenverzeichnisVorrang() {
        String vorher = System.getProperty(FakturaApplication.DATENVERZEICHNIS);
        try {
            System.clearProperty(FakturaApplication.DATENVERZEICHNIS);
            FakturaApplication.setzeDatenverzeichnis(new String[] {"--faktura.daten-verzeichnis=/tmp/x"});
            assertEquals("/tmp/x", System.getProperty(FakturaApplication.DATENVERZEICHNIS));

            System.clearProperty(FakturaApplication.DATENVERZEICHNIS);
            FakturaApplication.setzeDatenverzeichnis(new String[0]);
            assertEquals(Path.of(System.getProperty("user.home"), "Faktura", "daten").toString(),
                    System.getProperty(FakturaApplication.DATENVERZEICHNIS));
        } finally {
            if (vorher == null) {
                System.clearProperty(FakturaApplication.DATENVERZEICHNIS);
            } else {
                System.setProperty(FakturaApplication.DATENVERZEICHNIS, vorher);
            }
        }
    }
}
