package de.lucasstrubel.faktura.gemeinsam;

import de.lucasstrubel.faktura.FakturaApplication;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenVerwaltungsService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests der Datensicherung (Q-06, IF-01). Entscheidend ist, dass die
 * gesicherte Datenbank ein <em>konsistenter</em> Abzug ist: Die frühere
 * Umsetzung kopierte die laufende Datei, wodurch im WAL-Modus die jüngsten
 * Änderungen fehlen konnten. {@code VACUUM INTO} schließt das aus.
 */
class DatensicherungTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("DS-01: Die Sicherung enthält eine gültige Datenbank mit den aktuellen Daten")
    void sicherungEnthaeltKonsistenteDatenbank() throws Exception {
        Path ziel = tempDir.resolve("sicherung.zip");

        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            // Kunde anlegen und sofort sichern: bei laufenden Verbindungen und
            // aktivem WAL muss der Abzug den Kunden trotzdem enthalten
            kontext.getBean(KundenVerwaltungsService.class)
                    .legeAn(new Kunde("Muster GmbH", "Hauptstr. 1", "68163", "Mannheim"));
            kontext.getBean(Datensicherung.class).erstelle(tempDir, ziel);
        }

        assertTrue(Files.exists(ziel), "die Sicherungsdatei muss angelegt sein");
        assertTrue(entpackeEintraege(ziel).contains("faktura.db"),
                "die Sicherung muss die Datenbank enthalten");

        Path entpackt = entpackeDatenbank(ziel);
        assertEquals("ok", integritaetspruefung(entpackt),
                "der Abzug muss SQLites Integritätsprüfung bestehen");
        assertEquals(1, anzahlKunden(entpackt),
                "der unmittelbar zuvor angelegte Kunde muss im Abzug enthalten sein");
    }

    @Test
    @DisplayName("DS-02: Vorhandene JSON-Bestände werden mitgesichert, Logdateien nicht")
    void jsonBestaendeWerdenMitgesichert() throws Exception {
        Files.writeString(tempDir.resolve("kunden.json"), "[]");
        Files.writeString(tempDir.resolve("faktura.log"), "Protokoll");
        Path ziel = tempDir.resolve("sicherung.zip");

        try (ConfigurableApplicationContext kontext = neuerKontext()) {
            kontext.getBean(Datensicherung.class).erstelle(tempDir, ziel);
        }

        List<String> eintraege = entpackeEintraege(ziel);
        assertTrue(eintraege.contains("kunden.json"), "JSON-Bestände gehören in die Sicherung");
        assertTrue(eintraege.stream().noneMatch(name -> name.endsWith(".log")),
                "Logdateien gehören nicht in die Sicherung");
    }

    private static List<String> entpackeEintraege(Path zip) throws Exception {
        List<String> namen = new ArrayList<>();
        try (ZipInputStream strom = new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry eintrag = strom.getNextEntry(); eintrag != null;
                    eintrag = strom.getNextEntry()) {
                namen.add(eintrag.getName());
            }
        }
        return namen;
    }

    private Path entpackeDatenbank(Path zip) throws Exception {
        Path ziel = tempDir.resolve("entpackt.db");
        try (ZipInputStream strom = new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry eintrag = strom.getNextEntry(); eintrag != null;
                    eintrag = strom.getNextEntry()) {
                if ("faktura.db".equals(eintrag.getName())) {
                    Files.copy(strom, ziel);
                    return ziel;
                }
            }
        }
        throw new IllegalStateException("faktura.db fehlt in der Sicherung");
    }

    private static String integritaetspruefung(Path datenbank) throws SQLException {
        return abfrage(datenbank, "PRAGMA integrity_check");
    }

    private static int anzahlKunden(Path datenbank) throws SQLException {
        return Integer.parseInt(abfrage(datenbank, "SELECT COUNT(*) FROM kunde"));
    }

    private static String abfrage(Path datenbank, String sql) throws SQLException {
        try (Connection verbindung = DriverManager.getConnection("jdbc:sqlite:" + datenbank);
             Statement anweisung = verbindung.createStatement();
             ResultSet ergebnis = anweisung.executeQuery(sql)) {
            ergebnis.next();
            return ergebnis.getString(1);
        }
    }

    private ConfigurableApplicationContext neuerKontext() {
        return new SpringApplicationBuilder(FakturaApplication.class)
                .headless(true)
                .run("--faktura.daten-verzeichnis=" + tempDir);
    }
}
