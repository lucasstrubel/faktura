package de.lucasstrubel.faktura.dokumente;

/** Herkunft gespeicherter Beträge; keine Neuberechnung beim Laden (GR-02). */
public enum Berechnungsart {
    JE_STEUERSATZ, ALTE_POSITIONSRUNDUNG, UNGEKLAERT
}
