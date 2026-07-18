package de.lucasstrubel.faktura.kunden;

import java.util.List;

/**
 * Lesender Zugriff auf Kundenstammdaten — von Gruppe C implementiert,
 * von Gruppe A (Dokumentenzyklus) und Gruppe D (GUI) genutzt (C-F-14).
 */
public interface KundenService {

    /** Liefert den Kunden zur Kundennummer oder {@code null}, wenn nicht vorhanden. */
    Kunde findeKunde(String kundennummer);

    /** Volltextsuche über Name oder Kundennummer (Teilstring, case-insensitive, C-F-12). */
    List<Kunde> suche(String suchbegriff);
}
