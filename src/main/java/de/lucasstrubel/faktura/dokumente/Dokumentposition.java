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
    private String einheit;
    private String einheitCode;

    public Dokumentposition() {
    }

    public Dokumentposition(String produktReferenz, String bezeichnung, int menge,
                            BigDecimal einzelpreisNetto, BigDecimal steuersatz) {
        this(produktReferenz, bezeichnung, menge, einzelpreisNetto, steuersatz, null, null);
    }

    public Dokumentposition(String produktReferenz, String bezeichnung, int menge,
                            BigDecimal einzelpreisNetto, BigDecimal steuersatz,
                            String einheit, String einheitCode) {
        this.produktReferenz = produktReferenz;
        this.bezeichnung = bezeichnung;
        this.menge = menge;
        this.einzelpreisNetto = einzelpreisNetto == null ? null : einzelpreisNetto.setScale(2, RoundingMode.HALF_UP);
        this.steuersatz = steuersatz;
        this.positionssummeNetto = berechnePositionssummeNetto();
        this.einheit = einheit;
        this.einheitCode = einheitCode;
    }

    /** {@code einzelpreisNetto * menge}, Scale 2 (F-23, TC-02). */
    private BigDecimal berechnePositionssummeNetto() {
        if (einzelpreisNetto == null) { return null; }
        return einzelpreisNetto.multiply(BigDecimal.valueOf(menge))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getPositionssummeNetto() {
        return positionssummeNetto;
    }

    /** Steuerbetrag der Position: {@code positionssummeNetto * steuersatz}, Scale 2 (F-23, TC-01). */
    public BigDecimal getSteuerbetrag() {
        // Unbekannte historische Werte dürfen nicht als bekannte Null exportiert werden.
        if (positionssummeNetto == null || steuersatz == null) {
            return null;
        }
        return positionssummeNetto.multiply(steuersatz).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Bruttobetrag der Position: Netto + Steuer (TC-01). Informativ je Zeile;
     * die Belegsumme rechnet die Steuer je Steuersatz (A-F-25).
     */
    public BigDecimal getPositionssummeBrutto() {
        BigDecimal steuer = getSteuerbetrag();
        return positionssummeNetto == null || steuer == null ? null : positionssummeNetto.add(steuer);
    }

    /** Dieselbe Position mit negierter Menge — Baustein der Stornorechnung (A-F-29). */
    Dokumentposition negiert() {
        return wiederhergestellt(produktReferenz, bezeichnung, -menge,
                einzelpreisNetto, steuersatz, einheit, einheitCode,
                positionssummeNetto == null ? null : positionssummeNetto.negate());
    }

    /** Persistierte Werte unverändert übernehmen; keine erneute Rundung beim Laden. */
    public static Dokumentposition wiederhergestellt(String produkt, String text, int menge,
            BigDecimal preis, BigDecimal steuer, String einheit, String code, BigDecimal netto) {
        Dokumentposition position = new Dokumentposition();
        position.produktReferenz = produkt;
        position.bezeichnung = text;
        position.menge = menge;
        position.einzelpreisNetto = preis;
        position.steuersatz = steuer;
        position.einheit = einheit;
        position.einheitCode = code;
        position.positionssummeNetto = netto;
        return position;
    }

    public String getEinheit() { return einheit; }

    public String getEinheitCode() { return einheitCode; }

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
