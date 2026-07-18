package de.lucasstrubel.faktura.dokumente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Datenexport der Bewegungsdaten (Q-08, IF-04): vollständiger CSV-Export
 * aller Belege mit einer Zeile je Position.
 */
class DokumentCsvExportTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("TC-18 (Q-08): Export schreibt Kopfzeile und je eine Zeile pro Dokumentposition")
    void exportiertAlleBelegeMitPositionen() throws IOException {
        JsonDokumentRepository repository = new JsonDokumentRepository(tempDir.resolve("dokumente.json"));
        repository.speichere(TestBelege.rechnung("R-2026-000001", DokumentStatus.OFFEN));
        Rechnung storniert = TestBelege.rechnung("R-2026-000002", DokumentStatus.OFFEN);
        storniert.storniere(java.time.LocalDate.of(2026, 6, 10), "Anwender");
        repository.speichere(storniert);

        Path ziel = tempDir.resolve("export/dokumente.csv");
        new DokumentCsvExport(repository).exportiereCsv(ziel);

        List<String> zeilen = Files.readAllLines(ziel, StandardCharsets.UTF_8);
        assertEquals(3, zeilen.size()); // Kopfzeile + 2 Rechnungen mit je 1 Position
        assertTrue(zeilen.get(0).startsWith("belegnummer;belegtyp;datum;status"));
        assertTrue(zeilen.stream().anyMatch(z -> z.contains("R-2026-000001")));
        assertTrue(zeilen.stream().anyMatch(z -> z.contains("STORNIERT") && z.contains("Anwender")));
    }
}
