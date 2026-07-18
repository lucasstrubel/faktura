package de.lucasstrubel.faktura.gui;

/**
 * Gemeinsame Schnittstelle der Modulansichten für das Hauptfenster
 * (D-F-01, F-02): Navigation und Schutz ungespeicherter Eingaben.
 */
public interface ModulPanel {

    /** {@code true}, wenn das Formular ungespeicherte Eingaben enthält (F-02). */
    boolean hatUngespeicherteAenderungen();

    /** Lädt die Daten der Ansicht neu (z. B. nach Modulwechsel). */
    void aktualisiere();
}
