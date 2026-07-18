package de.lucasstrubel.faktura.kunden;

import de.lucasstrubel.faktura.gemeinsam.LoeschAbgelehntException;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modultestplan Komponente C (Pflichtenheft C, Kapitel 10): TC-01 bis TC-14,
 * ergänzt um TC-15 bis TC-17 für die erweiterte Formatvalidierung (C-F-16 bis C-F-18).
 * Die Schnittstelle {@code KundenReferenzPruefung} (Komponente A) wird durch
 * einen Stub ersetzt.
 */
class KundenVerwaltungTest {

    @TempDir
    Path tempDir;

    private JsonKundenRepository repository;
    private final Map<String, Integer> verknuepfteDokumente = new HashMap<>();

    @BeforeEach
    void setUp() {
        repository = new JsonKundenRepository(tempDir.resolve("kunden.json"));
    }

    private KundenVerwaltungsService service(KundennummernGenerator generator) {
        return new KundenVerwaltungsService(repository, generator,
                kundennummer -> verknuepfteDokumente.getOrDefault(kundennummer, 0));
    }

    private KundenVerwaltungsService serviceAusRepository() {
        return service(EinfacherKundennummernGenerator.ausRepository(repository));
    }

    private static Kunde kunde(String name, String strasse, String plz, String ort) {
        return new Kunde(name, strasse, plz, ort);
    }

    private Kunde lege(String nummer, String name) {
        Kunde kunde = kunde(name, "Hauptstr. 1", "68163", "Mannheim");
        kunde.setKundennummer(nummer);
        return repository.speichere(kunde);
    }

    @Test
    @DisplayName("TC-01: höchste Nummer K-000016 -> neuer Kunde erhält K-000017")
    void tc01NummernVergabe() {
        lege("K-000016", "Bestehender Kunde");
        Kunde gespeichert = serviceAusRepository()
                .legeAn(kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim"));
        assertEquals("K-000017", gespeichert.getKundennummer());
        assertNotNull(repository.findeNachNummer("K-000017"));
    }

    @Test
    @DisplayName("TC-02: Zähler 7 -> K-000007 (führende Nullen, String)")
    void tc02NummernFormat() {
        assertEquals("K-000007", new EinfacherKundennummernGenerator(7).naechsteNummer());
    }

    @Test
    @DisplayName("TC-03: fehlender Ort wird abgelehnt und benannt (Q-09)")
    void tc03FehlenderOrt() {
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> serviceAusRepository().legeAn(kunde("Muster GmbH", "Hauptstr. 1", "68163", null)));
        assertEquals("Ort", fehler.getFeldname());
    }

    @Test
    @DisplayName("TC-04: leerer Name wird abgelehnt und benannt")
    void tc04LeererName() {
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> serviceAusRepository().legeAn(kunde("", "Hauptstr. 1", "68163", "Mannheim")));
        assertEquals("Name", fehler.getFeldname());
    }

    @Test
    @DisplayName("TC-05: ungültige E-Mail 'max.mustermann' wird abgelehnt (C-F-04)")
    void tc05UngueltigeEMail() {
        Kunde kunde = kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim");
        kunde.setEMail("max.mustermann");
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> serviceAusRepository().legeAn(kunde));
        assertEquals("E-Mail", fehler.getFeldname());
    }

    @Test
    @DisplayName("TC-06: gültige E-Mail 'max@beispiel.de' wird gespeichert")
    void tc06GueltigeEMail() {
        Kunde kunde = kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim");
        kunde.setEMail("max@beispiel.de");
        Kunde gespeichert = serviceAusRepository().legeAn(kunde);
        assertEquals("max@beispiel.de",
                repository.findeNachNummer(gespeichert.getKundennummer()).getEMail());
    }

    @Test
    @DisplayName("TC-07: Ortsänderung Mannheim -> Heidelberg wird gespeichert")
    void tc07OrtAendern() {
        KundenVerwaltungsService service = serviceAusRepository();
        Kunde kunde = service.legeAn(kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim"));

        kunde.setOrt("Heidelberg");
        service.aendere(kunde);

        assertEquals("Heidelberg", repository.findeNachNummer(kunde.getKundennummer()).getOrt());
    }

    @Test
    @DisplayName("TC-08: Änderungsversuch der Kundennummer wirft IllegalArgumentException")
    void tc08KundennummerUnveraenderlich() {
        Kunde kunde = serviceAusRepository().legeAn(kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim"));
        assertThrows(IllegalArgumentException.class,
                () -> kunde.setKundennummer("K-999999"));
    }

    @Test
    @DisplayName("TC-09: unverknüpfter Kunde wird nach Bestätigung gelöscht")
    void tc09LoeschenUnverknuepft() {
        lege("K-000011", "Unverknüpft");
        serviceAusRepository().loescheKunde("K-000011");
        assertTrue(repository.alleSortiertNachName().stream()
                .noneMatch(k -> k.getKundennummer().equals("K-000011")));
    }

    @Test
    @DisplayName("TC-10: Kunde mit 3 verknüpften Dokumenten wird nicht gelöscht; Hinweis nennt Anzahl (GR-04)")
    void tc10Loeschsperre() {
        lege("K-000010", "Referenziert");
        verknuepfteDokumente.put("K-000010", 3);

        LoeschAbgelehntException fehler = assertThrows(LoeschAbgelehntException.class,
                () -> serviceAusRepository().loescheKunde("K-000010"));

        assertNotNull(repository.findeNachNummer("K-000010"));
        assertTrue(fehler.getMessage().contains("3"));
    }

    @Test
    @DisplayName("TC-11: Auflistung sortiert nach Name")
    void tc11Sortierung() {
        lege("K-000001", "Zimmer");
        lege("K-000002", "Albrecht");
        lege("K-000003", "Maier");

        List<String> namen = repository.alleSortiertNachName().stream()
                .map(Kunde::getName)
                .toList();
        assertEquals(List.of("Albrecht", "Maier", "Zimmer"), namen);
    }

    @Test
    @DisplayName("TC-12: Suche ist case-insensitive und findet Teilstrings")
    void tc12SucheName() {
        lege("K-000001", "Muster GmbH");
        List<Kunde> treffer = serviceAusRepository().suche("MUSTER");
        assertTrue(treffer.stream().anyMatch(k -> k.getName().equals("Muster GmbH")));
    }

    @Test
    @DisplayName("TC-13: Suche nach Kundennummer trifft; findeKunde liefert null für Unbekannte (C-F-14)")
    void tc13SucheNummerUndFindeKunde() {
        lege("K-000017", "Muster GmbH");
        KundenVerwaltungsService service = serviceAusRepository();

        List<Kunde> treffer = service.suche("K-000017");
        assertTrue(treffer.stream().anyMatch(k -> k.getKundennummer().equals("K-000017")));
        assertNull(service.findeKunde("K-999999"));
    }

    @Test
    @DisplayName("TC-14: CSV-Export mit Kopfzeile, Semikolon-getrennt, UTF-8 (C-F-15)")
    void tc14CsvExport() throws Exception {
        lege("K-000001", "Albrecht");
        lege("K-000002", "Maier");
        lege("K-000003", "Zimmer");

        Path ziel = tempDir.resolve("kunden.csv");
        new KundenCsvExport(repository).exportiereCsv(ziel);

        List<String> zeilen = Files.readAllLines(ziel, StandardCharsets.UTF_8);
        assertEquals(4, zeilen.size());
        assertEquals("kundennummer;name;strasse;plz;ort;eMail;telefon;ustIdNr", zeilen.get(0));
        assertTrue(zeilen.get(1).startsWith("K-000001;Albrecht;"));
    }

    @Test
    @DisplayName("TC-15: ungültige PLZ '123' wird beim Anlegen abgelehnt und benannt (C-F-16)")
    void tc15UngueltigePlz() {
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> serviceAusRepository().legeAn(kunde("Muster GmbH", "Hauptstr. 1", "123", "Mannheim")));
        assertEquals("PLZ", fehler.getFeldname());
    }

    @Test
    @DisplayName("TC-16: ungültige USt-IdNr. wird beim Anlegen abgelehnt und benannt (C-F-17)")
    void tc16UngueltigeUstIdNr() {
        Kunde kunde = kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim");
        kunde.setUstIdNr("AT123456789");
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> serviceAusRepository().legeAn(kunde));
        assertEquals("USt-IdNr.", fehler.getFeldname());
    }

    @Test
    @DisplayName("TC-17: ungültiges Telefon wird beim Ändern abgelehnt und benannt (C-F-18)")
    void tc17UngueltigesTelefon() {
        Kunde kunde = lege("K-000017", "Muster GmbH");
        kunde.setTelefon("keine-nummer");
        ValidierungsException fehler = assertThrows(ValidierungsException.class,
                () -> serviceAusRepository().aendere(kunde));
        assertEquals("Telefon", fehler.getFeldname());
    }
}
