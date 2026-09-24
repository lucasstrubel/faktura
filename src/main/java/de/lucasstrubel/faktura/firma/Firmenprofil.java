package de.lucasstrubel.faktura.firma;

import de.lucasstrubel.faktura.gemeinsam.Validierung;

/**
 * Stammdaten des Rechnungsausstellers (§ 14 UStG): Name und Anschrift
 * erscheinen im Briefkopf jedes Belegs, Steuernummer bzw. USt-IdNr. auf der
 * Rechnung, die Bankverbindung im Zahlungshinweis und in der E-Rechnung.
 *
 * <p>Belege übernehmen das Profil beim Erstellen als Snapshot (A-F-27);
 * spätere Änderungen wirken nur auf neue Belege.
 *
 * @param name         Firmen- oder Personenname (Pflichtfeld)
 * @param strasse      Straße und Hausnummer (Pflichtfeld)
 * @param plz          Postleitzahl (Pflichtfeld, 5 Ziffern)
 * @param ort          Ort (Pflichtfeld)
 * @param ustIdNr      Umsatzsteuer-Identifikationsnummer (optional)
 * @param steuernummer Steuernummer des Finanzamts (optional; für Rechnungen
 *                     ist Steuernummer oder USt-IdNr. Pflicht, § 14 Abs. 4 Nr. 2 UStG)
 * @param telefon      Telefonnummer (optional)
 * @param eMail        E-Mail-Adresse (optional)
 * @param iban         IBAN der Bankverbindung (optional)
 * @param bic          BIC der Bankverbindung (optional)
 * @param bank         Name der Bank (optional)
 */
public record Firmenprofil(String name, String strasse, String plz, String ort,
                           String ustIdNr, String steuernummer, String telefon, String eMail,
                           String iban, String bic, String bank) {

    /** Einzeilige Anschrift {@code PLZ Ort} für Briefkopf und E-Rechnung. */
    public String plzOrt() {
        return plz + " " + ort;
    }

    /** Steuernummer oder USt-IdNr. vorhanden — Voraussetzung für Rechnungen (A-F-26). */
    public boolean hatSteuerkennung() {
        return (ustIdNr != null && !ustIdNr.isBlank())
                || (steuernummer != null && !steuernummer.isBlank());
    }

    /** Einheitliche Schreibweise aller Felder, wie sie gespeichert wird (C-F-19). */
    public Firmenprofil bereinigt() {
        return new Firmenprofil(
                Validierung.bereinige(name), Validierung.bereinige(strasse),
                Validierung.bereinige(plz), Validierung.bereinige(ort),
                Validierung.normalisiereUstIdNr(ustIdNr), Validierung.bereinige(steuernummer),
                Validierung.bereinige(telefon), Validierung.bereinige(eMail),
                Validierung.normalisiereIban(iban), Validierung.normalisiereBic(bic),
                Validierung.bereinige(bank));
    }
}
