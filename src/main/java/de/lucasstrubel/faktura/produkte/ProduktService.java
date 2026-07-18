package de.lucasstrubel.faktura.produkte;

import java.util.List;

/**
 * Lesender Zugriff auf Produktstammdaten — von Komponente B implementiert,
 * von Komponente A (Dokumentenzyklus) und Komponente D (GUI) genutzt (B-F-14).
 */
public interface ProduktService {

    /** Liefert das Produkt zur Produktnummer oder {@code null}, wenn nicht vorhanden. */
    Produkt findeProdukt(String produktnummer);

    /** Suche über Bezeichnung oder Produktnummer (Teilstring, case-insensitive, B-F-12). */
    List<Produkt> suche(String suchbegriff);
}
