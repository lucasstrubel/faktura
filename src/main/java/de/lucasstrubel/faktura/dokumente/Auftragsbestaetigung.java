package de.lucasstrubel.faktura.dokumente;

/**
 * Auftragsbestätigung (BA-10, A-F-05 bis F-07): nutzt die Rückreferenz
 * {@code vorgaengerNr} auf das zugrunde liegende Angebot (GR-05).
 */
public class Auftragsbestaetigung extends Dokument {

    @Override
    public Belegtyp belegtyp() {
        return Belegtyp.AUFTRAGSBESTAETIGUNG;
    }
}
