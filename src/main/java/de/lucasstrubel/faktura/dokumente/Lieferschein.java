package de.lucasstrubel.faktura.dokumente;

import java.time.LocalDate;

/**
 * Lieferschein (BA-11, A-F-08 bis F-10): Beleg mit Lieferdatum.
 */
public class Lieferschein extends Dokument {

    private LocalDate lieferdatum;

    @Override
    public Belegtyp belegtyp() {
        return Belegtyp.LIEFERSCHEIN;
    }

    public LocalDate getLieferdatum() {
        return lieferdatum;
    }

    public void setLieferdatum(LocalDate lieferdatum) {
        pruefeAenderbar();
        this.lieferdatum = lieferdatum;
    }
}
