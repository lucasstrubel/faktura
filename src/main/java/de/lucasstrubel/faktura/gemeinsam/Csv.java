package de.lucasstrubel.faktura.gemeinsam;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Hilfsfunktionen für den CSV-Export der Stamm- und Bewegungsdaten
 * (B-F-15, C-F-15, IF-04: UTF-8, Semikolon-getrennt, mit Kopfzeile).
 *
 * <p>Die Datei beginnt mit einer Byte-Order-Mark: Ohne sie liest Excel auf
 * einem deutschen System UTF-8 als Windows-1252 und zeigt Umlaute verstümmelt.
 * Textfelder, die eine Tabellenkalkulation als Formel ausführen würde, werden
 * mit einem Apostroph entschärft (CSV-Injection).
 */
public final class Csv {

    public static final String TRENNZEICHEN = ";";

    private static final char BYTE_ORDER_MARK = '﻿';

    /** Harmlose Werte mit führendem + oder -: Zahlen und Telefonnummern. */
    private static final Pattern ZAHL_ODER_TELEFON = Pattern.compile("[+-][0-9 ()/.,-]*");

    private Csv() {
    }

    /**
     * Maskiert einen Textwert für CSV; {@code null} wird als leeres Feld
     * geschrieben. Beginnt der Wert mit {@code = @ + -} (und ist keine Zahl
     * oder Telefonnummer), Tabulator oder Wagenrücklauf, wird ein Apostroph
     * vorangestellt.
     */
    public static String feld(String wert) {
        if (wert == null) {
            return "";
        }
        return maskiere(istFormelVerdaechtig(wert) ? "'" + wert : wert);
    }

    /** Maskiert einen Zahlenwert (z. B. einen negativen Betrag) ohne Formelschutz. */
    public static String zahl(String wert) {
        return wert == null ? "" : maskiere(wert);
    }

    /** Schreibt die Zeilen als UTF-8-Datei mit BOM in das lokale Dateisystem (IF-04). */
    public static void schreibe(Path zielDatei, List<String> zeilen) {
        try {
            Path zielVerzeichnis = zielDatei.getParent();
            if (zielVerzeichnis != null) {
                Files.createDirectories(zielVerzeichnis);
            }
            try (BufferedWriter schreiber = Files.newBufferedWriter(zielDatei, StandardCharsets.UTF_8)) {
                schreiber.write(BYTE_ORDER_MARK);
                for (String zeile : zeilen) {
                    schreiber.write(zeile);
                    schreiber.write("\r\n");
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("CSV-Export fehlgeschlagen: " + zielDatei, e);
        }
    }

    private static String maskiere(String wert) {
        if (wert.contains(TRENNZEICHEN) || wert.contains("\"")
                || wert.contains("\n") || wert.contains("\r")) {
            return "\"" + wert.replace("\"", "\"\"") + "\"";
        }
        return wert;
    }

    private static boolean istFormelVerdaechtig(String wert) {
        if (wert.isEmpty()) {
            return false;
        }
        char erstes = wert.charAt(0);
        return switch (erstes) {
            case '=', '@', '\t', '\r' -> true;
            case '+', '-' -> !ZAHL_ODER_TELEFON.matcher(wert).matches();
            default -> false;
        };
    }
}
