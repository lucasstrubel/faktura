package de.lucasstrubel.faktura.dokumente;

/**
 * Die vier kaufmännischen Belegtypen mit ihren Nummern-Präfixen
 * (Komponente A, Kapitel 4: AN-, AB-, LS-, R-).
 */
public enum Belegtyp {
    ANGEBOT("AN", "Angebot"),
    AUFTRAGSBESTAETIGUNG("AB", "Auftragsbestätigung"),
    LIEFERSCHEIN("LS", "Lieferschein"),
    RECHNUNG("R", "Rechnung");

    private final String praefix;
    private final String anzeigename;

    Belegtyp(String praefix, String anzeigename) {
        this.praefix = praefix;
        this.anzeigename = anzeigename;
    }

    public String praefix() {
        return praefix;
    }

    public String anzeigename() {
        return anzeigename;
    }
}
