package de.lucasstrubel.faktura.dokumente;

import java.math.BigDecimal;

/**
 * Berechnete Netto-, Steuer- und Bruttosumme eines Belegs (F-23);
 * wird u. a. für die Wizard-Zusammenfassung (D-F-12) bereitgestellt.
 */
public record Summen(BigDecimal netto, BigDecimal steuer, BigDecimal brutto) {
}
