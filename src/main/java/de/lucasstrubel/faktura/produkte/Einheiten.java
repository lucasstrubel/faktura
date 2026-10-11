package de.lucasstrubel.faktura.produkte;

import java.util.List;
import java.util.Locale;

/**
 * Unterstützte Mengeneinheiten und ihre unveränderlichen Codes nach UN/ECE
 * Recommendation 20 für die E-Rechnung (A-F-33). Freie Einheiten ohne Code
 * bleiben erlaubt, taugen aber nur für das PDF.
 */
public final class Einheiten {

    /** Einheit für Produkte ohne Angabe — entspricht dem Verhalten vor 3.1 (C62). */
    public static final String STANDARD = "Stück";

    public static final List<String> VORGABEN = List.of(STANDARD, "Stunde", "Tag", "Monat", "Pauschale",
            "Kilogramm", "Gramm", "Meter", "Liter");

    private Einheiten() {
    }

    public static String normalisiere(String wert) {
        if (wert == null || wert.isBlank()) {
            return null;
        }
        return switch (wert.strip().toLowerCase(Locale.ROOT)) {
            case "stück", "stueck", "stk", "stk.", "c62" -> STANDARD;
            case "stunde", "stunden", "std", "std.", "h", "hur" -> "Stunde";
            case "tag", "tage", "day" -> "Tag";
            case "monat", "monate", "mon" -> "Monat";
            case "pauschale", "pauschal", "psch", "psch.", "ls" -> "Pauschale";
            case "kilogramm", "kg", "kgm" -> "Kilogramm";
            case "gramm", "g", "grm" -> "Gramm";
            case "meter", "m", "mtr" -> "Meter";
            case "liter", "l", "ltr" -> "Liter";
            default -> wert.strip();
        };
    }

    /**
     * Einheit für den Belegsnapshot: normalisiert, ohne Angabe {@link #STANDARD}.
     * Gilt nur für neu entstehende Positionen; Altbestände bleiben unverändert.
     */
    public static String fuerBeleg(String wert) {
        String einheit = normalisiere(wert);
        return einheit == null ? STANDARD : einheit;
    }

    public static String code(String wert) {
        String einheit = normalisiere(wert);
        if (einheit == null) {
            return null;
        }
        return switch (einheit) {
            case STANDARD -> "C62";
            case "Stunde" -> "HUR";
            case "Tag" -> "DAY";
            case "Monat" -> "MON";
            case "Pauschale" -> "LS";
            case "Kilogramm" -> "KGM";
            case "Gramm" -> "GRM";
            case "Meter" -> "MTR";
            case "Liter" -> "LTR";
            default -> null;
        };
    }
}
