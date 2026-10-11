package de.lucasstrubel.faktura.dokumente;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Liefert die Kennzahlen der Übersichtsansicht (D-F-18): offener Betrag,
 * überfällige Rechnungen, Jahresumsatz und die jüngsten Belege.
 *
 * <p>Bewusst frei von JavaFX: Die Auswertung ist Fachlogik der Komponente A
 * und damit ohne laufende Oberfläche prüfbar.
 * Gespeicherte Bruttobeträge bleiben maßgeblich (GR-02, A-F-25): Unbekannte
 * Beträge werden je Kennzahl gezählt und verhindern eine scheinbare Gesamtsumme.
 */
@Service
public class KennzahlenDienst {

    /** Anzahl der in der Übersicht gezeigten jüngsten Belege. */
    public static final int ANZAHL_LETZTE_BELEGE = 8;

    private final DokumentService dokumentService;

    public KennzahlenDienst(DokumentService dokumentService) {
        this.dokumentService = dokumentService;
    }

    /** Wertet den gesamten Belegbestand zum Stichtag {@code heute} aus. */
    public Kennzahlen ermittle(LocalDate heute) {
        List<Dokument> alle = dokumentService.alleDokumente();

        // Stornorechnungen sind keine Forderungen, sondern gleichen eine
        // stornierte Rechnung aus; beide zählen weder als offen noch als Umsatz
        List<Rechnung> rechnungen = alle.stream()
                .filter(Rechnung.class::isInstance)
                .map(Rechnung.class::cast)
                .filter(rechnung -> !rechnung.istStornorechnung())
                .toList();

        List<Rechnung> unbezahlt = rechnungen.stream()
                .filter(rechnung -> rechnung.getStatus() == DokumentStatus.OFFEN
                        || rechnung.getStatus() == DokumentStatus.VERSENDET)
                .filter(rechnung -> !rechnung.istBezahlt())
                .toList();

        List<Rechnung> ueberfaellig = unbezahlt.stream()
                .filter(rechnung -> istUeberfaellig(rechnung, heute))
                .toList();

        List<Rechnung> jahresrechnungen = rechnungen.stream()
                .filter(beleg -> beleg.getStatus() != DokumentStatus.STORNIERT)
                .filter(beleg -> beleg.getDatum() != null
                        && beleg.getDatum().getYear() == heute.getYear())
                .toList();

        List<Dokument> letzte = alle.stream()
                .sorted(Comparator.comparing(Dokument::getDatum,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Dokument::getBelegnummer, Comparator.reverseOrder()))
                .limit(ANZAHL_LETZTE_BELEGE)
                .toList();

        return new Kennzahlen(unbezahlt.size(), summe(unbezahlt), anzahlUnbekannterBetraege(unbezahlt),
                ueberfaellig.size(), summe(ueberfaellig), anzahlUnbekannterBetraege(ueberfaellig),
                summe(jahresrechnungen), anzahlUnbekannterBetraege(jahresrechnungen), alle.size(), letzte);
    }

    /**
     * Überfällig ist eine unbezahlte (A-F-28) Rechnung, deren Zahlungsziel vor dem
     * Stichtag liegt (GR-06). Ohne gesetztes Zahlungsziel gilt sie nicht als
     * überfällig — die Frist ist dann schlicht unbekannt.
     */
    private static boolean istUeberfaellig(Rechnung rechnung, LocalDate heute) {
        return rechnung.getZahlungsziel() != null && rechnung.getZahlungsziel().isBefore(heute);
    }

    /** Zählt fehlende gespeicherte Bruttobeträge, ohne sie neu zu berechnen (GR-02, A-F-25). */
    private static int anzahlUnbekannterBetraege(List<Rechnung> rechnungen) {
        return (int) rechnungen.stream()
                .filter(rechnung -> rechnung.getSummeBrutto() == null)
                .count();
    }

    /** Liefert bei unbekannten Beträgen keine Teilsumme als Gesamtbetrag (GR-02, A-F-25). */
    private static BigDecimal summe(List<Rechnung> rechnungen) {
        if (rechnungen.stream().anyMatch(rechnung -> rechnung.getSummeBrutto() == null)) {
            return null;
        }
        return skaliere(rechnungen.stream()
                .map(Dokument::getSummeBrutto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Geldbeträge stets mit Scale 2 und kaufmännischer Rundung. */
    private static BigDecimal skaliere(BigDecimal betrag) {
        return betrag == null ? null : betrag.setScale(2, RoundingMode.HALF_UP);
    }
}
