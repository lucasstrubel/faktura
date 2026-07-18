package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.dokumente.DokumentStatus;
import de.lucasstrubel.faktura.dokumente.Rechnung;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import java.util.List;

/**
 * Dialogführung der Dokumentliste (D-F-06 bis F-08, F-14, F-15):
 * Statusfilter, verfügbare Aktionen je Beleg und Stornierung nach
 * Bestätigung. GUI-frei und damit ohne Oberfläche testbar.
 */
public class DokumentListenController {

    private final DokumentService dokumentService;

    public DokumentListenController(DokumentService dokumentService) {
        this.dokumentService = dokumentService;
    }

    /** Dokumentliste, optional nach Status gefiltert (F-06); {@code null} = alle. */
    public List<Dokument> gefiltert(DokumentStatus statusFilter) {
        return dokumentService.alleDokumente().stream()
                .filter(d -> statusFilter == null || d.getStatus() == statusFilter)
                .toList();
    }

    /**
     * Verfügbare Aktionen je Beleg: <i>Stornieren</i> nur für Rechnungen im
     * Status {@code OFFEN} (F-14); inhaltliche Änderungen nur solange der
     * Beleg nicht versendet/storniert ist (F-08, GR-02); PDF-Export immer.
     */
    public BelegAktionen aktionenFuer(Dokument dokument) {
        boolean stornierbar = dokument instanceof Rechnung
                && dokument.getStatus() == DokumentStatus.OFFEN;
        boolean aenderbar = dokument.getStatus() == DokumentStatus.ENTWURF
                || dokument.getStatus() == DokumentStatus.OFFEN;
        return new BelegAktionen(stornierbar, aenderbar, true);
    }

    /**
     * Storniert erst nach Bestätigung der Anwender:in (F-15); ohne
     * Bestätigung erfolgt kein Aufruf an die Fachkomponente.
     */
    public Meldung storniere(String rechnungsnummer, boolean bestaetigt) {
        if (!bestaetigt) {
            return null;
        }
        try {
            dokumentService.storniere(rechnungsnummer);
            return Meldung.erfolg("Die Rechnung " + rechnungsnummer + " wurde storniert"
                    + protokoll(rechnungsnummer) + ".");
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
