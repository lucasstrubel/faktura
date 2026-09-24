package de.lucasstrubel.faktura.gemeinsam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CSV-Hilfsfunktionen (IF-04): UTF-8 mit Byte-Order-Mark für Excel,
 * vollständige Maskierung und Schutz vor Formel-Injektion.
 */
class CsvTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("CSV-01: Die Datei beginnt mit einer UTF-8-Byte-Order-Mark und behält Umlaute")
    void byteOrderMark() throws Exception {
        Path ziel = tempDir.resolve("x.csv");
        Csv.schreibe(ziel, List.of("name", "Müller"));

        byte[] inhalt = Files.readAllBytes(ziel);
        assertEquals((byte) 0xEF, inhalt[0]);
        assertEquals((byte) 0xBB, inhalt[1]);
        assertEquals((byte) 0xBF, inhalt[2]);
        assertTrue(Files.readString(ziel, StandardCharsets.UTF_8).contains("Müller"));
    }

    @Test
    @DisplayName("CSV-02: Trennzeichen, Anführungszeichen und Zeilenumbrüche (auch einzelnes CR) werden maskiert")
    void maskierung() {
        assertEquals("\"a;b\"", Csv.feld("a;b"));
        assertEquals("\"sagt \"\"hallo\"\"\"", Csv.feld("sagt \"hallo\""));
        assertEquals("\"a\rb\"", Csv.feld("a\rb"));
        assertEquals("", Csv.feld(null));
    }

    @Test
    @DisplayName("CSV-03: Formeln werden entschärft, Zahlen und Telefonnummern bleiben unverändert")
    void formelInjektion() {
        // Entschärft und anschließend wegen der Anführungszeichen maskiert
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", Csv.feld("=HYPERLINK(\"x\")"));
        assertEquals("'@SUMME(A1)", Csv.feld("@SUMME(A1)"));
        assertEquals("'+cmd|' /C calc'!A0", Csv.feld("+cmd|' /C calc'!A0"));
        assertEquals("+49 621 123456", Csv.feld("+49 621 123456"));
        assertEquals("-12.50", Csv.feld("-12.50"));
        assertEquals("-12.50", Csv.zahl("-12.50"));
    }
}
