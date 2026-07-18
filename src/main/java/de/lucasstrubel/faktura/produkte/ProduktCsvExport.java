package de.lucasstrubel.faktura.produkte;

import de.lucasstrubel.faktura.gemeinsam.Csv;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static de.lucasstrubel.faktura.gemeinsam.Csv.TRENNZEICHEN;
import static de.lucasstrubel.faktura.gemeinsam.Csv.feld;

/**
 * Export aller Produktstammdaten als CSV (B-F-15, Q-08):
 * UTF-8, Semikolon-getrennt, mit Kopfzeile, alle Attribute.
 */
public class ProduktCsvExport {

    private final ProduktRepository repository;

    public ProduktCsvExport(ProduktRepository repository) {
        this.repository = repository;
    }

    public void exportiereCsv(Path zielDatei) {
        List<String> zeilen = new ArrayList<>();
        zeilen.add(String.join(TRENNZEICHEN,
                "produktnummer", "bezeichnung", "beschreibung", "einzelpreisNetto", "steuersatz", "einheit"));
        for (Produkt p : repository.alleSortiertNachBezeichnung()) {
            zeilen.add(String.join(TRENNZEICHEN,
                    feld(p.getProduktnummer()), feld(p.getBezeichnung()), feld(p.getBeschreibung()),
                    feld(p.getEinzelpreisNetto() == null ? null : p.getEinzelpreisNetto().toPlainString()),
                    feld(p.getSteuersatz() == null ? null : p.getSteuersatz().toPlainString()),
                    feld(p.getEinheit())));
        }
        Csv.schreibe(zielDatei, zeilen);
    }
}
