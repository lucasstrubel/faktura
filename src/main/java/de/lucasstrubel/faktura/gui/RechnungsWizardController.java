package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.dokumente.Positionsangabe;
import de.lucasstrubel.faktura.dokumente.Rechnung;
import de.lucasstrubel.faktura.dokumente.StandardDokumentService;
import de.lucasstrubel.faktura.dokumente.Summen;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialogführung der geführten Rechnungserstellung (D-F-09 bis F-13):
 * Schrittfolge, Vollständigkeitsprüfung je Schritt, Zusammenfassung und
 * genau ein Speicheraufruf an den {@link DokumentService} (Komponente A).
 * GUI-frei und damit im Modultest ohne Oberfläche prüfbar.
 */
public class RechnungsWizardController {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final List<WizardSchritt> REIHENFOLGE = List.of(WizardSchritt.values());

    private final DokumentService dokumentService;
    private final KundenService kundenService;
    private final ProduktService produktService;
    private final RechnungsWizardModel model = new RechnungsWizardModel();

    private Meldung letzteMeldung;
    private Rechnung gespeicherteRechnung;

    public RechnungsWizardController(DokumentService dokumentService,
                                     KundenService kundenService,
                                     ProduktService produktService) {
        this.dokumentService = dokumentService;
        this.kundenService = kundenService;
        this.produktService = produktService;
    }

    public RechnungsWizardModel getModel() {
        return model;
    }

    public Meldung getLetzteMeldung() {
        return letzteMeldung;
    }

    public Rechnung getGespeicherteRechnung() {
        return gespeicherteRechnung;
    }

    /**
     * Wechselt zum nächsten Schritt; bei unvollständiger Eingabe wird der
     * Wechsel verhindert und die fehlende Eingabe benannt (F-10, Q-09).
     */
    public boolean weiter() {
        Meldung fehler = pruefeAktuellenSchritt();
        if (fehler != null) {
            letzteMeldung = fehler;
            return false;
        }
        int index = REIHENFOLGE.indexOf(model.getAktuellerSchritt());
        if (index < REIHENFOLGE.size() - 1) {
            model.setAktuellerSchritt(REIHENFOLGE.get(index + 1));
        }
        letzteMeldung = null;
        return true;
    }

    /** Rückkehr zum vorherigen Schritt ohne Verlust der Eingaben (F-11). */
    public void zurueck() {
        int index = REIHENFOLGE.indexOf(model.getAktuellerSchritt());
        if (index > 0) {
            model.setAktuellerSchritt(REIHENFOLGE.get(index - 1));
        }
        letzteMeldung = null;
    }

    /** Vollständigkeitsprüfung des aktuellen Schritts (F-10). */
    private Meldung pruefeAktuellenSchritt() {
        return switch (model.getAktuellerSchritt()) {
            case KUNDE_WAEHLEN -> model.getKundenNr() == null
                    ? Meldung.fehler("Kunde", "Bitte zuerst einen Kunden auswählen.")
                    : null;
            case POSITIONEN_ERFASSEN -> pruefePositionen();
            case DATEN_BESTAETIGEN -> model.getRechnungsdatum() == null
                    ? Meldung.fehler("Rechnungsdatum", "Bitte ein Rechnungsdatum angeben.")
                    : null;
            case ZUSAMMENFASSUNG, SPEICHERN -> null;
        };
    }

    private Meldung pruefePositionen() {
        if (model.getPositionen().isEmpty()) {
            return Meldung.fehler("Position", "Bitte mindestens eine Position erfassen.");
        }
        for (PositionsEingabe position : model.getPositionen()) {
            if (position.menge() <= 0) {
                return Meldung.fehler("Menge", "Die Menge muss größer als 0 sein.");
            }
        }
        return null;
    }

    /**
     * Zusammenfassung für Schritt 4 (F-12): Kunde, Positionen, Mengen,
     * Summen (vom {@link DokumentService} berechnet), Rechnungsdatum und
     * Zahlungsziel.
     */
    public String erzeugeZusammenfassung() {
        StringBuilder text = new StringBuilder();
        Kunde kunde = kundenService.findeKunde(model.getKundenNr());
        text.append("Kunde: ")
                .append(kunde != null ? kunde.getName() + " (" + kunde.getKundennummer() + ")"
                        : model.getKundenNr())
                .append('\n');

        text.append("Positionen:\n");
        for (PositionsEingabe position : model.getPositionen()) {
            Produkt produkt = produktService.findeProdukt(position.produktnummer());
            String bezeichnung = produkt != null ? produkt.getBezeichnung() : position.produktnummer();
            text.append("  ").append(position.menge()).append(" x ")
                    .append(bezeichnung).append(" (").append(position.produktnummer()).append(")\n");
        }

        Summen summen = dokumentService.berechneSummen(positionsangaben());
        text.append("Summe netto: ").append(summen.netto().toPlainString()).append(" EUR\n");
        text.append("Umsatzsteuer: ").append(summen.steuer().toPlainString()).append(" EUR\n");
        text.append("Summe brutto: ").append(summen.brutto().toPlainString()).append(" EUR\n");

        text.append("Rechnungsdatum: ").append(DATUM.format(model.getRechnungsdatum())).append('\n');
        LocalDate zahlungsziel = model.getZahlungsziel() != null
                ? model.getZahlungsziel()
                : model.getRechnungsdatum().plusDays(StandardDokumentService.STANDARD_ZAHLUNGSZIEL_TAGE);
        text.append("Zahlungsziel: ").append(DATUM.format(zahlungsziel));
        if (model.getZahlungsziel() == null) {
            text.append(" (Standard: 14 Tage)");
        }
        return text.toString();
    }

    /**
     * Löst genau einen Speicheraufruf am {@link DokumentService} aus (F-13);
     * Validierungsfehler der Fachkomponente werden als Meldung mit dem
     * betroffenen Feld dargestellt (F-05, F-16).
     */
    public Meldung speichern() {
        try {
            gespeicherteRechnung = dokumentService.erstelleRechnung(
                    model.getKundenNr(), positionsangaben(),
                    model.getRechnungsdatum(), model.getZahlungsziel());
            letzteMeldung = Meldung.erfolg("Die Rechnung " + gespeicherteRechnung.getBelegnummer()
                    + " wurde gespeichert.");
        } catch (ValidierungsException e) {
            letzteMeldung = Meldung.fehler(e.getFeldname(), e.getMessage());
        }
        return letzteMeldung;
    }

    private List<Positionsangabe> positionsangaben() {
        return model.getPositionen().stream()
                .map(p -> new Positionsangabe(p.produktnummer(), p.menge()))
                .toList();
    }
}
