package de.lucasstrubel.faktura.dokumente;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Zentrale Fachlogik des Dokumentenzyklus (Pflichtenheft Teil A,
 * Kapitel 7): Belegerzeugung, Summenberechnung, Nummernvergabe,
 * Verknüpfung, Statusführung, Stornierung und PDF-Export. Wird von der
 * Programmoberfläche (Komponente D) über diese Schnittstelle genutzt.
 */
public interface DokumentService {

    /** Erstellt ein Angebot (F-01, F-02); {@code gueltigBis = null} → Datum + 30 Tage. */
    Angebot erstelleAngebot(String kundenNr, List<Positionsangabe> positionen, LocalDate gueltigBis);

    /** Erstellt eine Auftragsbestätigung ohne Vorgängerbeleg (F-05, F-06). */
    Auftragsbestaetigung erstelleAuftragsbestaetigung(String kundenNr, List<Positionsangabe> positionen);

    /** Erstellt einen Lieferschein ohne Vorgängerbeleg (F-08, F-09). */
    Lieferschein erstelleLieferschein(String kundenNr, List<Positionsangabe> positionen, LocalDate lieferdatum);

    /**
     * Erstellt eine Rechnung (F-11 bis F-15); {@code leistungsdatum = null} →
     * Rechnungsdatum (A-F-31), {@code zahlungsziel = null} →
     * Standard-Zahlungsziel 14 Kalendertage ab Rechnungsdatum (GR-06).
     */
    Rechnung erstelleRechnung(String kundenNr, List<Positionsangabe> positionen,
                              LocalDate rechnungsdatum, LocalDate leistungsdatum,
                              LocalDate zahlungsziel);

    /** Erstellt eine Rechnung mit Leistungsdatum = Rechnungsdatum. */
    default Rechnung erstelleRechnung(String kundenNr, List<Positionsangabe> positionen,
                                      LocalDate rechnungsdatum, LocalDate zahlungsziel) {
        return erstelleRechnung(kundenNr, positionen, rechnungsdatum, null, zahlungsziel);
    }

    /**
     * Erzeugt den Folgebeleg im Dokumentenzyklus (GR-05, F-22):
     * Angebot → Auftragsbestätigung → Lieferschein → Rechnung. Kunde,
     * Positionen und Mengen werden übernommen, die Rückreferenz gespeichert.
     */
    Dokument erzeugeFolgebeleg(String belegnummer);

    /** Setzt den Belegstatus auf {@code VERSENDET}; danach gilt GR-02. */
    void versende(String belegnummer);

    /**
     * Storniert eine Rechnung (F-19, F-20, A-F-29). Eine offene Rechnung wird
     * in-place storniert; eine versendete zusätzlich durch eine neue
     * Stornorechnung mit negativen Mengen ausgeglichen (F-24).
     *
     * @return die erzeugte Stornorechnung, oder die stornierte Rechnung
     *         selbst, wenn keine Stornorechnung nötig war
     */
    Rechnung storniere(String rechnungsnummer);

    /** Erfasst den Zahlungseingang einer offenen oder versendeten Rechnung (A-F-28). */
    void markiereBezahlt(String rechnungsnummer, LocalDate bezahltAm);

    List<Dokument> alleDokumente();

    /** Alle Rechnungen im Status {@code OFFEN} (F-20). */
    List<Rechnung> offeneRechnungen();

    /** Berechnet die Summen für die Wizard-Zusammenfassung (D-F-12), ohne zu speichern. */
    Summen berechneSummen(List<Positionsangabe> positionen);

    /** Exportiert den Beleg als PDF in das lokale Dateisystem (F-04, F-07, F-10, F-15). */
    void exportierePdf(String belegnummer, Path zielDatei);
}
