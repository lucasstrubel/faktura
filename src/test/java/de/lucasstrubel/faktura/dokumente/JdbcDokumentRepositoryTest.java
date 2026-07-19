package de.lucasstrubel.faktura.dokumente;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rundreise-Tests der SQLite-Belegpersistenz (IF-01): Polymorphie über die
 * Diskriminatorspalte, Positionslisten, BigDecimal-Beträge ohne Verlust und
 * Wiederherstellung unveränderlicher Belege (GR-02).
 */
class JdbcDokumentRepositoryTest {

    @TempDir
    Path tempDir;

    private JdbcDokumentRepository repository;

    @BeforeEach
    void setUp() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("test.db"));
        Flyway.configure().dataSource(dataSource).load().migrate();
        repository = new JdbcDokumentRepository(new JdbcTemplate(dataSource));
    }

    private static Rechnung rechnung(String nummer) {
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer(nummer);
        rechnung.setDatum(LocalDate.of(2026, 6, 9));
        rechnung.setzeKunde("K-000017", "Muster GmbH", "Hauptstr. 1, 68163 Mannheim");
        rechnung.setLeistungsdatum(LocalDate.of(2026, 6, 8));
        rechnung.setZahlungsziel(LocalDate.of(2026, 6, 23));
        rechnung.setzePositionen(List.of(
                new Dokumentposition("P-000042", "Beratungsstunde", 2,
                        new BigDecimal("80.00"), new BigDecimal("0.19")),
                new Dokumentposition("P-000043", "Fahrtkosten", 1,
                        new BigDecimal("40.00"), new BigDecimal("0.19"))));
        return rechnung;
    }

    @Test
    @DisplayName("JDB-01: Rechnung mit Positionen wird verlustfrei gespeichert und geladen")
    void rechnungRundreise() {
        repository.speichere(rechnung("R-2026-000124"));

        Rechnung geladen = (Rechnung) repository.findeNachNummer("R-2026-000124");
        assertEquals("R-2026-000124", geladen.getBelegnummer());
        assertEquals("Muster GmbH", geladen.getKundeName());
        assertEquals(LocalDate.of(2026, 6, 23), geladen.getZahlungsziel());
        assertEquals(2, geladen.getPositionen().size());
        assertEquals(new BigDecimal("200.00"), geladen.getSummeNetto());
        assertEquals(new BigDecimal("38.00"), geladen.getSummeSteuer());
        assertEquals(new BigDecimal("238.00"), geladen.getSummeBrutto());
        assertEquals(new BigDecimal("80.00"), geladen.getPositionen().get(0).getEinzelpreisNetto());
    }

    @Test
    @DisplayName("JDB-02: versendete Rechnung bleibt nach dem Laden unveränderlich (GR-02)")
    void versendeteRechnungBleibtUnveraenderlich() {
        Rechnung rechnung = rechnung("R-2026-000125");
        rechnung.setzeStatus(DokumentStatus.OFFEN);
        rechnung.versende();
        repository.speichere(rechnung);

        Dokument geladen = repository.findeNachNummer("R-2026-000125");
        assertEquals(DokumentStatus.VERSENDET, geladen.getStatus());
        assertThrows(IllegalStateException.class,
                () -> geladen.setzeKunde("K-000001", "Anders", "Anderswo"));
    }

    @Test
    @DisplayName("JDB-03: stornierte Rechnung behält Stornodatum und -benutzer (BA-14)")
    void stornierteRechnungBehaeltProtokoll() {
        Rechnung rechnung = rechnung("R-2026-000126");
        rechnung.setzeStatus(DokumentStatus.OFFEN);
        rechnung.storniere(LocalDate.of(2026, 7, 1), "Anwender");
        repository.speichere(rechnung);

        Rechnung geladen = (Rechnung) repository.findeNachNummer("R-2026-000126");
        assertEquals(DokumentStatus.STORNIERT, geladen.getStatus());
        assertEquals(LocalDate.of(2026, 7, 1), geladen.getStorniertAm());
        assertEquals("Anwender", geladen.getStorniertVon());
    }

    @Test
    @DisplayName("JDB-04: alle vier Belegtypen werden über den Diskriminator korrekt geladen")
    void polymorphieUeberDiskriminator() {
        Angebot angebot = new Angebot();
        angebot.setBelegnummer("AN-2026-000001");
        angebot.setDatum(LocalDate.of(2026, 6, 1));
        angebot.setzeKunde("K-000017", "Muster GmbH", "Hauptstr. 1, 68163 Mannheim");
        angebot.setGueltigBis(LocalDate.of(2026, 7, 1));
        angebot.setzePositionen(List.of(new Dokumentposition("P-000042", "Beratung", 1,
                new BigDecimal("80.00"), new BigDecimal("0.19"))));
        repository.speichere(angebot);

        Lieferschein lieferschein = new Lieferschein();
        lieferschein.setBelegnummer("LS-2026-000001");
        lieferschein.setDatum(LocalDate.of(2026, 6, 2));
        lieferschein.setzeKunde("K-000017", "Muster GmbH", "Hauptstr. 1, 68163 Mannheim");
        lieferschein.setLieferdatum(LocalDate.of(2026, 6, 3));
        lieferschein.setzePositionen(List.of(new Dokumentposition("P-000042", "Beratung", 1,
                new BigDecimal("80.00"), new BigDecimal("0.19"))));
        repository.speichere(lieferschein);

        List<Dokument> alle = repository.alle();
        assertEquals(2, alle.size());
        assertTrue(alle.get(0) instanceof Angebot geladenesAngebot
                && LocalDate.of(2026, 7, 1).equals(geladenesAngebot.getGueltigBis()));
        assertTrue(alle.get(1) instanceof Lieferschein geladenerLieferschein
                && LocalDate.of(2026, 6, 3).equals(geladenerLieferschein.getLieferdatum()));
    }

    @Test
    @DisplayName("JDB-05: unbekannte Belegnummer liefert null; Speichern aktualisiert per Upsert")
    void unbekannteNummerUndUpsert() {
        assertNull(repository.findeNachNummer("R-9999-000001"));

        Rechnung rechnung = rechnung("R-2026-000127");
        repository.speichere(rechnung);
        rechnung.setzePositionen(List.of(new Dokumentposition("P-000042", "Beratungsstunde", 5,
                new BigDecimal("80.00"), new BigDecimal("0.19"))));
        repository.speichere(rechnung);

        Dokument geladen = repository.findeNachNummer("R-2026-000127");
        assertEquals(1, geladen.getPositionen().size());
        assertEquals(new BigDecimal("400.00"), geladen.getSummeNetto());
    }
}
