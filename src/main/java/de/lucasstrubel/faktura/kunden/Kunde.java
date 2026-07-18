package de.lucasstrubel.faktura.kunden;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Kundenstammdaten (Pflichtenheft Teil C, Kapitel 6.1).
 * Kundennummer und PLZ werden als {@code String} geführt (führende Nullen).
 * Die Kundennummer ist nach Vergabe unveränderlich (C-F-07).
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Kunde {

    private String kundennummer;
    private String name;
    private String strasse;
    private String plz;
    private String ort;
    private String eMail;
    private String telefon;
    private String ustIdNr;

    public Kunde() {
    }

    public Kunde(String name, String strasse, String plz, String ort) {
        this.name = name;
        this.strasse = strasse;
        this.plz = plz;
        this.ort = ort;
    }

    public String getKundennummer() {
        return kundennummer;
    }

    /** Einmalige Vergabe durch das System; jede spätere Änderung wird abgelehnt (C-F-07). */
    public void setKundennummer(String kundennummer) {
        if (this.kundennummer != null && !this.kundennummer.equals(kundennummer)) {
            throw new IllegalArgumentException(
                    "Die Kundennummer ist nach der Vergabe unveränderlich (C-F-07).");
        }
        this.kundennummer = kundennummer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStrasse() {
        return strasse;
    }

    public void setStrasse(String strasse) {
        this.strasse = strasse;
    }

    public String getPlz() {
        return plz;
    }

    public void setPlz(String plz) {
        this.plz = plz;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        this.ort = ort;
    }

    public String getEMail() {
        return eMail;
    }

    public void setEMail(String eMail) {
        this.eMail = eMail;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public String getUstIdNr() {
        return ustIdNr;
    }

    public void setUstIdNr(String ustIdNr) {
        this.ustIdNr = ustIdNr;
    }

    /** Anschrift einzeilig, z. B. für die Beleg-Übernahme durch Komponente A. */
    public String anschrift() {
        return strasse + ", " + plz + " " + ort;
    }

    @Override
    public String toString() {
        return (kundennummer != null ? kundennummer + " — " : "") + name;
    }
}
