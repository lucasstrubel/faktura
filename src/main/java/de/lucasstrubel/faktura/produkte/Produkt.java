package de.lucasstrubel.faktura.produkte;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.math.BigDecimal;

/**
 * Produktstammdaten (Pflichtenheft Teil B, Kapitel 6.1).
 * Geldbeträge als {@code BigDecimal} (Scale 2), Steuersatz als Faktor
 * (zulässig: 0.00, 0.07, 0.19). Die Produktnummer ist nach Vergabe
 * unveränderlich (B-F-07).
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Produkt {

    private String produktnummer;
    private String bezeichnung;
    private String beschreibung;
    private BigDecimal einzelpreisNetto;
    private BigDecimal steuersatz;
    private String einheit;

    public Produkt() {
    }

    public Produkt(String bezeichnung, BigDecimal einzelpreisNetto, BigDecimal steuersatz) {
        this.bezeichnung = bezeichnung;
        this.einzelpreisNetto = einzelpreisNetto;
        this.steuersatz = steuersatz;
    }

    public String getProduktnummer() {
        return produktnummer;
    }

    /** Einmalige Vergabe durch das System; jede spätere Änderung wird abgelehnt (B-F-07). */
    public void setProduktnummer(String produktnummer) {
        if (this.produktnummer != null && !this.produktnummer.equals(produktnummer)) {
            throw new IllegalArgumentException(
                    "Die Produktnummer ist nach der Vergabe unveränderlich (B-F-07).");
        }
        this.produktnummer = produktnummer;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public void setBezeichnung(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public BigDecimal getEinzelpreisNetto() {
        return einzelpreisNetto;
    }

    public void setEinzelpreisNetto(BigDecimal einzelpreisNetto) {
        this.einzelpreisNetto = einzelpreisNetto;
    }

    public BigDecimal getSteuersatz() {
        return steuersatz;
    }

    public void setSteuersatz(BigDecimal steuersatz) {
        this.steuersatz = steuersatz;
    }

    public String getEinheit() {
        return einheit;
    }

    public void setEinheit(String einheit) {
        this.einheit = einheit;
    }

    @Override
    public String toString() {
        return (produktnummer != null ? produktnummer + " — " : "") + bezeichnung;
    }
}
