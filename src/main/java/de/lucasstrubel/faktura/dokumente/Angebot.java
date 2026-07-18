package de.lucasstrubel.faktura.dokumente;

import java.time.LocalDate;

/**
 * Angebot (BA-09, A-F-01 bis F-04): Beleg mit Gültigkeitsdatum.
 */
public class Angebot extends Dokument {

    private LocalDate gueltigBis;

    @Override
    public Belegtyp belegtyp() {
        return Belegtyp.ANGEBOT;
    }

    public LocalDate getGueltigBis() {
        return gueltigBis;
    }

    public void setGueltigBis(LocalDate gueltigBis) {
        pruefeAenderbar();
        this.gueltigBis = gueltigBis;
    }
}
