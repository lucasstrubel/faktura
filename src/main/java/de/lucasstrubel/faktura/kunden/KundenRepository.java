package de.lucasstrubel.faktura.kunden;

import java.util.List;

/**
 * Persistenz der Kundenstammdaten im lokalen Dateisystem (IF-01, C Kapitel 6.2).
 */
public interface KundenRepository {

    Kunde speichere(Kunde kunde);

    void loesche(String kundennummer);

    /** Liefert den Kunden zur Nummer oder {@code null}. */
    Kunde findeNachNummer(String kundennummer);

    List<Kunde> alleSortiertNachName();

    /** Suche über Name ODER Kundennummer (Teilstring, case-insensitive). */
    List<Kunde> suche(String suchbegriff);
}
