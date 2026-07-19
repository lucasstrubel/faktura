package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.gemeinsam.Csv;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static de.lucasstrubel.faktura.gemeinsam.Csv.TRENNZEICHEN;
import static de.lucasstrubel.faktura.gemeinsam.Csv.feld;

import org.springframework.stereotype.Component;

/**
 * Export aller Belege (Bewegungsdaten) als CSV (Q-08, IF-04): UTF-8,
 * Semikolon-getrennt, mit Kopfzeile. Es wird eine Zeile je Dokumentposition
 * (denormalisiert) geschrieben, sodass der vollständige Datenbestand inklusive
 * Kopf- und Positionsdaten enthalten ist. Belege ohne Positionen erscheinen mit
 * leeren Positionsfeldern, damit kein Beleg verloren geht.
 */
@Component
public class DokumentCsvExport {

    private final DokumentRepository repository;

    public DokumentCsvExport(DokumentRepository repository) {
        this.repository = repository;
    }

    public void exportiereCsv(Path zielDatei) {
        List<String> zeilen = new ArrayList<>();
        zeilen.add(String.join(TRENNZEICHEN,
                "belegnummer", "belegtyp", "datum", "status", "vorgaengerNr",
                "kundenNr", "kundeName", "kundeAnschrift",
                "summeNetto", "summeSteuer", "summeBrutto",
                "zahlungsziel", "storniertAm", "storniertVon",
                "produktnummer", "bezeichnung", "menge",
                "einzelpreisNetto", "steuersatz", "positionssummeNetto", "positionssummeBrutto"));
        for (Dokument dokument : repository.alle()) {
            if (dokument.getPositionen().isEmpty()) {
                zeilen.add(belegFelder(dokument) + TRENNZEICHEN + leerePositionsFelder());
            } else {
                for (Dokumentposition position : dokument.getPositionen()) {
                    zeilen.add(belegFelder(dokument) + TRENNZEICHEN + positionsFelder(position));
                }
            }
        }
        Csv.schreibe(zielDatei, zeilen);
    }

    private static String belegFelder(Dokument dokument) {
        Rechnung rechnung = dokument instanceof Rechnung r ? r : null;
        return String.join(TRENNZEICHEN,
                feld(dokument.getBelegnummer()),
                feld(dokument.belegtyp().name()),
                feld(datum(dokument.getDatum())),
                feld(dokument.getStatus() == null ? null : dokument.getStatus().name()),
                feld(dokument.getVorgaengerNr()),
                feld(dokument.getKundenReferenz()),
                feld(dokument.getKundeName()),
                feld(dokument.getKundeAnschrift()),
                feld(betrag(dokument.getSummeNetto())),
                feld(betrag(dokument.getSummeSteuer())),
                feld(betrag(dokument.getSummeBrutto())),
                feld(rechnung == null ? null : datum(rechnung.getZahlungsziel())),
                feld(rechnung == null ? null : datum(rechnung.getStorniertAm())),
                feld(rechnung == null ? null : rechnung.getStorniertVon()));
    }

    private static String positionsFelder(Dokumentposition position) {
        return String.join(TRENNZEICHEN,
                feld(position.getProduktReferenz()),
                feld(position.getBezeichnung()),
                feld(Integer.toString(position.getMenge())),
                feld(betrag(position.getEinzelpreisNetto())),
                feld(betrag(position.getSteuersatz())),
                feld(betrag(position.getPositionssummeNetto())),
                feld(betrag(position.getPositionssummeBrutto())));
    }

    private static String leerePositionsFelder() {
        return TRENNZEICHEN.repeat(6);
    }

    private static String datum(LocalDate datum) {
        return datum == null ? null : datum.toString();
    }

    private static String betrag(BigDecimal wert) {
        return wert == null ? null : wert.toPlainString();
    }
}
