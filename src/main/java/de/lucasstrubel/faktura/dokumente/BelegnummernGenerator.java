package de.lucasstrubel.faktura.dokumente;

/**
 * Vergabe eindeutiger Belegnummern je Belegtyp (GR-01, A Kapitel 6.2).
 */
public interface BelegnummernGenerator {

    /**
     * Liefert die nächste fortlaufende, lückenlose Nummer für den Belegtyp,
     * z. B. {@code "R-2026-000124"} (Präfix, Jahr, führende Nullen).
     */
    String naechsteNummer(Belegtyp typ, int jahr);
}
