package de.lucasstrubel.faktura.gui;

/**
 * Im Wizard erfasste Position: gewähltes Produkt und Stückzahl
 * (Gruppe D, Kapitel 6.1).
 */
public record PositionsEingabe(String produktnummer, int menge) {
}
