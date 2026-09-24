package de.lucasstrubel.faktura.dokumente;

import java.time.LocalDate;

/**
 * Rechnung (BA-12 bis BA-14): führt die Pflichtangaben gemäß § 14 UStG
 * (F-13), das Zahlungsziel (GR-06), die Stornierung (F-19 bis F-21,
 * A-F-29) und den Zahlungseingang (A-F-28).
 *
 * <p>Eine Stornorechnung ist eine Rechnung mit negativen Mengen, die über
 * {@link #getStornoZu()} auf die stornierte Rechnung verweist. Sie erhält
 * eine eigene Nummer aus dem Rechnungskreis und wird weder bezahlt noch
 * selbst storniert.
 */
public class Rechnung extends Dokument {

    private LocalDate leistungsdatum;
    private LocalDate zahlungsziel;
    private LocalDate storniertAm;
    private String storniertVon;
    private LocalDate bezahltAm;
    private String stornoZu;

    @Override
    public Belegtyp belegtyp() {
        return Belegtyp.RECHNUNG;
    }

    /**
     * Storniert die Rechnung (BA-14, F-19, F-20, A-F-29): Status wird
     * {@code STORNIERT}, der Vorgang wird mit Datum und Benutzer protokolliert.
     * Zulässig für offene und versendete Rechnungen; bei einer versendeten
     * Rechnung erzeugt die Fachlogik zusätzlich die Stornorechnung. Der Inhalt
     * der Rechnung bleibt unberührt — nur der Status wechselt (GR-02).
     */
    public void storniere(LocalDate datum, String benutzer) {
        if (getStatus() != DokumentStatus.OFFEN && getStatus() != DokumentStatus.VERSENDET) {
            throw new IllegalStateException(
                    "Nur offene oder versendete Rechnungen können storniert werden (F-19), "
                            + "aktueller Status: " + getStatus());
        }
        if (istStornorechnung()) {
            throw new IllegalStateException(
                    "Eine Stornorechnung kann nicht selbst storniert werden (A-F-29).");
        }
        if (istBezahlt()) {
            throw new IllegalStateException("Die Rechnung " + getBelegnummer()
                    + " ist bereits bezahlt und kann nicht storniert werden (A-F-28).");
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

    /**
     * Erfasst den Zahlungseingang (A-F-28). Die Zahlung ändert den Inhalt
     * der Rechnung nicht und ist deshalb auch nach dem Versand zulässig; sie
     * wird genau einmal erfasst.
     */
    public void markiereBezahlt(LocalDate datum) {
        if (datum == null) {
            throw new IllegalArgumentException("Das Zahlungsdatum fehlt.");
        }
        if (getStatus() != DokumentStatus.OFFEN && getStatus() != DokumentStatus.VERSENDET) {
            throw new IllegalStateException("Nur offene oder versendete Rechnungen können als "
                    + "bezahlt markiert werden, aktueller Status: " + getStatus());
        }
        if (istStornorechnung()) {
            throw new IllegalStateException("Eine Stornorechnung wird nicht bezahlt (A-F-29).");
        }
        if (istBezahlt()) {
            throw new IllegalStateException("Die Rechnung " + getBelegnummer()
                    + " ist bereits als bezahlt erfasst (" + bezahltAm + ").");
        }
        this.bezahltAm = datum;
    }

    public boolean istBezahlt() {
        return bezahltAm != null;
    }

    public boolean istStornorechnung() {
        return stornoZu != null;
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

    public LocalDate getBezahltAm() {
        return bezahltAm;
    }

    /** Belegnummer der durch diese Stornorechnung stornierten Rechnung, sonst {@code null}. */
    public String getStornoZu() {
        return stornoZu;
    }

    public void setStornoZu(String stornoZu) {
        pruefeAenderbar();
        this.stornoZu = stornoZu;
    }
}
