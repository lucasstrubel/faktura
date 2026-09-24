package de.lucasstrubel.faktura.dokumente;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import de.lucasstrubel.faktura.firma.Firmenprofil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Abstrakte Oberklasse aller Belege (Komponente A, Kapitel 6.1).
 *
 * <p>Neben der Kundenreferenz werden Name und Anschrift des Kunden als
 * Snapshot zum Erstellzeitpunkt abgelegt, damit bereits erstellte Belege
 * von späteren Stammdatenänderungen unberührt bleiben (C-F-06, AC-C-03);
 * dasselbe gilt für die Ausstellerdaten aus dem Firmenprofil (A-F-27).
 *
 * <p>Belege im Status {@code VERSENDET} oder {@code STORNIERT} lehnen jede
 * inhaltliche Änderung mit {@link IllegalStateException} ab (F-24, GR-02).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "typ")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Angebot.class, name = "ANGEBOT"),
        @JsonSubTypes.Type(value = Auftragsbestaetigung.class, name = "AUFTRAGSBESTAETIGUNG"),
        @JsonSubTypes.Type(value = Lieferschein.class, name = "LIEFERSCHEIN"),
        @JsonSubTypes.Type(value = Rechnung.class, name = "RECHNUNG")
})
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class Dokument {

    private String belegnummer;
    private LocalDate datum;
    private String kundenReferenz;
    private String kundeName;
    private String kundeAnschrift;
    private Firmenprofil aussteller;
    private List<Dokumentposition> positionen = new ArrayList<>();
    private DokumentStatus status = DokumentStatus.ENTWURF;
    private String vorgaengerNr;
    private BigDecimal summeNetto = BigDecimal.ZERO.setScale(2);
    private BigDecimal summeSteuer = BigDecimal.ZERO.setScale(2);
    private BigDecimal summeBrutto = BigDecimal.ZERO.setScale(2);

    public abstract Belegtyp belegtyp();

    /** Lehnt inhaltliche Änderungen versendeter/stornierter Belege ab (F-24, F-21, GR-02). */
    public void pruefeAenderbar() {
        if (status == DokumentStatus.VERSENDET || status == DokumentStatus.STORNIERT) {
            throw new IllegalStateException(
                    "Der Beleg " + belegnummer + " ist im Status " + status
                            + " und darf inhaltlich nicht mehr geändert werden (GR-02).");
        }
    }

    /** Ersetzt die Positionen und berechnet die Summen neu (F-23). */
    public void setzePositionen(List<Dokumentposition> neuePositionen) {
        pruefeAenderbar();
        this.positionen = new ArrayList<>(neuePositionen);
        berechneSummen();
    }

    /**
     * Netto-, Steuer- und Bruttosumme aus den Positionen, Scale 2 (F-03, F-23,
     * TC-03). Die Umsatzsteuer wird je Steuersatz einmal auf die Nettosumme
     * gerundet (A-F-25) und nicht je Position: Sonst wiche die Summe bei
     * vielen Kleinbeträgen um Cent von der E-Rechnung ab, die nach EN 16931
     * (BR-CO-17) je Satz rechnet — PDF und XML derselben Rechnung müssen
     * denselben Betrag ausweisen.
     */
    public void berechneSummen() {
        BigDecimal netto = BigDecimal.ZERO;
        BigDecimal steuer = BigDecimal.ZERO;
        for (Steuerzeile zeile : steueraufschluesselung()) {
            netto = netto.add(zeile.netto());
            steuer = steuer.add(zeile.steuer());
        }
        this.summeNetto = netto.setScale(2, RoundingMode.HALF_UP);
        this.summeSteuer = steuer.setScale(2, RoundingMode.HALF_UP);
        this.summeBrutto = this.summeNetto.add(this.summeSteuer);
    }

    /**
     * Netto- und Steuerbetrag je Steuersatz, aufsteigend nach Satz (A-F-25,
     * § 14 Abs. 4 Nr. 7/8 UStG). Altdaten ohne Preis- oder Steuer-Snapshot
     * (IF-01) zählen als 0 statt zu scheitern.
     */
    public List<Steuerzeile> steueraufschluesselung() {
        Map<BigDecimal, BigDecimal> nettoJeSatz = new TreeMap<>();
        for (Dokumentposition position : positionen) {
            BigDecimal satz = position.getSteuersatz() == null
                    ? BigDecimal.ZERO : position.getSteuersatz().stripTrailingZeros();
            BigDecimal netto = position.getPositionssummeNetto() == null
                    ? BigDecimal.ZERO : position.getPositionssummeNetto();
            nettoJeSatz.merge(satz, netto, BigDecimal::add);
        }
        List<Steuerzeile> zeilen = new ArrayList<>();
        nettoJeSatz.forEach((satz, netto) -> zeilen.add(new Steuerzeile(satz,
                netto.setScale(2, RoundingMode.HALF_UP),
                netto.multiply(satz).setScale(2, RoundingMode.HALF_UP))));
        return zeilen;
    }

    /**
     * Stellt beim Laden die gespeicherten Summen eines versendeten oder
     * stornierten Belegs wieder her, statt sie neu zu berechnen: Ein
     * ausgestellter Betrag darf sich durch eine spätere Änderung der
     * Rechenregel nicht verschieben (GR-02). Nur für die Persistenz.
     */
    void stelleSummenWiederHer(BigDecimal netto, BigDecimal steuer, BigDecimal brutto) {
        if (netto != null && steuer != null && brutto != null) {
            this.summeNetto = netto;
            this.summeSteuer = steuer;
            this.summeBrutto = brutto;
        }
    }

    /** Statuswechsel auf {@code VERSENDET}; danach greift die Unveränderlichkeit (GR-02). */
    public void versende() {
        if (status == DokumentStatus.VERSENDET) {
            return;
        }
        if (status == DokumentStatus.STORNIERT) {
            throw new IllegalStateException(
                    "Ein stornierter Beleg kann nicht versendet werden.");
        }
        status = DokumentStatus.VERSENDET;
    }

    public String getBelegnummer() {
        return belegnummer;
    }

    /** Einmalige Vergabe durch das System (Kapitel 4, Belegnummern-Regel). */
    public void setBelegnummer(String belegnummer) {
        if (this.belegnummer != null && !this.belegnummer.equals(belegnummer)) {
            throw new IllegalArgumentException(
                    "Die Belegnummer ist nach der Vergabe unveränderlich.");
        }
        this.belegnummer = belegnummer;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public void setDatum(LocalDate datum) {
        pruefeAenderbar();
        this.datum = datum;
    }

    public String getKundenReferenz() {
        return kundenReferenz;
    }

    public String getKundeName() {
        return kundeName;
    }

    public String getKundeAnschrift() {
        return kundeAnschrift;
    }

    /** Übernimmt die Kundendaten als Snapshot zum Erstellzeitpunkt (GR-05, AC-C-03). */
    public void setzeKunde(String kundenReferenz, String kundeName, String kundeAnschrift) {
        pruefeAenderbar();
        this.kundenReferenz = kundenReferenz;
        this.kundeName = kundeName;
        this.kundeAnschrift = kundeAnschrift;
    }

    /** Ausstellerdaten zum Erstellzeitpunkt; {@code null} bei Belegen vor v3.0 (A-F-27). */
    public Firmenprofil getAussteller() {
        return aussteller;
    }

    /** Übernimmt das Firmenprofil als Snapshot zum Erstellzeitpunkt (A-F-27). */
    public void setzeAussteller(Firmenprofil aussteller) {
        pruefeAenderbar();
        this.aussteller = aussteller;
    }

    public List<Dokumentposition> getPositionen() {
        return Collections.unmodifiableList(positionen);
    }

    public DokumentStatus getStatus() {
        return status;
    }

    protected void setzeStatus(DokumentStatus status) {
        this.status = status;
    }

    public String getVorgaengerNr() {
        return vorgaengerNr;
    }

    /** Rückreferenz auf den Vorgängerbeleg im Dokumentenzyklus (GR-05, F-22). */
    public void setVorgaengerNr(String vorgaengerNr) {
        pruefeAenderbar();
        this.vorgaengerNr = vorgaengerNr;
    }

    public BigDecimal getSummeNetto() {
        return summeNetto;
    }

    public BigDecimal getSummeSteuer() {
        return summeSteuer;
    }

    public BigDecimal getSummeBrutto() {
        return summeBrutto;
    }
}
