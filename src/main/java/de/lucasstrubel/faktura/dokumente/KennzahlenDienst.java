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

        List<Rechnung> unbezahlt = alle.stream()
                .filter(Rechnung.class::isInstance)
                .map(Rechnung.class::cast)
                .filter(rechnung -> rechnung.getStatus() == DokumentStatus.OFFEN
                        || rechnung.getStatus() == DokumentStatus.VERSENDET)
                .toList();

        List<Rechnung> ueberfaellig = unbezahlt.stream()
                .filter(rechnung -> istUeberfaellig(rechnung, heute))
                .toList();

        BigDecimal umsatzJahr = alle.stream()
                .filter(Rechnung.class::isInstance)
                .filter(beleg -> beleg.getStatus() != DokumentStatus.STORNIERT)
                .filter(beleg -> beleg.getDatum() != null
                        && beleg.getDatum().getYear() == heute.getYear())
                .map(Dokument::getSummeBrutto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Dokument> letzte = alle.stream()
                .sorted(Comparator.comparing(Dokument::getDatum,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Dokument::getBelegnummer, Comparator.reverseOrder()))
                .limit(ANZAHL_LETZTE_BELEGE)
                .toList();

        return new Kennzahlen(unbezahlt.size(), summe(unbezahlt),
                ueberfaellig.size(), summe(ueberfaellig),
                skaliere(umsatzJahr), alle.size(), letzte);
    }

    /**
     * Überfällig ist eine unbezahlte Rechnung, deren Zahlungsziel vor dem
     * Stichtag liegt (GR-06). Ohne gesetztes Zahlungsziel gilt sie nicht als
     * überfällig — die Frist ist dann schlicht unbekannt.
     */
    private static boolean istUeberfaellig(Rechnung rechnung, LocalDate heute) {
        return rechnung.getZahlungsziel() != null && rechnung.getZahlungsziel().isBefore(heute);
    }

    private static BigDecimal summe(List<Rechnung> rechnungen) {
        return skaliere(rechnungen.stream()
                .map(Dokument::getSummeBrutto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Geldbeträge stets mit Scale 2 und kaufmännischer Rundung. */
    private static BigDecimal skaliere(BigDecimal betrag) {
        return betrag.setScale(2, RoundingMode.HALF_UP);
    }
}
