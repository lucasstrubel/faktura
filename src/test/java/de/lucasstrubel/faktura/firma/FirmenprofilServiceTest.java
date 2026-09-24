package de.lucasstrubel.faktura.firma;

import de.lucasstrubel.faktura.FakturaApplication;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests des Firmenprofils (§ 14 UStG): kein Platzhalterprofil, Speichern und
 * Laden in einheitlicher Schreibweise, Pflichtprofil für Belege (A-F-26)
 * sowie die Formatprüfungen der optionalen Felder (Q-09, C-F-19, C-F-20).
 */
class FirmenprofilServiceTest {

    @TempDir
    Path tempDir;

    private static final Firmenprofil GUELTIG = new Firmenprofil(
            "Strubel Software", "Hauptstr. 1", "68163", "Mannheim",
            "DE123456789", "37/123/45678", "0621 123456", "info@example.de",
            "DE02 1203 0000 0000 2020 51", "BYLADEM1001", "Musterbank");

    @Test
    @DisplayName("FP-01: Ohne gespeichertes Profil gibt es kein Platzhalterprofil, und Belege werden abgelehnt (A-F-26)")
    void ohneProfilKeinPlatzhalter() {
        mitKontext(dienst -> {
            assertTrue(dienst.lade().isEmpty());
            ValidierungsException fehler = assertThrows(ValidierungsException.class,
                    () -> dienst.fuerBeleg(false));
            assertEquals("Firmenprofil", fehler.getFeldname());
        });
    }

    @Test
    @DisplayName("FP-02: Ein gespeichertes Profil wird vollständig wieder geladen")
    void gespeichertesProfilWirdGeladen() {
        mitKontext(dienst -> {
            dienst.speichere(GUELTIG);
            assertEquals(GUELTIG, dienst.lade().orElseThrow());
        });
    }

    @Test
    @DisplayName("FP-03: Erneutes Speichern ersetzt das Profil, statt ein zweites anzulegen")
    void erneutesSpeichernErsetzt() {
        mitKontext(dienst -> {
            dienst.speichere(GUELTIG);
            Firmenprofil geaendert = new Firmenprofil("Neuer Name", "Neuweg 2", "69115",
                    "Heidelberg", null, "37/123/45678", null, null, null, null, null);
            dienst.speichere(geaendert);

            assertEquals(geaendert, dienst.lade().orElseThrow());
        });
    }

    @Test
    @DisplayName("FP-04: Fehlende Pflichtfelder werden mit Feldbezug abgelehnt (Q-09)")
    void pflichtfelderWerdenGeprueft() {
        mitKontext(dienst -> {
            ValidierungsException fehler = assertThrows(ValidierungsException.class,
                    () -> dienst.speichere(new Firmenprofil("  ", "Hauptstr. 1", "68163",
                            "Mannheim", null, null, null, null, null, null, null)));
            assertEquals("Name", fehler.getFeldname());
        });
    }

    @Test
    @DisplayName("FP-05: Ungültige Formate der optionalen Felder werden abgelehnt (C-F-20)")
    void formateWerdenGeprueft() {
        mitKontext(dienst -> {
            assertEquals("PLZ", feldnameBeiFehler(dienst, mit(GUELTIG.name(), "123", null, null, null, null, null, null)));
            assertEquals("USt-IdNr.", feldnameBeiFehler(dienst, mit(GUELTIG.name(), "68163", "XY1", null, null, null, null, null)));
            assertEquals("Steuernummer", feldnameBeiFehler(dienst, mit(GUELTIG.name(), "68163", null, "12/34", null, null, null, null)));
            assertEquals("Telefon", feldnameBeiFehler(dienst, mit(GUELTIG.name(), "68163", null, null, "abc", null, null, null)));
            assertEquals("E-Mail", feldnameBeiFehler(dienst, mit(GUELTIG.name(), "68163", null, null, null, "keine-adresse", null, null)));
            assertEquals("IBAN", feldnameBeiFehler(dienst, mit(GUELTIG.name(), "68163", null, null, null, null, "DE1", null)));
            assertEquals("BIC", feldnameBeiFehler(dienst, mit(GUELTIG.name(), "68163", null, null, null, null, null, "XYZ")));
        });
    }

    @Test
    @DisplayName("FP-06: plzOrt() setzt die einzeilige Anschrift für den Briefkopf zusammen")
    void plzOrtWirdZusammengesetzt() {
        assertEquals("68163 Mannheim", GUELTIG.plzOrt());
    }

    @Test
    @DisplayName("FP-07: Rechnungen verlangen Steuernummer oder USt-IdNr., andere Belege nicht (A-F-26)")
    void rechnungVerlangtSteuerkennung() {
        mitKontext(dienst -> {
            dienst.speichere(mit(GUELTIG.name(), "68163", null, null, null, null, null, null));

            assertDoesNotThrow(() -> dienst.fuerBeleg(false));
            ValidierungsException fehler = assertThrows(ValidierungsException.class,
                    () -> dienst.fuerBeleg(true));
            assertEquals("Steuernummer", fehler.getFeldname());

            dienst.speichere(mit(GUELTIG.name(), "68163", null, "37/123/45678", null, null, null, null));
            assertDoesNotThrow(() -> dienst.fuerBeleg(true));
        });
    }

    @Test
    @DisplayName("FP-08: Eingaben werden einheitlich gespeichert — getrimmt, USt-IdNr./IBAN/BIC normalisiert (C-F-19)")
    void eingabenWerdenNormalisiert() {
        mitKontext(dienst -> {
            Firmenprofil gespeichert = dienst.speichere(new Firmenprofil(
                    "  Strubel Software ", "Hauptstr. 1", " 68163", "Mannheim",
                    "de 123 456 789", " ", null, "", "de02120300000000202051", "byladem1001", null));

            assertEquals("Strubel Software", gespeichert.name());
            assertEquals("68163", gespeichert.plz());
            assertEquals("DE123456789", gespeichert.ustIdNr());
            assertEquals("DE02 1203 0000 0000 2020 51", gespeichert.iban());
            assertEquals("BYLADEM1001", gespeichert.bic());
            assertEquals(null, gespeichert.steuernummer());
            assertEquals(null, gespeichert.eMail());
            assertEquals(gespeichert, dienst.lade().orElseThrow());
        });
    }

    @Test
    @DisplayName("FP-09: hatSteuerkennung() erkennt Steuernummer oder USt-IdNr.")
    void steuerkennungWirdErkannt() {
        assertTrue(GUELTIG.hatSteuerkennung());
        assertFalse(mit("A", "68163", null, null, null, null, null, null).hatSteuerkennung());
        assertFalse(mit("A", "68163", " ", " ", null, null, null, null).hatSteuerkennung());
    }

    /* Records kennen keine "with"-Methoden; die Varianten entstehen explizit. */
    private static Firmenprofil mit(String name, String plz, String ustIdNr, String steuernummer,
                                    String telefon, String eMail, String iban, String bic) {
        return new Firmenprofil(name, GUELTIG.strasse(), plz, GUELTIG.ort(),
                ustIdNr, steuernummer, telefon, eMail, iban, bic, null);
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
