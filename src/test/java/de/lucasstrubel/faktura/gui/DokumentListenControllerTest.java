package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.Angebot;
import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.dokumente.DokumentStatus;
import de.lucasstrubel.faktura.dokumente.Dokumentposition;
import de.lucasstrubel.faktura.dokumente.Positionsangabe;
import de.lucasstrubel.faktura.dokumente.Rechnung;
import de.lucasstrubel.faktura.dokumente.Summen;
import de.lucasstrubel.faktura.dokumente.Auftragsbestaetigung;
import de.lucasstrubel.faktura.dokumente.Lieferschein;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modultests der Filterung der Belegliste (D-F-06): Statusfilter und Suche
 * nach Belegnummer oder Kunde. GUI-frei und damit ohne laufende Oberfläche
 * prüfbar.
 */
class DokumentListenControllerTest {

    private final List<Dokument> bestand = new ArrayList<>();
    private DokumentListenController controller;

    @BeforeEach
    void setUp() {
        controller = new DokumentListenController(new DokumentServiceStub());
        // Status über die fachlichen Übergänge setzen, nicht über einen Setter:
        // setzeStatus ist bewusst nicht außerhalb der Komponente A sichtbar.
        bestand.add(rechnung("R-2026-000001", "Hoffmann Elektrotechnik GmbH", "K-000001", false));
        bestand.add(rechnung("R-2026-000002", "Atelier Sonnenschein", "K-000002", true));
        bestand.add(angebot("AN-2026-000001", "Hoffmann Elektrotechnik GmbH", "K-000001"));
    }

    @Test
    @DisplayName("DL-01: Ohne Filter erscheinen alle Belege")
    void ohneFilterAlleBelege() {
        assertEquals(3, controller.gefiltert(null, null).size());
        assertEquals(3, controller.gefiltert(null, "").size());
        assertEquals(3, controller.gefiltert(null, "   ").size());
    }

    @Test
    @DisplayName("DL-02: Der Statusfilter grenzt auf den gewählten Status ein")
    void statusfilterGrenztEin() {
        List<Dokument> versendet = controller.gefiltert(DokumentStatus.VERSENDET, null);

        assertEquals(1, versendet.size());
        assertEquals("R-2026-000002", versendet.get(0).getBelegnummer());
        assertEquals(2, controller.gefiltert(DokumentStatus.ENTWURF, null).size());
    }

    @Test
    @DisplayName("DL-03: Die Suche findet Belegnummer, Kundenname und Kundennummer")
    void sucheFindetNummerUndKunde() {
        assertEquals(1, controller.gefiltert(null, "R-2026-000002").size());
        assertEquals(2, controller.gefiltert(null, "Hoffmann").size());
        assertEquals(2, controller.gefiltert(null, "K-000001").size());
    }

    @Test
    @DisplayName("DL-04: Die Suche achtet nicht auf Groß- und Kleinschreibung")
    void sucheIgnoriertSchreibweise() {
        assertEquals(2, controller.gefiltert(null, "hoffmann").size());
        assertEquals(2, controller.gefiltert(null, "HOFFMANN").size());
        assertEquals(1, controller.gefiltert(null, "an-2026-000001").size());
    }

    @Test
    @DisplayName("DL-05: Status und Suche wirken zusammen")
    void statusUndSucheWirkenZusammen() {
        List<Dokument> treffer = controller.gefiltert(DokumentStatus.ENTWURF, "Hoffmann");

        assertEquals(2, treffer.size(), "Rechnung und Angebot desselben Kunden im Entwurf");
        assertTrue(controller.gefiltert(DokumentStatus.VERSENDET, "Hoffmann").isEmpty());
    }

    @Test
    @DisplayName("DL-06: Ein Suchbegriff ohne Treffer liefert eine leere Liste")
    void ohneTrefferLeereListe() {
        assertTrue(controller.gefiltert(null, "gibtesnicht").isEmpty());
    }

    private static Rechnung rechnung(String nummer, String kunde, String kundenNr,
                                     boolean versendet) {
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer(nummer);
        rechnung.setDatum(LocalDate.of(2026, 7, 1));
        rechnung.setzeKunde(kundenNr, kunde, "Musterweg 1, 68163 Mannheim");
        rechnung.setzePositionen(List.of(position()));
        if (versendet) {
            rechnung.versende();
        }
        return rechnung;
    }

    private static Angebot angebot(String nummer, String kunde, String kundenNr) {
        Angebot angebot = new Angebot();
        angebot.setBelegnummer(nummer);
        angebot.setDatum(LocalDate.of(2026, 7, 1));
        angebot.setzeKunde(kundenNr, kunde, "Musterweg 1, 68163 Mannheim");
        angebot.setzePositionen(List.of(position()));
        return angebot;
    }

    private static Dokumentposition position() {
        return new Dokumentposition("P-000001", "Leistung", 1,
                new BigDecimal("100.00"), new BigDecimal("0.19"));
    }

    /** Nur {@code alleDokumente()} wird ausgewertet. */
    private class DokumentServiceStub implements DokumentService {

        @Override
        public List<Dokument> alleDokumente() {
            return List.copyOf(bestand);
        }

        @Override
        public Angebot erstelleAngebot(String k, List<Positionsangabe> p, LocalDate g) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Auftragsbestaetigung erstelleAuftragsbestaetigung(String k, List<Positionsangabe> p) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Lieferschein erstelleLieferschein(String k, List<Positionsangabe> p, LocalDate l) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Rechnung erstelleRechnung(String k, List<Positionsangabe> p, LocalDate r, LocalDate z) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Dokument erzeugeFolgebeleg(String belegnummer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void versende(String belegnummer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void storniere(String rechnungsnummer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Rechnung> offeneRechnungen() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Summen berechneSummen(List<Positionsangabe> positionen) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void exportierePdf(String belegnummer, Path zielDatei) {
            throw new UnsupportedOperationException();
        }
    }
}
