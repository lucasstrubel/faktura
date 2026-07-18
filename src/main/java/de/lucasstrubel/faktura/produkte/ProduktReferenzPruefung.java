package de.lucasstrubel.faktura.produkte;

/**
 * Löschsperre für referenzierte Produkte: von Komponente A bereitgestellt,
 * von Komponente B vor jedem Löschvorgang genutzt (B-F-10).
 */
public interface ProduktReferenzPruefung {

    /** {@code true}, wenn das Produkt in mindestens einer Dokumentposition referenziert wird. */
    boolean istProduktReferenziert(String produktnummer);
}
