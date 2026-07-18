package de.lucasstrubel.faktura.gui;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * UI-Zustandsmodell der geführten Rechnungserstellung (Gruppe D,
 * Kapitel 6.1). Frei von GUI-Framework-Klassen und damit ohne
 * Oberfläche testbar.
 */
public class RechnungsWizardModel {

    private WizardSchritt aktuellerSchritt = WizardSchritt.KUNDE_WAEHLEN;
    private String kundenNr;
    private final List<PositionsEingabe> positionen = new ArrayList<>();
    private LocalDate rechnungsdatum = LocalDate.now();
    private LocalDate zahlungsziel;

    public WizardSchritt getAktuellerSchritt() {
        return aktuellerSchritt;
    }

    void setAktuellerSchritt(WizardSchritt schritt) {
        this.aktuellerSchritt = schritt;
    }

    public String getKundenNr() {
        return kundenNr;
    }

    public void setKundenNr(String kundenNr) {
        this.kundenNr = kundenNr;
    }

    public List<PositionsEingabe> getPositionen() {
        return positionen;
    }

    public void fuegePositionHinzu(PositionsEingabe position) {
        positionen.add(position);
    }

    public void entfernePosition(int index) {
        positionen.remove(index);
    }

    public LocalDate getRechnungsdatum() {
        return rechnungsdatum;
    }

    public void setRechnungsdatum(LocalDate rechnungsdatum) {
        this.rechnungsdatum = rechnungsdatum;
    }

    /** {@code null} = Standard-Zahlungsziel der Gruppe A (GR-06: +14 Tage). */
    public LocalDate getZahlungsziel() {
        return zahlungsziel;
    }

    public void setZahlungsziel(LocalDate zahlungsziel) {
        this.zahlungsziel = zahlungsziel;
    }
}
