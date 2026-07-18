package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.kunden.KundenReferenzPruefung;
import de.lucasstrubel.faktura.produkte.ProduktReferenzPruefung;

/**
 * Von Gruppe A bereitgestellte Referenzprüfungen für die Löschsperren der
 * Stammdatenmodule: GR-04 (Kunden, C-F-10) und B-F-10 (Produkte).
 */
public class DokumentReferenzPruefung implements KundenReferenzPruefung, ProduktReferenzPruefung {

    private final DokumentRepository repository;

    public DokumentReferenzPruefung(DokumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public int anzahlVerknuepfterDokumente(String kundennummer) {
        return (int) repository.alle().stream()
                .filter(d -> kundennummer.equals(d.getKundenReferenz()))
                .count();
    }

    @Override
    public boolean istProduktReferenziert(String produktnummer) {
        return repository.alle().stream()
                .flatMap(d -> d.getPositionen().stream())
                .anyMatch(p -> produktnummer.equals(p.getProduktReferenz()));
    }
}
