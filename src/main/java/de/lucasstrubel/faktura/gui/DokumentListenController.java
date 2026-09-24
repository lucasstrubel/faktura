package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.dokumente.DokumentStatus;
import de.lucasstrubel.faktura.dokumente.Rechnung;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Dialogführung der Dokumentliste (D-F-06 bis F-08, F-14, F-15):
 * Statusfilter, verfügbare Aktionen je Beleg und Stornierung nach
 * Bestätigung. GUI-frei und damit ohne Oberfläche testbar.
 */
public class DokumentListenController {

    /** Feste Locale: Die Suche soll unabhängig von der Systemeinstellung gleich wirken. */
    private static final Locale LOKAL = Locale.ROOT;

    private final DokumentService dokumentService;

    public DokumentListenController(DokumentService dokumentService) {
        this.dokumentService = dokumentService;
    }

    /** Dokumentliste, optional nach Status gefiltert (F-06); {@code null} = alle. */
    public List<Dokument> gefiltert(DokumentStatus statusFilter) {
        return gefiltert(statusFilter, null);
    }

    /**
     * Dokumentliste nach Status <em>und</em> Suchbegriff (F-06). Gesucht wird
     * ohne Rücksicht auf Groß-/Kleinschreibung in Belegnummer und Kundenname;
     * ein leerer Begriff filtert nicht.
     */
    public List<Dokument> gefiltert(DokumentStatus statusFilter, String suchbegriff) {
        String begriff = suchbegriff == null ? "" : suchbegriff.strip().toLowerCase(LOKAL);
        return dokumentService.alleDokumente().stream()
                .filter(d -> statusFilter == null || d.getStatus() == statusFilter)
                .filter(d -> begriff.isEmpty() || passt(d, begriff))
                .toList();
    }

    private static boolean passt(Dokument dokument, String begriff) {
        return enthaelt(dokument.getBelegnummer(), begriff)
                || enthaelt(dokument.getKundeName(), begriff)
                || enthaelt(dokument.getKundenReferenz(), begriff);
    }

    private static boolean enthaelt(String wert, String begriff) {
        return wert != null && wert.toLowerCase(LOKAL).contains(begriff);
    }

    /**
     * Verfügbare Aktionen je Beleg: <i>Stornieren</i> und <i>Als bezahlt
     * markieren</i> für offene oder versendete, noch unbezahlte Rechnungen,
     * die nicht selbst Stornorechnung sind (F-14, A-F-28, A-F-29);
     * inhaltliche Änderungen nur solange der Beleg nicht versendet/storniert
     * ist (F-08, GR-02); PDF-Export immer.
     */
    public BelegAktionen aktionenFuer(Dokument dokument) {
        boolean offeneForderung = dokument instanceof Rechnung rechnung
                && (rechnung.getStatus() == DokumentStatus.OFFEN
                    || rechnung.getStatus() == DokumentStatus.VERSENDET)
                && !rechnung.istBezahlt()
                && !rechnung.istStornorechnung();
        boolean aenderbar = dokument.getStatus() == DokumentStatus.ENTWURF
                || dokument.getStatus() == DokumentStatus.OFFEN;
        return new BelegAktionen(offeneForderung, aenderbar, true, offeneForderung);
    }

    /**
     * Storniert erst nach Bestätigung der Anwender:in (F-15); ohne
     * Bestätigung erfolgt kein Aufruf an die Fachkomponente. Bei einer
     * versendeten Rechnung nennt die Meldung die neu erzeugte Stornorechnung,
     * die dem Kunden zugestellt werden muss (A-F-29).
     */
    public Meldung storniere(String rechnungsnummer, boolean bestaetigt) {
        if (!bestaetigt) {
            return null;
        }
        try {
            Rechnung ergebnis = dokumentService.storniere(rechnungsnummer);
            String meldung = "Die Rechnung " + rechnungsnummer + " wurde storniert"
                    + protokoll(rechnungsnummer) + ".";
            if (ergebnis != null && ergebnis.istStornorechnung()) {
                meldung += " Die Stornorechnung " + ergebnis.getBelegnummer()
                        + " wurde erstellt — bitte dem Kunden zusenden.";
            }
            return Meldung.erfolg(meldung);
        } catch (ValidierungsException e) {
            return Meldung.fehler(e.getFeldname(), e.getMessage());
        } catch (IllegalStateException e) {
            return Meldung.fehler(null, e.getMessage());
        }
    }

    /** Erfasst den Zahlungseingang einer Rechnung (A-F-28). */
    public Meldung markiereBezahlt(String rechnungsnummer, LocalDate bezahltAm) {
        try {
            dokumentService.markiereBezahlt(rechnungsnummer, bezahltAm);
            return Meldung.erfolg("Der Zahlungseingang für " + rechnungsnummer + " wurde erfasst.");
        } catch (ValidierungsException e) {
            return Meldung.fehler(e.getFeldname(), e.getMessage());
        } catch (IllegalStateException e) {
            return Meldung.fehler(null, e.getMessage());
        }
    }

    /**
     * Liest das Storno-Protokoll (Datum, Benutzer) aus dem gespeicherten Beleg
     * für die Erfolgsmeldung (BA-14); leer, falls nicht ermittelbar.
     */
    private String protokoll(String rechnungsnummer) {
        return dokumentService.alleDokumente().stream()
                .filter(d -> d instanceof Rechnung && d.getBelegnummer().equals(rechnungsnummer))
                .map(d -> (Rechnung) d)
                .filter(r -> r.getStorniertAm() != null)
                .findFirst()
                .map(r -> " am " + r.getStorniertAm()
                        + (r.getStorniertVon() == null ? "" : " durch " + r.getStorniertVon()))
                .orElse("");
    }
}
