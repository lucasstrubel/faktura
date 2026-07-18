package de.lucasstrubel.faktura.gui;

/**
 * Im Wizard erfasste Position: gewähltes Produkt und Stückzahl
 * (Komponente D, Kapitel 6.1).
 */
public record PositionsEingabe(String produktnummer, int menge) {
}
