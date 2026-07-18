package de.lucasstrubel.faktura.gemeinsam;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Hilfsfunktionen für den CSV-Export der Stammdaten
 * (B-F-15, C-F-15: UTF-8, Semikolon-getrennt, mit Kopfzeile).
 */
public final class Csv {

    public static final String TRENNZEICHEN = ";";

    private Csv() {
    }

    /** Maskiert einen Wert für CSV; {@code null} wird als leeres Feld geschrieben. */
    public static String feld(String wert) {
        if (wert == null) {
            return "";
        }
        if (wert.contains(TRENNZEICHEN) || wert.contains("\"") || wert.contains("\n")) {
            return "\"" + wert.replace("\"", "\"\"") + "\"";
        }
        return wert;
    }

    /** Schreibt die Zeilen als UTF-8-Datei in das lokale Dateisystem (IF-04). */
    public static void schreibe(Path zielDatei, List<String> zeilen) {
        try {
            if (zielDatei.getParent() != null) {
                Files.createDirectories(zielDatei.getParent());
            }
            Files.write(zielDatei, zeilen, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("CSV-Export fehlgeschlagen: " + zielDatei, e);
        }
    }
}
