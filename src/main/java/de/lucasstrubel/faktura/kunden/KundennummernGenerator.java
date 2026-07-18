package de.lucasstrubel.faktura.kunden;

/**
 * Vergabe eindeutiger, fortlaufender Kundennummern (C-F-02).
 */
public interface KundennummernGenerator {

    /** Liefert die nächste fortlaufende Kundennummer, z. B. {@code "K-000017"}. */
    String naechsteNummer();
}
