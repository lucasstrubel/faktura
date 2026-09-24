package de.lucasstrubel.faktura.gemeinsam;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Zentrale Eingabevalidierung der Stammdaten (Q-09; C-F-03, C-F-04 sowie die
 * erweiterten Formatprüfungen C-F-16 bis C-F-20). Jede Prüfung benennt bei
 * Ablehnung das betroffene Feld über eine {@link ValidierungsException}.
 *
 * <p>Optionale Felder (E-Mail, Telefon, USt-IdNr., IBAN, BIC, Steuernummer)
 * gelten als gültig, wenn sie leer sind; geprüft wird nur ein vorhandener
 * Wert. Die {@code normalisiere…}-Methoden liefern die Schreibweise, in der
 * ein Wert gespeichert wird (C-F-19) — so landet auf Beleg und E-Rechnung
 * dieselbe Form, die auch geprüft wurde.
 */
public final class Validierung {

    /** Lokalteil@Domain mit mindestens einer Top-Level-Domain (C-F-04). */
    private static final Pattern E_MAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}");

    /** Deutsche Postleitzahl: genau 5 Ziffern, führende Nullen erlaubt (C-F-16). */
    private static final Pattern PLZ = Pattern.compile("\\d{5}");

    /**
     * USt-IdNr. der EU-Mitgliedstaaten nach den Formaten des
     * MwSt-Informationsaustauschsystems (VIES); Schlüssel ist der Länderpräfix
     * (C-F-17). Kunden im EU-Ausland tragen ihre eigene Nummer.
     */
    private static final Map<String, Pattern> UST_ID_NR = Map.ofEntries(
            Map.entry("DE", Pattern.compile("\\d{9}")),
            Map.entry("AT", Pattern.compile("U\\d{8}")),
            Map.entry("BE", Pattern.compile("[01]\\d{9}")),
            Map.entry("BG", Pattern.compile("\\d{9,10}")),
            Map.entry("CY", Pattern.compile("\\d{8}[A-Z]")),
            Map.entry("CZ", Pattern.compile("\\d{8,10}")),
            Map.entry("DK", Pattern.compile("\\d{8}")),
            Map.entry("EE", Pattern.compile("\\d{9}")),
            Map.entry("EL", Pattern.compile("\\d{9}")),
            Map.entry("ES", Pattern.compile("[A-Z0-9]\\d{7}[A-Z0-9]")),
            Map.entry("FI", Pattern.compile("\\d{8}")),
            Map.entry("FR", Pattern.compile("[A-Z0-9]{2}\\d{9}")),
            Map.entry("HR", Pattern.compile("\\d{11}")),
            Map.entry("HU", Pattern.compile("\\d{8}")),
            Map.entry("IE", Pattern.compile("\\d[A-Z0-9+*]\\d{5}[A-Z]{1,2}")),
            Map.entry("IT", Pattern.compile("\\d{11}")),
            Map.entry("LT", Pattern.compile("\\d{9}|\\d{12}")),
            Map.entry("LU", Pattern.compile("\\d{8}")),
            Map.entry("LV", Pattern.compile("\\d{11}")),
            Map.entry("MT", Pattern.compile("\\d{8}")),
            Map.entry("NL", Pattern.compile("\\d{9}B\\d{2}")),
            Map.entry("PL", Pattern.compile("\\d{10}")),
            Map.entry("PT", Pattern.compile("\\d{9}")),
            Map.entry("RO", Pattern.compile("\\d{2,10}")),
            Map.entry("SE", Pattern.compile("\\d{12}")),
            Map.entry("SI", Pattern.compile("\\d{8}")),
            Map.entry("SK", Pattern.compile("\\d{10}")),
            Map.entry("XI", Pattern.compile("\\d{9}|\\d{12}|GD\\d{3}|HA\\d{3}")));

    /** Erlaubte Telefonzeichen; die Mindestlänge wird über die Ziffernzahl geprüft (C-F-18). */
    private static final Pattern TELEFON_ZEICHEN = Pattern.compile("[0-9+ ()/\\-]+");

    /** IBAN: Ländercode, zwei Prüfziffern, 10–30 alphanumerische Stellen. */
    private static final Pattern IBAN = Pattern.compile("[A-Z]{2}\\d{2}[A-Z0-9]{10,30}");

    /** BIC: Institut (4), Land (2), Ort (2), optional Filiale (3). */
    private static final Pattern BIC = Pattern.compile("[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?");

    /** Steuernummer: Ziffern mit den üblichen Trennern, 10 bis 13 Ziffern (C-F-20). */
    private static final Pattern STEUERNUMMER_ZEICHEN = Pattern.compile("[0-9/ ]+");

    private static final int TELEFON_MIN_ZIFFERN = 6;
    private static final int STEUERNUMMER_MIN_ZIFFERN = 10;
    private static final int STEUERNUMMER_MAX_ZIFFERN = 13;
    private static final BigInteger IBAN_MODUL = BigInteger.valueOf(97);

    private Validierung() {
    }

    /**
     * Entfernt führende und folgende Leerzeichen; ein leerer Wert wird zu
     * {@code null}, damit "nicht angegeben" nur eine Darstellung hat (C-F-19).
     */
    public static String bereinige(String wert) {
        if (wert == null) {
            return null;
        }
        String gestutzt = wert.strip();
        return gestutzt.isEmpty() ? null : gestutzt;
    }

    /** USt-IdNr. ohne Leerzeichen in Großschrift, z. B. {@code "de 123 456 789"} → {@code "DE123456789"}. */
    public static String normalisiereUstIdNr(String ustIdNr) {
        String bereinigt = bereinige(ustIdNr);
        return bereinigt == null ? null : bereinigt.replace(" ", "").toUpperCase(Locale.ROOT);
    }

    /** IBAN in Vierergruppen, Großschrift, z. B. {@code "DE02 1203 0000 0000 2020 51"}. */
    public static String normalisiereIban(String iban) {
        String kompakt = kompakt(iban);
        if (kompakt == null) {
            return null;
        }
        StringBuilder gruppiert = new StringBuilder();
        for (int i = 0; i < kompakt.length(); i += 4) {
            if (i > 0) {
                gruppiert.append(' ');
            }
            gruppiert.append(kompakt, i, Math.min(i + 4, kompakt.length()));
        }
        return gruppiert.toString();
    }

    /** BIC ohne Leerzeichen in Großschrift. */
    public static String normalisiereBic(String bic) {
        return kompakt(bic);
    }

    /** Pflichtfelder dürfen weder {@code null} noch leer sein (C-F-03, B-F-04, A-F-18). */
    public static void pruefePflichtfeld(String wert, String feldname) {
        if (wert == null || wert.isBlank()) {
            throw new ValidierungsException(feldname,
                    "Das Pflichtfeld '" + feldname + "' fehlt.");
        }
    }

    public static void pruefePlz(String plz) {
        if (plz != null && !plz.isBlank() && !PLZ.matcher(plz.strip()).matches()) {
            throw new ValidierungsException("PLZ",
                    "Das Feld 'PLZ' muss aus genau 5 Ziffern bestehen (z. B. 68163): " + plz);
        }
    }

    public static void pruefeEMail(String eMail) {
        if (eMail != null && !eMail.isBlank() && !E_MAIL.matcher(eMail.strip()).matches()) {
            throw new ValidierungsException("E-Mail",
                    "Das Feld 'E-Mail' hat ein ungültiges Format (erwartet name@domain.de): " + eMail);
        }
    }

    /**
     * USt-IdNr. eines EU-Mitgliedstaats (C-F-17); Leerzeichen und
     * Kleinschreibung sind erlaubt und werden vor der Prüfung entfernt.
     */
    public static void pruefeUstIdNr(String ustIdNr) {
        String normalisiert = normalisiereUstIdNr(ustIdNr);
        if (normalisiert == null) {
            return;
        }
        Pattern format = normalisiert.length() > 2
                ? UST_ID_NR.get(normalisiert.substring(0, 2)) : null;
        if (format == null || !format.matcher(normalisiert.substring(2)).matches()) {
            throw new ValidierungsException("USt-IdNr.",
                    "Das Feld 'USt-IdNr.' hat kein gültiges Format (Länderkennung + Nummer,"
                            + " z. B. DE123456789): " + ustIdNr);
        }
    }

    /**
     * IBAN-Prüfung: Format (Ländercode, 2 Prüfziffern, 10–30 Stellen) und
     * Prüfsumme nach ISO 13616 (Modulo 97) — ein Tippfehler in der
     * Bankverbindung soll nicht erst beim Kunden auffallen (C-F-20).
     */
    public static void pruefeIban(String iban) {
        String kompakt = kompakt(iban);
        if (kompakt == null) {
            return;
        }
        if (!IBAN.matcher(kompakt).matches()) {
            throw new ValidierungsException("IBAN",
                    "Das Feld 'IBAN' hat kein gültiges Format (z. B. DE02 1203 0000 0000 2020 51): "
                            + iban);
        }
        if (!hatGueltigePruefsumme(kompakt)) {
            throw new ValidierungsException("IBAN",
                    "Die Prüfziffern der 'IBAN' stimmen nicht — bitte auf Tippfehler prüfen: " + iban);
        }
    }

    /** BIC: 8 oder 11 Zeichen (C-F-20). */
    public static void pruefeBic(String bic) {
        String kompakt = kompakt(bic);
        if (kompakt != null && !BIC.matcher(kompakt).matches()) {
            throw new ValidierungsException("BIC",
                    "Das Feld 'BIC' muss 8 oder 11 Zeichen haben (z. B. BYLADEM1001): " + bic);
        }
    }

    /**
     * Steuernummer des Finanzamts (z. B. {@code 37/123/45678}): Ziffern mit
     * Schrägstrich oder Leerzeichen, 10 bis 13 Ziffern (C-F-20).
     */
    public static void pruefeSteuernummer(String steuernummer) {
        String bereinigt = bereinige(steuernummer);
        if (bereinigt == null) {
            return;
        }
        long ziffern = bereinigt.chars().filter(Character::isDigit).count();
        if (!STEUERNUMMER_ZEICHEN.matcher(bereinigt).matches()
                || ziffern < STEUERNUMMER_MIN_ZIFFERN || ziffern > STEUERNUMMER_MAX_ZIFFERN) {
            throw new ValidierungsException("Steuernummer",
                    "Das Feld 'Steuernummer' muss 10 bis 13 Ziffern enthalten"
                            + " (z. B. 37/123/45678): " + steuernummer);
        }
    }

    public static void pruefeTelefon(String telefon) {
        if (telefon == null || telefon.isBlank()) {
            return;
        }
        String gestutzt = telefon.strip();
        long ziffern = gestutzt.chars().filter(Character::isDigit).count();
        if (!TELEFON_ZEICHEN.matcher(gestutzt).matches() || ziffern < TELEFON_MIN_ZIFFERN) {
            throw new ValidierungsException("Telefon",
                    "Das Feld 'Telefon' darf nur Ziffern, Leerzeichen und + ( ) / - enthalten"
                            + " und muss mindestens " + TELEFON_MIN_ZIFFERN + " Ziffern haben: " + telefon);
        }
    }

    private static String kompakt(String wert) {
        String bereinigt = bereinige(wert);
        return bereinigt == null ? null : bereinigt.replace(" ", "").toUpperCase(Locale.ROOT);
    }

    /** ISO 13616: Länderkennung und Prüfziffern ans Ende, Buchstaben als Zahlen, Rest 1. */
    private static boolean hatGueltigePruefsumme(String iban) {
        String umgestellt = iban.substring(4) + iban.substring(0, 4);
        StringBuilder ziffern = new StringBuilder();
        for (char zeichen : umgestellt.toCharArray()) {
            ziffern.append(Character.getNumericValue(zeichen));
        }
        return new BigInteger(ziffern.toString()).mod(IBAN_MODUL).intValue() == 1;
    }
}
