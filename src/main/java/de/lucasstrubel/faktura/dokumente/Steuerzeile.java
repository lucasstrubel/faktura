package de.lucasstrubel.faktura.dokumente;

import java.math.BigDecimal;

/**
 * Umsatzsteuer eines Belegs für einen Steuersatz (A-F-25): Nettobetrag aller
 * Positionen mit diesem Satz und der darauf einmal gerundete Steuerbetrag.
 * § 14 Abs. 4 Nr. 7/8 UStG verlangt das Entgelt und den Steuerbetrag je
 * Steuersatz; EN 16931 (BR-CO-17) rechnet ebenso je Satz.
 *
 * @param steuersatz Steuersatz als Faktor, z. B. {@code 0.19}
 * @param netto      Summe der Positionsnettobeträge mit diesem Satz, Scale 2
 * @param steuer     {@code netto * steuersatz}, Scale 2, HALF_UP
 */
public record Steuerzeile(BigDecimal steuersatz, BigDecimal netto, BigDecimal steuer) {
}
