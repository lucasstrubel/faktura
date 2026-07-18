package de.lucasstrubel.faktura.dokumente;

/**
 * Eingabedaten einer Belegposition: Produktreferenz und Menge.
 * Aus dieser Angabe erzeugt der {@link DokumentService} die
 * {@link Dokumentposition} mit Preis-/Steuersatz-Snapshot (GR-03).
 */
public record Positionsangabe(String produktnummer, int menge) {
}
