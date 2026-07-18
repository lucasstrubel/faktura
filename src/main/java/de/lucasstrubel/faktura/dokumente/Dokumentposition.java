package de.lucasstrubel.faktura.dokumente;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Position eines Belegs (Komponente A, Kapitel 6.1). Bezeichnung, Einzelpreis
 * und Steuersatz sind ein unveränderlicher <b>Snapshot</b> des Produkts zum
 * Erstellzeitpunkt (GR-03, F-23). Beträge: {@code BigDecimal}, Scale 2,
 * kaufmännische Rundung.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Dokumentposition {

    private String produktReferenz;
    private String bezeichnung;
    private int menge;
    private BigDecimal einzelpreisNetto;
    private BigDecimal steuersatz;
    private BigDecimal positionssummeNetto;

    public Dokumentposition() {
    }

    public Dokumentposition(String produktReferenz, String bezeichnung, int menge,
                            BigDecimal einzelpreisNetto, BigDecimal steuersatz) {
        this.produktReferenz = produktReferenz;
        this.bezeichnung = bezeichnung;
        this.menge = menge;
        this.einzelpreisNetto = einzelpreisNetto.setScale(2, RoundingMode.HALF_UP);
        this.steuersatz = steuersatz;
        this.positionssummeNetto = berechnePositionssummeNetto();
    }

    /** {@code einzelpreisNetto * menge}, Scale 2 (F-23, TC-02). */
    private BigDecimal berechnePositionssummeNetto() {
        return einzelpreisNetto.multiply(BigDecimal.valueOf(menge))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getPositionssummeNetto() {
        return positionssummeNetto;
    }

    /** Steuerbetrag der Position: {@code positionssummeNetto * steuersatz}, Scale 2 (F-23, TC-01). */
    public BigDecimal getSteuerbetrag() {
        // Altdaten ohne Preis-Snapshot (IF-01) liefern 0 statt eines Fehlers
        if (positionssummeNetto == null || steuersatz == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return positionssummeNetto.multiply(steuersatz).setScale(2, RoundingMode.HALF_UP);
    }

    /** Bruttobetrag der Position: Netto + Steuer (TC-01). */
    public BigDecimal getPositionssummeBrutto() {
        return positionssummeNetto.add(getSteuerbetrag());
    }

    public String getProduktReferenz() {
        return produktReferenz;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public int getMenge() {
        return menge;
    }

    public BigDecimal getEinzelpreisNetto() {
        return einzelpreisNetto;
    }

    public BigDecimal getSteuersatz() {
        return steuersatz;
    }
}
