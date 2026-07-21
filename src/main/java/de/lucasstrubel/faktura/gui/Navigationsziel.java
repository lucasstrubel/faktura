package de.lucasstrubel.faktura.gui;

import org.kordamp.ikonli.feather.Feather;

/**
 * Die Bereiche der Seitennavigation (D-F-01) mit Beschriftung, Symbol und der
 * zugehörigen FXML-Ansicht. Die Reihenfolge der Konstanten ist zugleich die
 * Reihenfolge in der Navigation und bestimmt die Tastenkürzel
 * {@code Strg+1} bis {@code Strg+5}.
 */
public enum Navigationsziel {

    UEBERSICHT("Übersicht", "Zahlen und jüngste Belege auf einen Blick",
            Feather.HOME, "uebersicht_ansicht"),
    KUNDEN("Kunden", "Kundenstammdaten verwalten",
            Feather.USERS, "kunden_ansicht"),
    PRODUKTE("Produkte", "Produkte und Leistungen verwalten",
            Feather.PACKAGE, "produkt_ansicht"),
    BELEGE("Belege", "Angebote, Auftragsbestätigungen, Lieferscheine und Rechnungen",
            Feather.FILE_TEXT, "dokument_ansicht"),
    EINSTELLUNGEN("Einstellungen", "Firmenprofil und Datensicherung",
            Feather.SETTINGS, "einstellungen_ansicht");

    private final String beschriftung;
    private final String beschreibung;
    private final Feather symbol;
    private final String ansicht;

    Navigationsziel(String beschriftung, String beschreibung, Feather symbol, String ansicht) {
        this.beschriftung = beschriftung;
        this.beschreibung = beschreibung;
        this.symbol = symbol;
        this.ansicht = ansicht;
    }

    public String beschriftung() {
        return beschriftung;
    }

    /** Erläuternder Untertitel in der Kopfzeile der Ansicht. */
    public String beschreibung() {
        return beschreibung;
    }

    public Feather symbol() {
        return symbol;
    }

    /** Name der FXML-Datei ohne Endung, wie ihn {@link FxmlLader} erwartet. */
    public String ansicht() {
        return ansicht;
    }
}
