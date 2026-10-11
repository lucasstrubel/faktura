package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.kunden.KundenReferenzPruefung;
import de.lucasstrubel.faktura.produkte.ProduktReferenzPruefung;

import org.springframework.stereotype.Component;

/**
 * Von Komponente A bereitgestellte Referenzprüfungen für die Löschsperren der
 * Stammdatenmodule: GR-04 (Kunden, C-F-10) und B-F-10 (Produkte).
 */
@Component
public class DokumentReferenzPruefung implements KundenReferenzPruefung, ProduktReferenzPruefung {

    private final DokumentRepository repository;

    public DokumentReferenzPruefung(DokumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public int anzahlVerknuepfterDokumente(String kundennummer) {
        return repository.anzahlFuerKunde(kundennummer);
    }

    @Override
    public boolean istProduktReferenziert(String produktnummer) {
        return repository.istProduktReferenziert(produktnummer);
    }
}
