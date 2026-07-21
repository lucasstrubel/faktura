package de.lucasstrubel.faktura.gemeinsam;

/**
 * Ein Beleg konnte nicht ausgegeben werden (IF-02 Druck, IF-03 E-Mail): kein
 * Drucker eingerichtet, kein Standard-Mailprogramm vorhanden oder das
 * Betriebssystem unterstützt den Vorgang nicht.
 *
 * <p>Bewusst von {@link ValidierungsException} und
 * {@link LoeschAbgelehntException} getrennt: Es liegt kein Eingabe- oder
 * Regelverstoß vor, sondern eine Einschränkung der Arbeitsumgebung — der
 * Anwender kann sie beheben, ohne seine Daten zu ändern.
 */
public class AusgabeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AusgabeException(String meldung) {
        super(meldung);
    }

    public AusgabeException(String meldung, Throwable ursache) {
        super(meldung, ursache);
    }
}
