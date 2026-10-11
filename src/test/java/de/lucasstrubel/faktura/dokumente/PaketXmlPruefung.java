package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.firma.Firmenprofil;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.sqlite.SQLiteDataSource;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ERE-PAKET-01: Offline-Schemas und Schematron aus dem unveränderten Release-JAR;
 * zusätzlich findet Flyway dort alle Migrationen, auch die Java-Migration V6.
 */
public final class PaketXmlPruefung {
    private PaketXmlPruefung() { }

    public static void main(String[] argumente) throws Exception {
        Path ziel = Path.of(argumente[0]);
        Files.createDirectories(ziel.toAbsolutePath().getParent());
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer("R-2026-000001");
        rechnung.setDatum(LocalDate.of(2026, 10, 10));
        rechnung.setLeistungsdatum(LocalDate.of(2026, 10, 9));
        rechnung.setZahlungsziel(LocalDate.of(2026, 10, 24));
        rechnung.setzeKunde("K-000001", "Paketprüfung GmbH", "Testweg 1, 68163 Mannheim");
        rechnung.setzeAussteller(new Firmenprofil("Prüffirma", "Testweg 2", "68163", "Mannheim",
                "DE123456789", "37/123/45678", null, null,
                "DE02 1203 0000 0000 2020 51", "BYLADEM1001", "Testbank"));
        rechnung.setzePositionen(List.of(new Dokumentposition("P-000001", "Beratung", 2,
                new BigDecimal("80.00"), new BigDecimal("0.19"), "Stunde", "HUR")));
        ERechnungExport export = new ERechnungExport(null);
        export.exportiereXml(rechnung, ziel);
        byte[] vorher = Files.readAllBytes(ziel);
        rechnung.setzeKunde("K-000001", "", "Testweg 1, 68163 Mannheim");
        try {
            export.exportiereXml(rechnung, ziel);
            throw new AssertionError("Schematron hat den fehlenden Käufernamen nicht abgelehnt");
        } catch (ValidierungsException erwartet) {
            if (!Arrays.equals(vorher, Files.readAllBytes(ziel))) {
                throw new AssertionError("Ungültiger Export hat die Zieldatei verändert");
            }
        }
        pruefeMigrationen(ziel.toAbsolutePath().getParent().resolve("migrationspruefung.db"));
        System.out.println("FAKTURA_XML_VALIDATED: Schema, Schematron, Dateischutz und Migrationen im Release-JAR geprüft");
    }

    /**
     * Java-Migrationen liegen im Fat-JAR unter BOOT-INF/classes. Fände Flyway
     * sie dort nicht, liefe die Anwendung ohne Fehler an — erst eine spätere
     * Migration scheiterte an der Validierung. Deshalb hier ausdrücklich prüfen.
     */
    private static void pruefeMigrationen(Path datenbank) throws Exception {
        Files.deleteIfExists(datenbank);
        SQLiteDataSource quelle = new SQLiteDataSource();
        quelle.setUrl("jdbc:sqlite:" + datenbank);
        Set<String> versionen = Arrays.stream(Flyway.configure().dataSource(quelle).load().info().all())
                .map(MigrationInfo::getVersion)
                .map(Object::toString)
                .collect(Collectors.toSet());
        for (String erwartet : List.of("1", "2", "3", "4", "5", "6")) {
            if (!versionen.contains(erwartet)) {
                throw new AssertionError("Migration V" + erwartet + " fehlt im Release-JAR; gefunden: " + versionen);
            }
        }
    }
}
