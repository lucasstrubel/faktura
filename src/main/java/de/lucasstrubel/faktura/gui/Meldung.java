package de.lucasstrubel.faktura.gui;

/**
 * Einheitliche Fehler- und Erfolgsmeldung der Oberfläche (D-F-16, F-17).
 *
 * @param typ      Erfolg oder Fehler
 * @param feldname betroffenes Eingabefeld bei Validierungsfehlern, sonst {@code null}
 * @param text     anzuzeigender Meldungstext
 */
public record Meldung(MeldungsTyp typ, String feldname, String text) {

    /** Erfolgsmeldung ohne Feldbezug (D-F-17). */
    public static Meldung erfolg(String text) {
        return new Meldung(MeldungsTyp.ERFOLG, null, text);
    }

    /** Fehlermeldung; {@code feldname} benennt das betroffene Eingabefeld (Q-09). */
    public static Meldung fehler(String feldname, String text) {
        return new Meldung(MeldungsTyp.FEHLER, feldname, text);
    }
}
