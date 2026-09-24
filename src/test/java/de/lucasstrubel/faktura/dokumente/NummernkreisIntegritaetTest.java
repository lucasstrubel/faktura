package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.FakturaApplication;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenVerwaltungsService;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktVerwaltungsService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integritätstests des Nummernkreises (GR-01, F-12): Eine Belegnummer darf
 * nur dann verbraucht sein, wenn der Beleg auch tatsächlich gespeichert wurde.
 *
 * <p>Vor Einführung der Tabelle {@code nummernkreis} lief der Zähler im
 * Speicher und wurde vor dem Speichern erhöht — ein Fehlschlag hinterließ eine
 * Lücke in der Rechnungsnummernfolge. Diese Tests halten das behobene
 * Verhalten fest: Nummernvergabe und Speichern liegen in einer Transaktion.
 */
class NummernkreisIntegritaetTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        FehlerKonfiguration.speichernScheitert = false;
    }

    @Test
    @DisplayName("NK-01: Ein fehlgeschlagenes Speichern verbraucht keine Rechnungsnummer (GR-01)")
    void fehlgeschlagenesSpeichernVerbrauchtKeineNummer() {
        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            DokumentService dokumentService = kontext.getBean(DokumentService.class);
            String kundenNr = legeKundeAn(kontext);
            String produktNr = legeProduktAn(kontext);
            List<Positionsangabe> positionen = List.of(new Positionsangabe(produktNr, 1));
            int jahr = LocalDate.now().getYear();

            Rechnung erste = dokumentService.erstelleRechnung(
                    kundenNr, positionen, LocalDate.now(), null);
            assertEquals(String.format("R-%04d-000001", jahr), erste.getBelegnummer());

            // Speichern erzwungen scheitern lassen: die Transaktion rollt zurück
            FehlerKonfiguration.speichernScheitert = true;
            assertThrows(IllegalStateException.class, () -> dokumentService.erstelleRechnung(
                    kundenNr, positionen, LocalDate.now(), null));
            FehlerKonfiguration.speichernScheitert = false;

            Rechnung zweite = dokumentService.erstelleRechnung(
                    kundenNr, positionen, LocalDate.now(), null);
            assertEquals(String.format("R-%04d-000002", jahr), zweite.getBelegnummer(),
                    "die Nummer des gescheiterten Versuchs darf nicht verbraucht sein (GR-01)");
            assertNull(dokumentService.alleDokumente().stream()
                            .filter(d -> d.getBelegnummer().equals(String.format("R-%04d-000003", jahr)))
                            .findAny().orElse(null),
                    "der gescheiterte Beleg darf nicht persistiert worden sein");
        }
    }

    @Test
    @DisplayName("NK-02: Ein ungültiger Beleg berührt den Nummernkreis nicht (F-18)")
    void validierungsfehlerVerbrauchtKeineNummer() {
        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            DokumentService dokumentService = kontext.getBean(DokumentService.class);
            String kundenNr = legeKundeAn(kontext);
            String produktNr = legeProduktAn(kontext);
            int jahr = LocalDate.now().getYear();

            // Ohne Positionen: scheitert in der Prüfung, vor der Nummernvergabe
            assertThrows(RuntimeException.class, () -> dokumentService.erstelleRechnung(
                    kundenNr, List.of(), LocalDate.now(), null));

            Rechnung erste = dokumentService.erstelleRechnung(kundenNr,
                    List.of(new Positionsangabe(produktNr, 1)), LocalDate.now(), null);
            assertEquals(String.format("R-%04d-000001", jahr), erste.getBelegnummer(),
                    "die erste erfolgreiche Rechnung muss die laufende Nummer 1 tragen");
        }
    }

    @Test
    @DisplayName("NK-03: Der Zähler übersteht einen Neustart und zählt lückenlos weiter (GR-01)")
    void zaehlerUeberstehtNeustart() {
        int jahr = LocalDate.now().getYear();
        String kundenNr;
        String produktNr;

        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            kundenNr = legeKundeAn(kontext);
            produktNr = legeProduktAn(kontext);
            kontext.getBean(DokumentService.class).erstelleRechnung(kundenNr,
                    List.of(new Positionsangabe(produktNr, 1)), LocalDate.now(), null);
        }

        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            Rechnung zweite = kontext.getBean(DokumentService.class).erstelleRechnung(kundenNr,
                    List.of(new Positionsangabe(produktNr, 1)), LocalDate.now(), null);
            assertEquals(String.format("R-%04d-000002", jahr), zweite.getBelegnummer(),
                    "nach dem Neustart muss die Zählung ohne Lücke fortgesetzt werden");
        }
    }

    private String legeKundeAn(ConfigurableApplicationContext kontext) {
        return kontext.getBean(KundenVerwaltungsService.class)
                .legeAn(new Kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim"))
                .getKundennummer();
    }

    private String legeProduktAn(ConfigurableApplicationContext kontext) {
        return kontext.getBean(ProduktVerwaltungsService.class)
                .legeAn(new Produkt("Beratung", new BigDecimal("100.00"), new BigDecimal("0.19")))
                .getProduktnummer();
    }

    /** Kontext mit hinterlegtem Firmenprofil — ohne Profil entsteht kein Beleg (A-F-26). */
    private ConfigurableApplicationContext neuerKontext() {
        ConfigurableApplicationContext kontext = new SpringApplicationBuilder(
                FakturaApplication.class, FehlerKonfiguration.class)
                .headless(true)
                .run("--faktura.daten-verzeichnis=" + tempDir);
        kontext.getBean(de.lucasstrubel.faktura.firma.FirmenprofilService.class)
                .speichere(TestBelege.FIRMA);
        return kontext;
    }

    /**
     * Schiebt einen schaltbaren Fehler vor das echte Repository, um einen
     * Persistenzfehler mitten in der Belegerstellung nachzustellen.
     */
    @Configuration
    static class FehlerKonfiguration {

        static volatile boolean speichernScheitert;

        @Bean
        @Primary
        DokumentRepository fehlerRepository(JdbcDokumentRepository echtesRepository) {
            return new DokumentRepository() {

                @Override
                public Dokument speichere(Dokument dokument) {
                    if (speichernScheitert) {
                        throw new IllegalStateException(
                                "Speichern im Test erzwungen fehlgeschlagen");
                    }
                    return echtesRepository.speichere(dokument);
                }

                @Override
                public Dokument findeNachNummer(String belegnummer) {
                    return echtesRepository.findeNachNummer(belegnummer);
                }

                @Override
                public List<Dokument> alle() {
                    return echtesRepository.alle();
                }
            };
        }
    }
}
