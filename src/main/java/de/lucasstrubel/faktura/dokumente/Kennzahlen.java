package de.lucasstrubel.faktura.dokumente;

import java.math.BigDecimal;
import java.util.List;

/**
 * Kennzahlen der Übersichtsansicht (D-F-18).
 *
 * <p>Die Summen beruhen auf den gespeicherten Bruttobeträgen (GR-02, A-F-25).
 * Fehlt einer der einbezogenen Beträge, bleibt die jeweilige Summe {@code null};
 * der zugehörige Zähler nennt die Anzahl der Rechnungen mit unbekanntem Betrag.
 *
 * @param offeneAnzahl                   Anzahl offener und versendeter, noch nicht bezahlter Rechnungen
 * @param offenerBetrag                  Bruttosumme dieser Rechnungen, bei unbekannten Beträgen {@code null}
 * @param offeneUnbekannteAnzahl         davon mit unbekanntem Bruttobetrag
 * @param ueberfaelligAnzahl             davon mit überschrittenem Zahlungsziel
 * @param ueberfaelligBetrag             Bruttosumme der überfälligen Rechnungen, bei unbekannten Beträgen {@code null}
 * @param ueberfaelligeUnbekannteAnzahl   überfällige Rechnungen mit unbekanntem Bruttobetrag
 * @param umsatzJahr                     Bruttoumsatz der nicht stornierten Rechnungen des laufenden Jahres,
 *                                      bei unbekannten Beträgen {@code null}
 * @param umsatzJahrUnbekannteAnzahl      in den Jahresumsatz einbezogene Rechnungen mit unbekanntem Bruttobetrag
 * @param belegeGesamt                   Anzahl aller Belege
 * @param letzteBelege                   jüngste Belege, absteigend nach Datum
 */
public record Kennzahlen(int offeneAnzahl,
                         BigDecimal offenerBetrag,
                         int offeneUnbekannteAnzahl,
                         int ueberfaelligAnzahl,
                         BigDecimal ueberfaelligBetrag,
                         int ueberfaelligeUnbekannteAnzahl,
                         BigDecimal umsatzJahr,
                         int umsatzJahrUnbekannteAnzahl,
                         int belegeGesamt,
                         List<Dokument> letzteBelege) {

    public Kennzahlen {
        letzteBelege = List.copyOf(letzteBelege);
    }
}
