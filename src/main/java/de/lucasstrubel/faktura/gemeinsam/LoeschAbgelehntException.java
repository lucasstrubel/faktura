package de.lucasstrubel.faktura.gemeinsam;

/**
 * Ablehnung eines Löschvorgangs wegen referenzieller Integrität
 * (GR-04 Kunden, B-F-09 Produkte). Die Meldung enthält den Hinweis
 * für die Anwender:in (bei Kunden inkl. Anzahl verknüpfter Dokumente).
 */
public class LoeschAbgelehntException extends RuntimeException {

    public LoeschAbgelehntException(String message) {
        super(message);
    }
}
