package de.lucasstrubel.faktura.produkte;

import java.util.List;

/**
 * Persistenz der Produktstammdaten im lokalen Dateisystem (IF-01, B Kapitel 6.2).
 */
public interface ProduktRepository {

    Produkt speichere(Produkt produkt);

    void loesche(String produktnummer);

    /** Liefert das Produkt zur Nummer oder {@code null}. */
    Produkt findeNachNummer(String produktnummer);

    List<Produkt> alleSortiertNachBezeichnung();

    /** Suche über Bezeichnung ODER Produktnummer (Teilstring, case-insensitive). */
    List<Produkt> suche(String suchbegriff);
}
