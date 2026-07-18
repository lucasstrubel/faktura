package de.lucasstrubel.faktura.dokumente;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Zentrale Fachlogik des Dokumentenzyklus (Pflichtenheft Gruppe A,
 * Kapitel 7): Belegerzeugung, Summenberechnung, Nummernvergabe,
 * Verknüpfung, Statusführung, Stornierung und PDF-Export. Wird von der
 * Programmoberfläche (Gruppe D) über diese Schnittstelle genutzt.
 */
public interface DokumentService {

    /** Erstellt ein Angebot (F-01, F-02); {@code gueltigBis = null} → Datum + 30 Tage. */
    Angebot erstelleAngebot(String kundenNr, List<Positionsangabe> positionen, LocalDate gueltigBis);

    /** Erstellt eine Auftragsbestätigung ohne Vorgängerbeleg (F-05, F-06). */
    Auftragsbestaetigung erstelleAuftragsbestaetigung(String kundenNr, List<Positionsangabe> positionen);

    /** Erstellt einen Lieferschein ohne Vorgängerbeleg (F-08, F-09). */
    Lieferschein erstelleLieferschein(String kundenNr, List<Positionsangabe> positionen, LocalDate lieferdatum);

    /**
     * Erstellt eine Rechnung (F-11 bis F-15); {@code zahlungsziel = null} →
     * Standard-Zahlungsziel 14 Kalendertage ab Rechnungsdatum (GR-06).
     */
    Rechnung erstelleRechnung(String kundenNr, List<Positionsangabe> positionen,
                              LocalDate rechnungsdatum, LocalDate zahlungsziel);

    /**
     * Erzeugt den Folgebeleg im Dokumentenzyklus (GR-05, F-22):
     * Angebot → Auftragsbestätigung → Lieferschein → Rechnung. Kunde,
     * Positionen und Mengen werden übernommen, die Rückreferenz gespeichert.
     */
    Dokument erzeugeFolgebeleg(String belegnummer);

    /** Setzt den Belegstatus auf {@code VERSENDET}; danach gilt GR-02. */
    void versende(String belegnummer);

    /** Storniert eine offene Rechnung (F-19, F-20). */
    void storniere(String rechnungsnummer);

    List<Dokument> alleDokumente();

    /** Alle Rechnungen im Status {@code OFFEN} (F-20). */
    List<Rechnung> offeneRechnungen();

    /** Berechnet die Summen für die Wizard-Zusammenfassung (D-F-12), ohne zu speichern. */
    Summen berechneSummen(List<Positionsangabe> positionen);

    /** Exportiert den Beleg als PDF in das lokale Dateisystem (F-04, F-07, F-10, F-15). */
    void exportierePdf(String belegnummer, Path zielDatei);
}
