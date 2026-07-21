package de.lucasstrubel.faktura.dokumente;

import java.math.BigDecimal;
import java.util.List;

/**
 * Kennzahlen der Übersichtsansicht (D-F-18).
 *
 * @param offeneAnzahl       Anzahl offener und versendeter Rechnungen
 * @param offenerBetrag      Bruttosumme dieser Rechnungen
 * @param ueberfaelligAnzahl davon mit überschrittenem Zahlungsziel
 * @param ueberfaelligBetrag Bruttosumme der überfälligen Rechnungen
 * @param umsatzJahr         Bruttoumsatz der nicht stornierten Rechnungen des laufenden Jahres
 * @param belegeGesamt       Anzahl aller Belege
 * @param letzteBelege       jüngste Belege, absteigend nach Datum
 */
public record Kennzahlen(int offeneAnzahl,
                         BigDecimal offenerBetrag,
                         int ueberfaelligAnzahl,
                         BigDecimal ueberfaelligBetrag,
                         BigDecimal umsatzJahr,
                         int belegeGesamt,
                         List<Dokument> letzteBelege) {

    public Kennzahlen {
        letzteBelege = List.copyOf(letzteBelege);
    }
}
