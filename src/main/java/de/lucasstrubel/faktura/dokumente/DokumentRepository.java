package de.lucasstrubel.faktura.dokumente;

import java.util.List;

/**
 * Persistenz der Belege im lokalen Dateisystem (IF-01, A Kapitel 7).
 * Belege werden nie gelöscht (GoBD: lückenlose Erfassung).
 */
public interface DokumentRepository {

    Dokument speichere(Dokument dokument);

    /** Liefert den Beleg zur Belegnummer oder {@code null}. */
    Dokument findeNachNummer(String belegnummer);

    List<Dokument> alle();

    default int anzahlFuerKunde(String kundennummer) {
        return (int) alle().stream().filter(d -> kundennummer.equals(d.getKundenReferenz())).count();
    }

    default boolean istProduktReferenziert(String produktnummer) {
        return alle().stream().flatMap(d -> d.getPositionen().stream())
                .anyMatch(p -> produktnummer.equals(p.getProduktReferenz()));
    }
}
