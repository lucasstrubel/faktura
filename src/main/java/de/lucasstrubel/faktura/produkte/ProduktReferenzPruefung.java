package de.lucasstrubel.faktura.produkte;

/**
 * Löschsperre für referenzierte Produkte: von Gruppe A bereitgestellt,
 * von Gruppe B vor jedem Löschvorgang genutzt (B-F-10).
 */
public interface ProduktReferenzPruefung {

    /** {@code true}, wenn das Produkt in mindestens einer Dokumentposition referenziert wird. */
    boolean istProduktReferenziert(String produktnummer);
}
