package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modultestplan Gruppe A (separates Dokument Modultestplan_GruppeA.md): TC-01 bis TC-13.
 * Die Schnittstellen der Gruppen B und C werden durch Stubs ersetzt,
 * der PDF-Export durch einen No-Op-Stub.
 */
class DokumentzyklusTest {

    private static final String KUNDE_NR = "K-000001";
    private static final String PRODUKT_NR = "P-000001";

    @TempDir
    Path tempDir;

    private JsonDokumentRepository repository;
    private EinfacherBelegnummernGenerator nummernGenerator;
    private Map<String, Produkt> produkte;
    private StandardDokumentService service;

    @BeforeEach
    void setUp() {
        repository = new JsonDokumentRepository(tempDir.resolve("dokumente.json"));
        nummernGenerator = new EinfacherBelegnummernGenerator();

        Kunde kunde = new Kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim");
        kunde.setKundennummer(KUNDE_NR);
        KundenService kundenStub = new KundenService() {
            @Override
            public Kunde findeKunde(String kundennummer) {
                return KUNDE_NR.equals(kundennummer) ? kunde : null;
            }

            @Override
            public List<Kunde> suche(String suchbegriff) {
                return List.of(kunde);
            }
        };

        produkte = new HashMap<>();
        produkte.put(PRODUKT_NR, produkt(PRODUKT_NR, "Beratungsstunde", "50.00", "0.19"));
        ProduktService produktStub = new ProduktService() {
            @Override
            public Produkt findeProdukt(String produktnummer) {
                return produkte.get(produktnummer);
            }

            @Override
            public List<Produkt> suche(String suchbegriff) {
                return List.copyOf(produkte.values());
            }
        };

        PdfExporter pdfStub = (dokument, ziel) -> { };
        service = new StandardDokumentService(repository, nummernGenerator,
                kundenStub, produktStub, pdfStub);
    }

    private static Produkt produkt(String nummer, String bezeichnung, String preis, String steuersatz) {
        Produkt produkt = new Produkt(bezeichnung, new BigDecimal(preis), new BigDecimal(steuersatz));
        produkt.setProduktnummer(nummer);
        return produkt;
    }

    @Test
    @DisplayName("TC-01: Position 100.00 EUR @ 0.19 -> Steuer 19.00, Brutto 119.00")
    void tc01SteuerUndBruttoEinerPosition() {
        Dokumentposition position = new Dokumentposition(
                PRODUKT_NR, "Test", 1, new BigDecimal("100.00"), new BigDecimal("0.19"));
        assertEquals(new BigDecimal("19.00"), position.getSteuerbetrag());
        assertEquals(new BigDecimal("119.00"), position.getPositionssummeBrutto());
    }

    @Test
    @DisplayName("TC-02: Einzelpreis 50.00 EUR, Menge 3 -> Positionssumme 150.00")
    void tc02Positionssumme() {
        Dokumentposition position = new Dokumentposition(
                PRODUKT_NR, "Test", 3, new BigDecimal("50.00"), new BigDecimal("0.19"));
        assertEquals(new BigDecimal("150.00"), position.getPositionssummeNetto());
    }

    @Test
    @DisplayName("TC-03: Beleg mit 150.00 @ 0.19 und 50.00 @ 0.07 -> 200.00 / 32.00 / 232.00")
    void tc03BelegSummen() {
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer("R-2026-000001");
        rechnung.setzePositionen(List.of(
                new Dokumentposition("P-1", "A", 1, new BigDecimal("150.00"), new BigDecimal("0.19")),
                new Dokumentposition("P-2", "B", 1, new BigDecimal("50.00"), new BigDecimal("0.07"))));
        assertEquals(new BigDecimal("200.00"), rechnung.getSummeNetto());
        assertEquals(new BigDecimal("32.00"), rechnung.getSummeSteuer());
        assertEquals(new BigDecimal("232.00"), rechnung.getSummeBrutto());
    }

    @Test
    @DisplayName("TC-04: letzte Rechnungsnummer R-2026-000123 -> naechste R-2026-000124 (lückenlos)")
    void tc04LueckenloseRechnungsnummer() {
        repository.speichere(TestBelege.rechnung("R-2026-000123", DokumentStatus.OFFEN));
        EinfacherBelegnummernGenerator generator =
                EinfacherBelegnummernGenerator.ausRepository(repository);
        assertEquals("R-2026-000124", generator.naechsteNummer(Belegtyp.RECHNUNG, 2026));
    }

    @Test
    @DisplayName("TC-05: Zähler 7, Jahr 2026 -> R-2026-000007 (führende Nullen, String)")
    void tc05NummernFormat() {
        nummernGenerator.setzeZaehler(Belegtyp.RECHNUNG, 2026, 7);
        assertEquals("R-2026-000007", nummernGenerator.naechsteNummer(Belegtyp.RECHNUNG, 2026));
    }

    @Test
    @DisplayName("TC-06: kein Zahlungsziel -> Standard +14 Tage (GR-06)")
    void tc06StandardZahlungsziel() {
        Rechnung rechnung = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)),
                LocalDate.of(2026, 6, 9), null);
        assertEquals(LocalDate.of(2026, 6, 23), rechnung.getZahlungsziel());
    }

    @Test
    @DisplayName("TC-07: abweichendes Zahlungsziel wird übernommen")
    void tc07AbweichendesZahlungsziel() {
        Rechnung rechnung = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)),
                LocalDate.of(2026, 6, 9), LocalDate.of(2026, 7, 31));
        assertEquals(LocalDate.of(2026, 7, 31), rechnung.getZahlungsziel());
    }

    @Test
    @DisplayName("TC-08: Änderung einer versendeten Rechnung wirft IllegalStateException (GR-02)")
    void tc08UnveraenderlichkeitVersendet() {
        Rechnung rechnung = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)), LocalDate.of(2026, 6, 9), null);
        service.versende(rechnung.getBelegnummer());
        Rechnung versendet = (Rechnung) repository.findeNachNummer(rechnung.getBelegnummer());
        assertThrows(IllegalStateException.class, () -> versendet.setzePositionen(List.of(
                new Dokumentposition("P-9", "Neu", 1, new BigDecimal("1.00"), new BigDecimal("0.19")))));
    }

    @Test
    @DisplayName("TC-09: Storno einer offenen Rechnung -> STORNIERT, nicht mehr offen, mit Datum und Benutzer protokolliert (BA-14)")
    void tc09Storno() {
        Rechnung rechnung = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)), LocalDate.of(2026, 6, 9), null);
        service.storniere(rechnung.getBelegnummer());

        Rechnung storniert = (Rechnung) repository.findeNachNummer(rechnung.getBelegnummer());
        assertEquals(DokumentStatus.STORNIERT, storniert.getStatus());
        assertTrue(service.offeneRechnungen().stream()
                .noneMatch(r -> r.getBelegnummer().equals(rechnung.getBelegnummer())));
        assertNotNull(storniert.getStorniertAm());
        assertEquals(StandardDokumentService.SYSTEM_BENUTZER, storniert.getStorniertVon());
    }

    @Test
    @DisplayName("TC-10: AB aus Angebot übernimmt Kunde, Positionen, Mengen und Rückreferenz (GR-05)")
    void tc10FolgebelegAusAngebot() {
        produkte.put("P-000002", produkt("P-000002", "Zweitprodukt", "10.00", "0.07"));
        Angebot angebot = service.erstelleAngebot(KUNDE_NR, List.of(
                new Positionsangabe(PRODUKT_NR, 2),
                new Positionsangabe("P-000002", 5)), null);

        Dokument folgebeleg = service.erzeugeFolgebeleg(angebot.getBelegnummer());

        assertTrue(folgebeleg instanceof Auftragsbestaetigung);
        assertEquals(angebot.getBelegnummer(), folgebeleg.getVorgaengerNr());
        assertEquals(angebot.getKundenReferenz(), folgebeleg.getKundenReferenz());
        assertEquals(2, folgebeleg.getPositionen().size());
        assertEquals(2, folgebeleg.getPositionen().get(0).getMenge());
        assertEquals(5, folgebeleg.getPositionen().get(1).getMenge());
    }

    @Test
    @DisplayName("TC-11: Produktpreisänderung lässt bestehende Rechnung unverändert (Snapshot, GR-03)")
    void tc11Snapshot() {
        Rechnung rechnung = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)), LocalDate.of(2026, 6, 9), null);

        produkte.put(PRODUKT_NR, produkt(PRODUKT_NR, "Beratungsstunde", "80.00", "0.19"));

        Rechnung gelesen = (Rechnung) repository.findeNachNummer(rechnung.getBelegnummer());
        assertEquals(new BigDecimal("50.00"), gelesen.getPositionen().get(0).getEinzelpreisNetto());
    }

    @Test
    @DisplayName("TC-12: fehlender Kunde bzw. fehlende Position -> Validierungsfehler benennt Pflichtfeld")
    void tc12PflichtfeldValidierung() {
        ValidierungsException ohneKunde = assertThrows(ValidierungsException.class,
                () -> service.erstelleRechnung(null,
                        List.of(new Positionsangabe(PRODUKT_NR, 1)), LocalDate.now(), null));
        assertEquals("Kunde", ohneKunde.getFeldname());

        ValidierungsException ohnePosition = assertThrows(ValidierungsException.class,
                () -> service.erstelleRechnung(KUNDE_NR, List.of(), LocalDate.now(), null));
        assertEquals("Position", ohnePosition.getFeldname());
    }

    @Test
    @DisplayName("TC-13: vollständige Rechnung mit allen Pflichtangaben gemäß § 14 UStG")
    void tc13VollstaendigeRechnung() {
        Rechnung rechnung = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 2)), LocalDate.of(2026, 6, 9), null);

        Rechnung gespeichert = (Rechnung) repository.findeNachNummer(rechnung.getBelegnummer());
        assertNotNull(gespeichert);
        assertTrue(gespeichert.getBelegnummer().startsWith("R-2026-"));
        assertEquals(LocalDate.of(2026, 6, 9), gespeichert.getDatum());
        assertEquals(LocalDate.of(2026, 6, 9), gespeichert.getLeistungsdatum());
        assertEquals("Muster GmbH", gespeichert.getKundeName());
        assertEquals("Hauptstr. 1, 68163 Mannheim", gespeichert.getKundeAnschrift());
        assertEquals(1, gespeichert.getPositionen().size());
        assertEquals(new BigDecimal("0.19"), gespeichert.getPositionen().get(0).getSteuersatz());
        assertEquals(new BigDecimal("100.00"), gespeichert.getSummeNetto());
        assertEquals(new BigDecimal("19.00"), gespeichert.getSummeSteuer());
        assertEquals(new BigDecimal("119.00"), gespeichert.getSummeBrutto());
        assertNotNull(gespeichert.getZahlungsziel());
        assertFalse(service.offeneRechnungen().isEmpty());
    }
}
