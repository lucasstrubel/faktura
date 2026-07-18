package de.lucasstrubel.faktura.produkte;

/**
 * Vergabe eindeutiger, fortlaufender Produktnummern (B-F-02).
 */
public interface ProduktnummernGenerator {

    /** Liefert die nächste fortlaufende Produktnummer, z. B. {@code "P-000042"}. */
    String naechsteNummer();
}
