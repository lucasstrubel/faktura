package de.lucasstrubel.faktura.produkte;

import de.lucasstrubel.faktura.gemeinsam.LoeschAbgelehntException;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modultestplan Komponente B (Pflichtenheft B, Kapitel 10): TC-01 bis TC-14.
 * Die Schnittstelle {@code ProduktReferenzPruefung} (Komponente A) wird durch
 * einen Stub ersetzt.
 */
class ProduktVerwaltungTest {

    @TempDir
    Path tempDir;

    private JsonProduktRepository repository;
    private final Set<String> referenzierteProdukte = new HashSet<>();

    @BeforeEach
    void setUp() {
        repository = new JsonProduktRepository(tempDir.resolve("produkte.json"));
    }

    private ProduktVerwaltungsService service(ProduktnummernGenerator generator) {
        return new ProduktVerwaltungsService(repository, generator, referenzierteProdukte::contains);
    }

    private ProduktVerwaltungsService serviceAusRepository() {
        return service(EinfacherProduktnummernGenerator.ausRepository(repository));
    }

    private static Produkt produkt(String bezeichnung, String preis, String steuersatz) {
        return new Produkt(bezeichnung, new BigDecimal(preis), new BigDecimal(steuersatz));
    }

    private Produkt lege(String nummer, String bezeichnung) {
        Produkt produkt = produkt(bezeichnung, "10.00", "0.19");
        produkt.setProduktnummer(nummer);
        return repository.speichere(produkt);
    }

    @Test
    @DisplayName("TC-01: höchste Nummer P-000041 -> neues Produkt erhält P-000042")
    void tc01NummernVergabe() {
        lege("P-000041", "Bestehendes Produkt");
        Produkt gespeichert = serviceAusRepository()
                .legeAn(produkt("Beratungsstunde", "80.00", "0.19"));
        assertEquals("P-000042", gespeichert.getProduktnummer());
        assertNotNull(repository.findeNachNummer("P-000042"));
    }

    @Test
    @DisplayName("TC-02: Zähler 7 -> P-000007 (führende Nullen, String)")
    void tc02NummernFormat() {
        assertEquals("P-000007", new EinfacherProduktnummernGenerator(7).naechsteNummer());
    }

    @Test
    @DisplayName("TC-03: negativer Einzelpreis wird abgelehnt")
    void tc03NegativerPreis() {
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> serviceAusRepository().legeAn(produkt("Test", "-1.00", "0.19")));
        assertEquals("Einzelpreis", fehler.getFeldname());
    }

    @Test
    @DisplayName("TC-04: unzulässiger Steuersatz 0.15 wird abgelehnt")
    void tc04UnzulaessigerSteuersatz() {
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> serviceAusRepository().legeAn(produkt("Test", "10.00", "0.15")));
        assertEquals("Steuersatz", fehler.getFeldname());
    }

    @Test
    @DisplayName("TC-05: fehlende Bezeichnung wird abgelehnt und benannt (Q-09)")
    void tc05FehlendeBezeichnung() {
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> serviceAusRepository().legeAn(produkt(null, "10.00", "0.19")));
        assertEquals("Bezeichnung", fehler.getFeldname());
    }

    @Test
    @DisplayName("TC-06: Preisänderung 80.00 -> 95.00 wird gespeichert")
    void tc06PreisAendern() {
        ProduktVerwaltungsService service = serviceAusRepository();
        Produkt produkt = service.legeAn(produkt("Beratungsstunde", "80.00", "0.19"));

        produkt.setEinzelpreisNetto(new BigDecimal("95.00"));
        service.aendere(produkt);

        assertEquals(new BigDecimal("95.00"),
                repository.findeNachNummer(produkt.getProduktnummer()).getEinzelpreisNetto());
    }

    @Test
    @DisplayName("TC-07: Änderungsversuch der Produktnummer wirft IllegalArgumentException")
    void tc07ProduktnummerUnveraenderlich() {
        Produkt produkt = serviceAusRepository().legeAn(produkt("Beratungsstunde", "80.00", "0.19"));
        assertThrows(IllegalArgumentException.class,
                () -> produkt.setProduktnummer("P-999999"));
    }

    @Test
    @DisplayName("TC-08: unverknüpftes Produkt wird nach Bestätigung gelöscht")
    void tc08LoeschenUnverknuepft() {
        lege("P-000011", "Unverknüpft");
        serviceAusRepository().loescheProdukt("P-000011");
        assertTrue(repository.alleSortiertNachBezeichnung().stream()
                .noneMatch(p -> p.getProduktnummer().equals("P-000011")));
    }

    @Test
    @DisplayName("TC-09: referenziertes Produkt wird nicht gelöscht (Löschsperre)")
    void tc09Loeschsperre() {
        lege("P-000010", "Referenziert");
        referenzierteProdukte.add("P-000010");

        LoeschAbgelehntException fehler = assertThrows(LoeschAbgelehntException.class,
                () -> serviceAusRepository().loescheProdukt("P-000010"));

        assertNotNull(repository.findeNachNummer("P-000010"));
        assertTrue(fehler.getMessage().contains("P-000010"));
    }

    @Test
    @DisplayName("TC-10: Auflistung sortiert nach Bezeichnung")
    void tc10Sortierung() {
        lege("P-000001", "Zaun");
        lege("P-000002", "Anker");
        lege("P-000003", "Mast");

        List<String> bezeichnungen = repository.alleSortiertNachBezeichnung().stream()
                .map(Produkt::getBezeichnung)
                .toList();
        assertEquals(List.of("Anker", "Mast", "Zaun"), bezeichnungen);
    }

    @Test
    @DisplayName("TC-11: Suche ist case-insensitive und findet Teilstrings")
    void tc11SucheBezeichnung() {
        lege("P-000001", "Beratungsstunde");
        List<Produkt> treffer = serviceAusRepository().suche("BERATUNG");
        assertTrue(treffer.stream().anyMatch(p -> p.getBezeichnung().equals("Beratungsstunde")));
    }

    @Test
    @DisplayName("TC-12: Suche nach Produktnummer findet genau das Produkt")
    void tc12SucheNummer() {
        lege("P-000042", "Beratungsstunde");
        lege("P-000043", "Anderes Produkt");

        List<Produkt> treffer = serviceAusRepository().suche("P-000042");
        assertEquals(1, treffer.size());
        assertEquals("P-000042", treffer.get(0).getProduktnummer());
    }

    @Test
    @DisplayName("TC-13: findeProdukt liefert null für unbekannte Nummer (B-F-14)")
    void tc13FindeProduktNull() {
        assertNull(serviceAusRepository().findeProdukt("P-999999"));
    }

    @Test
    @DisplayName("TC-14: CSV-Export mit Kopfzeile, Semikolon-getrennt, UTF-8 (B-F-15)")
    void tc14CsvExport() throws Exception {
        lege("P-000001", "Anker");
        lege("P-000002", "Mast");
        lege("P-000003", "Zaun");

        Path ziel = tempDir.resolve("produkte.csv");
        new ProduktCsvExport(repository).exportiereCsv(ziel);

        List<String> zeilen = Files.readAllLines(ziel, StandardCharsets.UTF_8);
        assertEquals(4, zeilen.size());
        assertEquals("produktnummer;bezeichnung;beschreibung;einzelpreisNetto;steuersatz;einheit",
                zeilen.get(0));
        assertTrue(zeilen.get(1).startsWith("P-000001;Anker;"));
    }
}
