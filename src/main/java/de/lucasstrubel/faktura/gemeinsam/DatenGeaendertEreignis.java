package de.lucasstrubel.faktura.gemeinsam;

/**
 * Spring-Anwendungsereignis: Ein Service hat schreibend auf den angegebenen
 * {@link DatenBereich} zugegriffen. Die Services veröffentlichen das Ereignis
 * über den {@code ApplicationEventPublisher}; der {@link EreignisBus} leitet
 * es an die abonnierten Modulansichten weiter (Observer-Muster).
 *
 * @param bereich der geänderte Datenbereich
 */
public record DatenGeaendertEreignis(DatenBereich bereich) {
}
