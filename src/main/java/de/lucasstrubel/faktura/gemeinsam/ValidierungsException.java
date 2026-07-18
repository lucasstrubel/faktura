package de.lucasstrubel.faktura.gemeinsam;

/**
 * Validierungsfehler, der das betroffene Eingabefeld namentlich benennt
 * (Q-09: Pflichtfeldhinweise; A-F-18, B-F-04, C-F-03, D-F-16).
 */
public class ValidierungsException extends RuntimeException {

    private final String feldname;

    public ValidierungsException(String feldname, String message) {
        super(message);
        this.feldname = feldname;
    }

    /** Name des fehlenden oder ungültigen Eingabefelds, z. B. "Ort". */
    public String getFeldname() {
        return feldname;
    }
}
