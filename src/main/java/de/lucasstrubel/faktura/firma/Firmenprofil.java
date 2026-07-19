package de.lucasstrubel.faktura.firma;

/**
 * Stammdaten des Rechnungsausstellers (§ 14 UStG): Name und Anschrift
 * erscheinen im Briefkopf jedes Belegs, die Bankverbindung im
 * Zahlungshinweis der Rechnung und in der E-Rechnung.
 *
 * @param name     Firmen- oder Personenname (Pflichtfeld)
 * @param strasse  Straße und Hausnummer (Pflichtfeld)
 * @param plz      Postleitzahl (Pflichtfeld, 5 Ziffern)
 * @param ort      Ort (Pflichtfeld)
 * @param ustIdNr  Umsatzsteuer-Identifikationsnummer (optional, Format DE + 9 Ziffern)
 * @param telefon  Telefonnummer (optional)
 * @param eMail    E-Mail-Adresse (optional)
 * @param iban     IBAN der Bankverbindung (optional)
 * @param bic      BIC der Bankverbindung (optional)
 * @param bank     Name der Bank (optional)
 */
public record Firmenprofil(String name, String strasse, String plz, String ort,
                           String ustIdNr, String telefon, String eMail,
                           String iban, String bic, String bank) {

    /** Voreinstellung, solange noch kein Profil gespeichert wurde. */
    public static Firmenprofil standard() {
        return new Firmenprofil("Faktura Software", "Musterstraße 1", "68163", "Mannheim",
                "DE000000000", null, null, null, null, null);
    }

    /** Einzeilige Anschrift {@code PLZ Ort} für Briefkopf und E-Rechnung. */
    public String plzOrt() {
        return plz + " " + ort;
    }
}
