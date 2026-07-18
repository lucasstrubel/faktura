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
}
