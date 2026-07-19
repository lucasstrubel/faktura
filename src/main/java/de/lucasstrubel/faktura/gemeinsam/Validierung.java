package de.lucasstrubel.faktura.gemeinsam;

import java.util.regex.Pattern;

/**
 * Zentrale Eingabevalidierung der Stammdaten (Q-09; C-F-03, C-F-04 sowie die
 * erweiterten Formatprüfungen C-F-16 bis C-F-18). Jede Prüfung benennt bei
 * Ablehnung das betroffene Feld über eine {@link ValidierungsException}.
 *
 * <p>Optionale Felder (E-Mail, Telefon, USt-IdNr.) gelten als gültig, wenn sie
 * leer sind; geprüft wird nur ein vorhandener Wert.
 */
public final class Validierung {

    /** Lokalteil@Domain mit mindestens einer Top-Level-Domain (C-F-04). */
    private static final Pattern E_MAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}");

    /** Deutsche Postleitzahl: genau 5 Ziffern, führende Nullen erlaubt (C-F-16). */
    private static final Pattern PLZ = Pattern.compile("\\d{5}");

    /** Deutsche USt-IdNr.: {@code DE} gefolgt von 9 Ziffern (C-F-17). */
    private static final Pattern UST_ID_NR = Pattern.compile("DE\\d{9}");

    /** Erlaubte Telefonzeichen; die Mindestlänge wird über die Ziffernzahl geprüft (C-F-18). */
    private static final Pattern TELEFON_ZEICHEN = Pattern.compile("[0-9+ ()/\\-]+");

    /** IBAN: Ländercode, zwei Prüfziffern, 10–30 alphanumerische Stellen. */
    private static final Pattern IBAN = Pattern.compile("[A-Z]{2}\\d{2}[A-Z0-9]{10,30}");

    private static final int TELEFON_MIN_ZIFFERN = 6;

    private Validierung() {
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

    /** Leerzeichen im Wert sind erlaubt und werden vor der Prüfung entfernt. */
    public static void pruefeUstIdNr(String ustIdNr) {
        if (ustIdNr == null || ustIdNr.isBlank()) {
            return;
        }
        String normalisiert = ustIdNr.replace(" ", "");
        if (!UST_ID_NR.matcher(normalisiert).matches()) {
            throw new ValidierungsException("USt-IdNr.",
                    "Das Feld 'USt-IdNr.' muss dem Format DE + 9 Ziffern entsprechen"
                            + " (z. B. DE123456789): " + ustIdNr);
        }
    }

    /** IBAN-Plausibilität: Ländercode, 2 Prüfziffern, 10–30 Stellen; Leerzeichen erlaubt. */
    public static void pruefeIban(String iban) {
        if (iban == null || iban.isBlank()) {
            return;
        }
        String normalisiert = iban.replace(" ", "").toUpperCase(java.util.Locale.ROOT);
        if (!IBAN.matcher(normalisiert).matches()) {
            throw new ValidierungsException("IBAN",
                    "Das Feld 'IBAN' hat kein gültiges Format (z. B. DE02 1203 0000 0000 2020 51): "
                            + iban);
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
}
