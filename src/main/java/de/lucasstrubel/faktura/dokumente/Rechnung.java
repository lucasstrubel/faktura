package de.lucasstrubel.faktura.dokumente;

import java.time.LocalDate;

/**
 * Rechnung (BA-12 bis BA-14): führt die Pflichtangaben gemäß § 14 UStG
 * (F-13), das Zahlungsziel (GR-06) und die Stornierung (F-19 bis F-21).
 */
public class Rechnung extends Dokument {

    private LocalDate leistungsdatum;
    private LocalDate zahlungsziel;
    private LocalDate storniertAm;
    private String storniertVon;

    @Override
    public Belegtyp belegtyp() {
        return Belegtyp.RECHNUNG;
    }

    /**
     * Storniert eine offene Rechnung (BA-14, F-19, F-20): Status wird
     * {@code STORNIERT}, der Vorgang wird mit Datum und Benutzer protokolliert.
     */
    public void storniere(LocalDate datum, String benutzer) {
        if (getStatus() != DokumentStatus.OFFEN) {
            throw new IllegalStateException(
                    "Nur Rechnungen im Status OFFEN können storniert werden (F-19), "
                            + "aktueller Status: " + getStatus());
        }
        setzeStatus(DokumentStatus.STORNIERT);
        this.storniertAm = datum;
        this.storniertVon = benutzer;
    }

    /** Storniert mit Datum, ohne Benutzerangabe (Rückwärtskompatibilität). */
    public void storniere(LocalDate datum) {
        storniere(datum, null);
    }

    public void storniere() {
        storniere(LocalDate.now(), null);
    }

    public LocalDate getLeistungsdatum() {
        return leistungsdatum;
    }

    public void setLeistungsdatum(LocalDate leistungsdatum) {
        pruefeAenderbar();
        this.leistungsdatum = leistungsdatum;
    }

    public LocalDate getZahlungsziel() {
        return zahlungsziel;
    }

    public void setZahlungsziel(LocalDate zahlungsziel) {
        pruefeAenderbar();
        this.zahlungsziel = zahlungsziel;
    }

    public LocalDate getStorniertAm() {
        return storniertAm;
    }

    public String getStorniertVon() {
        return storniertVon;
    }
}
