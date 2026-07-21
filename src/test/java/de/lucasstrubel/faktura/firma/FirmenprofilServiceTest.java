package de.lucasstrubel.faktura.firma;

import de.lucasstrubel.faktura.FakturaApplication;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests des Firmenprofils (§ 14 UStG): Voreinstellung, Speichern und Laden
 * sowie die Formatprüfungen der optionalen Felder (Q-09).
 */
class FirmenprofilServiceTest {

    @TempDir
    Path tempDir;

    private static final Firmenprofil GUELTIG = new Firmenprofil(
            "Strubel Software", "Hauptstr. 1", "68163", "Mannheim",
            "DE123456789", "0621 123456", "info@example.de",
            "DE02120300000000202051", "BYLADEM1001", "Musterbank");

    @Test
    @DisplayName("FP-01: Ohne gespeichertes Profil liefert der Dienst die Voreinstellung")
    void ohneProfilVoreinstellung() {
        mitKontext(dienst -> assertEquals(Firmenprofil.standard(), dienst.lade()));
    }

    @Test
    @DisplayName("FP-02: Ein gespeichertes Profil wird vollständig wieder geladen")
    void gespeichertesProfilWirdGeladen() {
        mitKontext(dienst -> {
            dienst.speichere(GUELTIG);
            assertEquals(GUELTIG, dienst.lade());
        });
    }

    @Test
    @DisplayName("FP-03: Erneutes Speichern ersetzt das Profil, statt ein zweites anzulegen")
    void erneutesSpeichernErsetzt() {
        mitKontext(dienst -> {
            dienst.speichere(GUELTIG);
            Firmenprofil geaendert = new Firmenprofil("Neuer Name", "Neuweg 2", "69115",
                    "Heidelberg", null, null, null, null, null, null);
            dienst.speichere(geaendert);

            assertEquals(geaendert, dienst.lade());
        });
    }

    @Test
    @DisplayName("FP-04: Fehlende Pflichtfelder werden mit Feldbezug abgelehnt (Q-09)")
    void pflichtfelderWerdenGeprueft() {
        mitKontext(dienst -> {
            ValidierungsException fehler = assertThrows(ValidierungsException.class,
                    () -> dienst.speichere(new Firmenprofil("  ", "Hauptstr. 1", "68163",
                            "Mannheim", null, null, null, null, null, null)));
            assertEquals("Name", fehler.getFeldname());
        });
    }

    @Test
    @DisplayName("FP-05: Ungültige Formate der optionalen Felder werden abgelehnt")
    void formateWerdenGeprueft() {
        mitKontext(dienst -> {
            assertEquals("PLZ", feldnameBeiFehler(dienst, mitPlz("123")));
            assertEquals("USt-IdNr.", feldnameBeiFehler(dienst, mitUstIdNr("XY1")));
            assertEquals("E-Mail", feldnameBeiFehler(dienst, mitEMail("keine-adresse")));
            assertEquals("IBAN", feldnameBeiFehler(dienst, mitIban("DE1")));
            assertEquals("Telefon", feldnameBeiFehler(dienst, mitTelefon("abc")));
        });
    }

    /* Records kennen keine "with"-Methoden; die Varianten entstehen explizit. */

    private static Firmenprofil mitPlz(String plz) {
        return new Firmenprofil(GUELTIG.name(), GUELTIG.strasse(), plz, GUELTIG.ort(),
                null, null, null, null, null, null);
    }

    private static Firmenprofil mitUstIdNr(String ustIdNr) {
        return new Firmenprofil(GUELTIG.name(), GUELTIG.strasse(), GUELTIG.plz(), GUELTIG.ort(),
                ustIdNr, null, null, null, null, null);
    }

    private static Firmenprofil mitEMail(String eMail) {
        return new Firmenprofil(GUELTIG.name(), GUELTIG.strasse(), GUELTIG.plz(), GUELTIG.ort(),
                null, null, eMail, null, null, null);
    }

    private static Firmenprofil mitIban(String iban) {
        return new Firmenprofil(GUELTIG.name(), GUELTIG.strasse(), GUELTIG.plz(), GUELTIG.ort(),
                null, null, null, iban, null, null);
    }

    private static Firmenprofil mitTelefon(String telefon) {
        return new Firmenprofil(GUELTIG.name(), GUELTIG.strasse(), GUELTIG.plz(), GUELTIG.ort(),
                null, telefon, null, null, null, null);
    }

    @Test
    @DisplayName("FP-06: plzOrt() setzt die einzeilige Anschrift für den Briefkopf zusammen")
    void plzOrtWirdZusammengesetzt() {
        assertEquals("68163 Mannheim", GUELTIG.plzOrt());
    }

    private static String feldnameBeiFehler(FirmenprofilService dienst, Firmenprofil profil) {
        return assertThrows(ValidierungsException.class, () -> dienst.speichere(profil))
                .getFeldname();
    }

    private void mitKontext(java.util.function.Consumer<FirmenprofilService> pruefung) {
        try (ConfigurableApplicationContext kontext = new SpringApplicationBuilder(
                FakturaApplication.class)
                .headless(true)
                .run("--faktura.daten-verzeichnis=" + tempDir)) {
            pruefung.accept(kontext.getBean(FirmenprofilService.class));
        }
    }
}
