package de.lucasstrubel.faktura.kunden;

import de.lucasstrubel.faktura.gemeinsam.Csv;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static de.lucasstrubel.faktura.gemeinsam.Csv.TRENNZEICHEN;
import static de.lucasstrubel.faktura.gemeinsam.Csv.feld;

/**
 * Export aller Kundenstammdaten als CSV (C-F-15, Q-08):
 * UTF-8, Semikolon-getrennt, mit Kopfzeile, alle Attribute.
 */
public class KundenCsvExport {

    private final KundenRepository repository;

    public KundenCsvExport(KundenRepository repository) {
        this.repository = repository;
    }

    public void exportiereCsv(Path zielDatei) {
        List<String> zeilen = new ArrayList<>();
        zeilen.add(String.join(TRENNZEICHEN,
                "kundennummer", "name", "strasse", "plz", "ort", "eMail", "telefon", "ustIdNr"));
        for (Kunde k : repository.alleSortiertNachName()) {
            zeilen.add(String.join(TRENNZEICHEN,
                    feld(k.getKundennummer()), feld(k.getName()), feld(k.getStrasse()),
                    feld(k.getPlz()), feld(k.getOrt()), feld(k.getEMail()),
                    feld(k.getTelefon()), feld(k.getUstIdNr())));
        }
        Csv.schreibe(zielDatei, zeilen);
    }
}
