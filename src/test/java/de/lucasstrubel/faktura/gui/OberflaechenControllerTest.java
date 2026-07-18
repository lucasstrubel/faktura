package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.Angebot;
import de.lucasstrubel.faktura.dokumente.Auftragsbestaetigung;
import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.dokumente.DokumentStatus;
import de.lucasstrubel.faktura.dokumente.Lieferschein;
import de.lucasstrubel.faktura.dokumente.Positionsangabe;
import de.lucasstrubel.faktura.dokumente.Rechnung;
import de.lucasstrubel.faktura.dokumente.Summen;
import de.lucasstrubel.faktura.dokumente.TestBelege;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modultestplan Gruppe D (Pflichtenheft D, Kapitel 10): TC-01 bis TC-14.
 * Getestet wird die GUI-freie Controller- und Modell-Schicht; die
 * Service-Schnittstellen der Gruppen A-C werden durch Stubs ersetzt.
 */
class OberflaechenControllerTest {

    private DokumentServiceStub dokumentService;
    private KundenService kundenService;
    private ProduktService produktService;
    private RechnungsWizardController wizard;

    @BeforeEach
    void setUp() {
        dokumentService = new DokumentServiceStub();

        Kunde kunde = new Kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim");
        kunde.setKundennummer("K-000017");
        kundenService = new KundenService() {
            @Override
            public Kunde findeKunde(String kundennummer) {
                return "K-000017".equals(kundennummer) ? kunde : null;
            }

            @Override
            public List<Kunde> suche(String suchbegriff) {
                return kunde.getName().toLowerCase().contains(suchbegriff.toLowerCase())
                        ? List.of(kunde) : List.of();
            }
        };

        Produkt produkt = new Produkt("Beratungsstunde", new BigDecimal("80.00"), new BigDecimal("0.19"));
        produkt.setProduktnummer("P-000042");
        produktService = new ProduktService() {
            @Override
            public Produkt findeProdukt(String produktnummer) {
                return "P-000042".equals(produktnummer) ? produkt : null;
            }

            @Override
            public List<Produkt> suche(String suchbegriff) {
                return List.of(produkt);
            }
        };

        wizard = new RechnungsWizardController(dokumentService, kundenService, produktService);
    }

    private void fuelleGueltigesModell() {
        wizard.getModel().setKundenNr("K-000017");
        wizard.getModel().fuegePositionHinzu(new PositionsEingabe("P-000042", 2));
        wizard.getModel().setRechnungsdatum(LocalDate.of(2026, 6, 10));
    }

    @Test
    @DisplayName("TC-01: neuer Wizard startet mit Schritt KUNDE_WAEHLEN")
    void tc01ErsterSchritt() {
        assertEquals(WizardSchritt.KUNDE_WAEHLEN, wizard.getModel().getAktuellerSchritt());
    }

    @Test
    @DisplayName("TC-02: viermal weiter() mit gültigen Eingaben durchläuft alle Schritte")
    void tc02Schrittfolge() {
        fuelleGueltigesModell();

        assertTrue(wizard.weiter());
        assertEquals(WizardSchritt.POSITIONEN_ERFASSEN, wizard.getModel().getAktuellerSchritt());
        assertTrue(wizard.weiter());
        assertEquals(WizardSchritt.DATEN_BESTAETIGEN, wizard.getModel().getAktuellerSchritt());
        assertTrue(wizard.weiter());
        assertEquals(WizardSchritt.ZUSAMMENFASSUNG, wizard.getModel().getAktuellerSchritt());
        assertTrue(wizard.weiter());
        assertEquals(WizardSchritt.SPEICHERN, wizard.getModel().getAktuellerSchritt());
    }

    @Test
    @DisplayName("TC-03: ohne Kunden wird der Wechsel verhindert; Meldung benennt 'Kunde' (F-10)")
    void tc03KeinKunde() {
        assertFalse(wizard.weiter());
        assertEquals(WizardSchritt.KUNDE_WAEHLEN, wizard.getModel().getAktuellerSchritt());
        assertEquals(MeldungsTyp.FEHLER, wizard.getLetzteMeldung().typ());
        assertEquals("Kunde", wizard.getLetzteMeldung().feldname());
    }

    @Test
    @DisplayName("TC-04: leere Positionsliste verhindert den Wechsel; Meldung benennt 'Position'")
    void tc04KeinePosition() {
        wizard.getModel().setKundenNr("K-000017");
        assertTrue(wizard.weiter());

        assertFalse(wizard.weiter());
        assertEquals("Position", wizard.getLetzteMeldung().feldname());
    }

    @Test
    @DisplayName("TC-05: Position mit Menge 0 verhindert den Wechsel; Meldung benennt 'Menge'")
    void tc05MengeNull() {
        wizard.getModel().setKundenNr("K-000017");
        wizard.getModel().fuegePositionHinzu(new PositionsEingabe("P-000042", 0));
        assertTrue(wizard.weiter());

        assertFalse(wizard.weiter());
        assertEquals("Menge", wizard.getLetzteMeldung().feldname());
    }

    @Test
    @DisplayName("TC-06: zurueck() bis Schritt 1 erhält Kunde und Positionen (F-11)")
    void tc06ZurueckOhneDatenverlust() {
        fuelleGueltigesModell();
        wizard.weiter();
        wizard.weiter();
        assertEquals(WizardSchritt.DATEN_BESTAETIGEN, wizard.getModel().getAktuellerSchritt());

        wizard.zurueck();
        wizard.zurueck();

        assertEquals(WizardSchritt.KUNDE_WAEHLEN, wizard.getModel().getAktuellerSchritt());
        assertEquals("K-000017", wizard.getModel().getKundenNr());
        assertEquals(1, wizard.getModel().getPositionen().size());
        assertEquals(2, wizard.getModel().getPositionen().get(0).menge());
    }

    @Test
    @DisplayName("TC-07: Zusammenfassung enthält Kunde, Positionen, Mengen, Summen, Datum, Zahlungsziel (F-12)")
    void tc07Zusammenfassung() {
        fuelleGueltigesModell();
        dokumentService.summen = new Summen(
                new BigDecimal("200.00"), new BigDecimal("38.00"), new BigDecimal("238.00"));

        String text = wizard.erzeugeZusammenfassung();

        assertTrue(text.contains("Muster GmbH"));
        assertTrue(text.contains("2 x Beratungsstunde"));
        assertTrue(text.contains("200.00"));
        assertTrue(text.contains("38.00"));
        assertTrue(text.contains("238.00"));
        assertTrue(text.contains("10.06.2026"));
        assertTrue(text.contains("Zahlungsziel"));
    }

    @Test
    @DisplayName("TC-08: speichern() löst genau einen Aufruf aus; Erfolgsmeldung nennt Rechnungsnummer (F-13)")
    void tc08GenauEinSpeicheraufruf() {
        fuelleGueltigesModell();

        Meldung meldung = wizard.speichern();

        assertEquals(1, dokumentService.erstelleRechnungAufrufe);
        assertEquals(MeldungsTyp.ERFOLG, meldung.typ());
        assertTrue(meldung.text().contains("R-2026-000124"));
    }

    @Test
    @DisplayName("TC-09: Validierungsfehler der Fachkomponente wird als Fehlermeldung dargestellt (F-05/F-16)")
    void tc09SpeichernFehlerfall() {
        fuelleGueltigesModell();
        dokumentService.erstelleRechnungFehler =
                new ValidierungsException("Rechnungsdatum", "Das Pflichtfeld 'Rechnungsdatum' fehlt.");

        Meldung meldung = wizard.speichern();

        assertEquals(MeldungsTyp.FEHLER, meldung.typ());
        assertEquals("Rechnungsdatum", meldung.feldname());
    }

    @Test
    @DisplayName("TC-10: Stornieren ist nur bei Rechnungen im Status OFFEN aktiviert (F-14)")
    void tc10StornierenNurOffen() {
        DokumentListenController controller = new DokumentListenController(dokumentService);
        Rechnung offen = TestBelege.rechnung("R-2026-000001", DokumentStatus.OFFEN);
        Rechnung versendet = TestBelege.rechnung("R-2026-000002", DokumentStatus.VERSENDET);
        Rechnung storniert = TestBelege.rechnung("R-2026-000003", DokumentStatus.STORNIERT);

        assertTrue(controller.aktionenFuer(offen).stornierbar());
        assertFalse(controller.aktionenFuer(versendet).stornierbar());
        assertFalse(controller.aktionenFuer(storniert).stornierbar());
    }

    @Test
    @DisplayName("TC-11: ohne Bestätigung kein Service-Aufruf; mit Bestätigung genau einer (F-15)")
    void tc11StornoNurNachBestaetigung() {
        DokumentListenController controller = new DokumentListenController(dokumentService);

        assertNull(controller.storniere("R-2026-000124", false));
        assertEquals(0, dokumentService.storniereAufrufe);

        controller.storniere("R-2026-000124", true);
        assertEquals(1, dokumentService.storniereAufrufe);
        assertEquals("R-2026-000124", dokumentService.letzteStornierteNummer);
    }

    @Test
    @DisplayName("TC-12: versendeter Beleg: Änderungsaktionen deaktiviert, PDF-Export aktiviert (F-08)")
    void tc12VersendeterBeleg() {
        DokumentListenController controller = new DokumentListenController(dokumentService);
        Rechnung versendet = TestBelege.rechnung("R-2026-000002", DokumentStatus.VERSENDET);

        BelegAktionen aktionen = controller.aktionenFuer(versendet);
        assertFalse(aktionen.aenderbar());
        assertTrue(aktionen.pdfExport());
    }

    @Test
    @DisplayName("TC-13: Statusfilter OFFEN liefert genau die offenen Belege (F-06)")
    void tc13Statusfilter() {
        dokumentService.dokumente.add(TestBelege.rechnung("R-2026-000001", DokumentStatus.OFFEN));
        dokumentService.dokumente.add(TestBelege.rechnung("R-2026-000002", DokumentStatus.OFFEN));
        dokumentService.dokumente.add(TestBelege.rechnung("R-2026-000003", DokumentStatus.STORNIERT));

        DokumentListenController controller = new DokumentListenController(dokumentService);
        List<Dokument> offene = controller.gefiltert(DokumentStatus.OFFEN);

        assertEquals(2, offene.size());
        assertTrue(offene.stream().allMatch(d -> d.getStatus() == DokumentStatus.OFFEN));
    }

    @Test
    @DisplayName("TC-14: Stammdaten-Suche delegiert an KundenService und liefert den Treffer (F-03)")
    void tc14StammdatenSuche() {
        StammdatenController controller = new StammdatenController(kundenService, produktService);

        List<Kunde> treffer = controller.sucheKunden("Muster");

        assertEquals(1, treffer.size());
        assertEquals("K-000017", treffer.get(0).getKundennummer());
    }

    @Test
    @DisplayName("TC-15: kundenListe zeigt bei leerem Suchbegriff den Bestand und filtert sonst (D-F-03)")
    void tc15KundenListe() {
        StammdatenController controller = new StammdatenController(kundenService, produktService);

        // leerer oder fehlender Suchbegriff: gesamter Bestand
        assertEquals(1, controller.kundenListe("").size());
        assertEquals(1, controller.kundenListe("   ").size());
        assertEquals(1, controller.kundenListe(null).size());

        // Suchbegriff filtert (Teilstring über den Namen)
        assertEquals(1, controller.kundenListe("Muster").size());
        assertEquals(0, controller.kundenListe("unbekannt").size());
    }

    /** Zähl-Stub des DokumentService (Gruppe A) für die Controller-Tests. */
    private static final class DokumentServiceStub implements DokumentService {

        int erstelleRechnungAufrufe;
        int storniereAufrufe;
        String letzteStornierteNummer;
        ValidierungsException erstelleRechnungFehler;
        Summen summen = new Summen(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        final List<Dokument> dokumente = new ArrayList<>();

        @Override
        public Rechnung erstelleRechnung(String kundenNr, List<Positionsangabe> positionen,
                                         LocalDate rechnungsdatum, LocalDate zahlungsziel) {
            erstelleRechnungAufrufe++;
            if (erstelleRechnungFehler != null) {
                throw erstelleRechnungFehler;
            }
            Rechnung rechnung = new Rechnung();
            rechnung.setBelegnummer("R-2026-000124");
            return rechnung;
        }

        @Override
        public void storniere(String rechnungsnummer) {
            storniereAufrufe++;
            letzteStornierteNummer = rechnungsnummer;
        }

        @Override
        public Summen berechneSummen(List<Positionsangabe> positionen) {
            return summen;
        }

        @Override
        public List<Dokument> alleDokumente() {
            return dokumente;
        }

        @Override
        public Angebot erstelleAngebot(String kundenNr, List<Positionsangabe> positionen,
                                       LocalDate gueltigBis) {
            return new Angebot();
        }

        @Override
        public Auftragsbestaetigung erstelleAuftragsbestaetigung(String kundenNr,
                                                                 List<Positionsangabe> positionen) {
            return new Auftragsbestaetigung();
        }

        @Override
        public Lieferschein erstelleLieferschein(String kundenNr, List<Positionsangabe> positionen,
                                                 LocalDate lieferdatum) {
            return new Lieferschein();
        }

        @Override
        public Dokument erzeugeFolgebeleg(String belegnummer) {
            return null;
        }

        @Override
        public void versende(String belegnummer) {
        }

        @Override
        public List<Rechnung> offeneRechnungen() {
            return List.of();
        }

        @Override
        public void exportierePdf(String belegnummer, Path zielDatei) {
        }
    }
}
