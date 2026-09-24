package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.firma.Firmenprofil;
import de.lucasstrubel.faktura.firma.FirmenprofilService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modultestplan Komponente A (Dokument Modultestplan.md): TC-01 bis TC-13,
 * dazu die Weiterentwicklung 3.0 (DZ-01 bis DZ-10: Stornorechnung,
 * Zahlungseingang, Leistungsdatum, Datumsprüfung, Aussteller-Snapshot,
 * Pflichtprofil, Nummernschutz, Umsatzsteuer je Satz).
 * Die Schnittstellen der Komponenten B und C werden durch Stubs ersetzt,
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
    /** Aktuelles Firmenprofil des Stubs; {@code null} = noch keines gespeichert. */
    private Firmenprofil firmenprofil;

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

        firmenprofil = TestBelege.FIRMA;
        FirmenprofilService firmenStub = new FirmenprofilService(null) {
            @Override
            public Optional<Firmenprofil> lade() {
                return Optional.ofNullable(firmenprofil);
            }
        };

        PdfExporter pdfStub = (dokument, ziel) -> { };
        service = new StandardDokumentService(repository, nummernGenerator,
                kundenStub, produktStub, firmenStub, pdfStub, ereignis -> { });
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

    private Rechnung versendeteRechnung() {
        Rechnung rechnung = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 2)), LocalDate.now(), null);
        service.versende(rechnung.getBelegnummer());
        return rechnung;
    }

    @Test
    @DisplayName("DZ-01: Storno einer versendeten Rechnung erzeugt eine Stornorechnung mit negativen Mengen (A-F-29)")
    void dz01StornorechnungFuerVersendeteRechnung() {
        Rechnung original = versendeteRechnung();

        Rechnung storno = service.storniere(original.getBelegnummer());

        assertTrue(storno.istStornorechnung());
        assertEquals(original.getBelegnummer(), storno.getStornoZu());
        assertEquals(original.getBelegnummer(), storno.getVorgaengerNr());
        assertTrue(storno.getBelegnummer().startsWith("R-"), "eigene Nummer aus dem Rechnungskreis");
        assertFalse(storno.getBelegnummer().equals(original.getBelegnummer()));
        assertEquals(-2, storno.getPositionen().get(0).getMenge());
        assertEquals(new BigDecimal("-119.00"), storno.getSummeBrutto());
        assertEquals(original.getKundeName(), storno.getKundeName());
        assertNull(storno.getZahlungsziel(), "eine Stornorechnung wird nicht bezahlt");

        Rechnung gelesen = (Rechnung) repository.findeNachNummer(original.getBelegnummer());
        assertEquals(DokumentStatus.STORNIERT, gelesen.getStatus());
        assertEquals(new BigDecimal("119.00"), gelesen.getSummeBrutto(), "Original bleibt unverändert");
    }

    @Test
    @DisplayName("DZ-02: Eine offene Rechnung wird ohne Stornorechnung storniert; Stornorechnungen sind weder stornier- noch bezahlbar")
    void dz02StornoregelnOffenUndStornorechnung() {
        Rechnung offen = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)), LocalDate.now(), null);
        Rechnung ergebnis = service.storniere(offen.getBelegnummer());
        assertFalse(ergebnis.istStornorechnung());
        assertEquals(1, repository.alle().size(), "keine zusätzliche Stornorechnung");

        Rechnung storno = service.storniere(versendeteRechnung().getBelegnummer());
        assertThrows(IllegalStateException.class, () -> service.storniere(storno.getBelegnummer()));
        assertThrows(IllegalStateException.class,
                () -> service.markiereBezahlt(storno.getBelegnummer(), LocalDate.now()));
    }

    @Test
    @DisplayName("DZ-03: Zahlungseingang einer versendeten Rechnung wird erfasst, der Inhalt bleibt gesperrt (A-F-28)")
    void dz03Zahlungseingang() {
        Rechnung rechnung = versendeteRechnung();
        LocalDate heute = LocalDate.now();

        service.markiereBezahlt(rechnung.getBelegnummer(), heute);

        Rechnung gelesen = (Rechnung) repository.findeNachNummer(rechnung.getBelegnummer());
        assertTrue(gelesen.istBezahlt());
        assertEquals(heute, gelesen.getBezahltAm());
        assertEquals(DokumentStatus.VERSENDET, gelesen.getStatus());
        assertThrows(IllegalStateException.class, () -> gelesen.setZahlungsziel(heute));
        assertThrows(IllegalStateException.class,
                () -> service.markiereBezahlt(rechnung.getBelegnummer(), heute), "nur einmal");
        assertThrows(IllegalStateException.class,
                () -> service.storniere(rechnung.getBelegnummer()), "bezahlt = nicht stornierbar");
    }

    @Test
    @DisplayName("DZ-04: Zahlungsdatum ist Pflicht und darf nicht vor dem Rechnungsdatum liegen (A-F-28)")
    void dz04Zahlungsdatum() {
        Rechnung rechnung = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)), LocalDate.of(2026, 6, 9), null);
        assertEquals("Zahlungsdatum", assertThrows(ValidierungsException.class,
                () -> service.markiereBezahlt(rechnung.getBelegnummer(), null)).getFeldname());
        assertEquals("Zahlungsdatum", assertThrows(ValidierungsException.class,
                () -> service.markiereBezahlt(rechnung.getBelegnummer(), LocalDate.of(2026, 6, 1)))
                .getFeldname());
    }

    @Test
    @DisplayName("DZ-05: Rechnung aus Lieferschein übernimmt das Lieferdatum als Leistungsdatum (A-F-31)")
    void dz05LeistungsdatumAusLieferschein() {
        LocalDate lieferdatum = LocalDate.now().minusDays(20);
        Lieferschein lieferschein = service.erstelleLieferschein(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)), lieferdatum);

        Rechnung rechnung = (Rechnung) service.erzeugeFolgebeleg(lieferschein.getBelegnummer());

        assertEquals(lieferdatum, rechnung.getLeistungsdatum());
        assertEquals(LocalDate.now(), rechnung.getDatum());
    }

    @Test
    @DisplayName("DZ-06: Abweichendes Leistungsdatum wird übernommen; Zahlungsziel/Gültigkeit vor dem Belegdatum abgelehnt (A-F-31, A-F-32)")
    void dz06DatumsangabenWerdenGeprueft() {
        Rechnung rechnung = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)), LocalDate.of(2026, 6, 9),
                LocalDate.of(2026, 5, 31), null);
        assertEquals(LocalDate.of(2026, 5, 31), rechnung.getLeistungsdatum());

        assertEquals("Zahlungsziel", assertThrows(ValidierungsException.class,
                () -> service.erstelleRechnung(KUNDE_NR, List.of(new Positionsangabe(PRODUKT_NR, 1)),
                        LocalDate.of(2026, 6, 9), LocalDate.of(2026, 6, 1))).getFeldname());
        assertEquals("Gültig bis", assertThrows(ValidierungsException.class,
                () -> service.erstelleAngebot(KUNDE_NR, List.of(new Positionsangabe(PRODUKT_NR, 1)),
                        LocalDate.now().minusDays(1))).getFeldname());
    }

    @Test
    @DisplayName("DZ-07: Belege speichern das Firmenprofil als Snapshot; spätere Änderungen wirken nicht zurück (A-F-27)")
    void dz07AusstellerSnapshot() {
        Rechnung rechnung = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)), LocalDate.now(), null);

        firmenprofil = new Firmenprofil("Umbenannt GmbH", "Neuweg 9", "69115", "Heidelberg",
                "DE123456789", null, null, null, null, null, null);

        Rechnung gelesen = (Rechnung) repository.findeNachNummer(rechnung.getBelegnummer());
        assertEquals(TestBelege.FIRMA, gelesen.getAussteller());
    }

    @Test
    @DisplayName("DZ-08: Ohne Firmenprofil bzw. ohne Steuerkennung kein Beleg — und keine verbrauchte Nummer (A-F-26, GR-01)")
    void dz08PflichtprofilOhneNummernverbrauch() {
        firmenprofil = null;
        assertEquals("Firmenprofil", assertThrows(ValidierungsException.class,
                () -> service.erstelleAngebot(KUNDE_NR, List.of(new Positionsangabe(PRODUKT_NR, 1)), null))
                .getFeldname());

        firmenprofil = new Firmenprofil("Ohne Steuer", "Weg 1", "68163", "Mannheim",
                null, null, null, null, null, null, null);
        assertEquals("Steuernummer", assertThrows(ValidierungsException.class,
                () -> service.erstelleRechnung(KUNDE_NR, List.of(new Positionsangabe(PRODUKT_NR, 1)),
                        LocalDate.of(2026, 6, 9), null)).getFeldname());

        firmenprofil = TestBelege.FIRMA;
        Rechnung rechnung = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)), LocalDate.of(2026, 6, 9), null);
        assertEquals("R-2026-000001", rechnung.getBelegnummer());
    }

    @Test
    @DisplayName("DZ-09: Liefert der Nummernkreis eine vergebene Nummer, wird nichts überschrieben (GR-01, GR-02)")
    void dz09VergebeneNummerWirdNichtUeberschrieben() {
        Rechnung erste = service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 1)), LocalDate.of(2026, 6, 9), null);
        nummernGenerator.setzeZaehler(Belegtyp.RECHNUNG, 2026, 1);

        assertThrows(IllegalStateException.class, () -> service.erstelleRechnung(KUNDE_NR,
                List.of(new Positionsangabe(PRODUKT_NR, 5)), LocalDate.of(2026, 6, 9), null));
        Rechnung gelesen = (Rechnung) repository.findeNachNummer(erste.getBelegnummer());
        assertEquals(1, gelesen.getPositionen().get(0).getMenge());
    }

    @Test
    @DisplayName("DZ-10: Umsatzsteuer je Steuersatz — 7 % und 19 % getrennt ausgewiesen (A-F-25)")
    void dz10SteueraufschluesselungJeSatz() {
        Rechnung rechnung = new Rechnung();
        rechnung.setzePositionen(List.of(
                new Dokumentposition("P-1", "A", 3, new BigDecimal("0.35"), new BigDecimal("0.19")),
                new Dokumentposition("P-2", "B", 1, new BigDecimal("0.35"), new BigDecimal("0.19")),
                new Dokumentposition("P-3", "C", 1, new BigDecimal("50.00"), new BigDecimal("0.07"))));

        List<Steuerzeile> zeilen = rechnung.steueraufschluesselung();
        assertEquals(2, zeilen.size());
        assertEquals(new BigDecimal("50.00"), zeilen.get(0).netto());
        assertEquals(new BigDecimal("3.50"), zeilen.get(0).steuer());
        assertEquals(new BigDecimal("1.40"), zeilen.get(1).netto());
        // 1,40 × 0,19 = 0,266 → 0,27, einmal je Satz gerundet
        assertEquals(new BigDecimal("0.27"), zeilen.get(1).steuer());
        assertEquals(new BigDecimal("3.77"), rechnung.getSummeSteuer());
        assertEquals(new BigDecimal("55.17"), rechnung.getSummeBrutto());
    }
}
