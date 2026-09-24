package de.lucasstrubel.faktura.gui;

/**
 * Je Beleg verfügbare Aktionen der Dokumentliste (D-F-07, F-08, F-14):
 * inhaltliche Änderungsaktionen sind bei versendeten/stornierten Belegen
 * deaktiviert, der PDF-Export bleibt stets verfügbar. Stornieren und
 * Zahlungseingang gelten für offene und versendete Rechnungen, die weder
 * bezahlt noch selbst eine Stornorechnung sind (A-F-28, A-F-29).
 */
public record BelegAktionen(boolean stornierbar, boolean aenderbar, boolean pdfExport,
                            boolean bezahltMarkierbar) {
}
