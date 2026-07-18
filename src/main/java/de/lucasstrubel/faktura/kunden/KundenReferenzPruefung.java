package de.lucasstrubel.faktura.kunden;

/**
 * Löschsperre GR-04: von Gruppe A bereitgestellt, von Gruppe C vor jedem
 * Löschvorgang genutzt (C-F-10).
 */
public interface KundenReferenzPruefung {

    /** Anzahl aktiver und archivierter Dokumente, die den Kunden referenzieren. */
    int anzahlVerknuepfterDokumente(String kundennummer);
}
