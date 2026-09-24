package de.lucasstrubel.faktura;

import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentRepository;
import de.lucasstrubel.faktura.dokumente.JsonDokumentRepository;
import de.lucasstrubel.faktura.kunden.JsonKundenRepository;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenRepository;
import de.lucasstrubel.faktura.produkte.JsonProduktRepository;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Einmalige Übernahme des bisherigen JSON-Bestands (kunden.json,
 * produkte.json, dokumente.json) in die SQLite-Datenbank: Sie läuft nur,
 * wenn die Datenbank vollständig leer ist — auch ohne je vergebene Nummer —
 * und mindestens eine JSON-Datei existiert.
 *
 * <p>Die Übernahme ist alles oder nichts: Erst werden alle drei Dateien
 * gelesen, dann wird in <em>einer</em> Transaktion geschrieben. Scheitert
 * etwa der Beleg-Import, bleibt die Datenbank leer und der nächste Start
 * versucht es erneut — statt mit halb übernommenem Bestand weiterzulaufen
 * und die Belege stillschweigend nie zu übernehmen.
 *
 * <p>Nach Erfolg werden die Dateien in {@code *.uebernommen} umbenannt. Sie
 * bleiben als Sicherung erhalten, werden aber nicht erneut eingelesen, falls
 * die Datenbank später verloren geht: Ein erneuter Import des alten Stands
 * würde die Nummernkreise zurücksetzen und bereits vergebene
 * Rechnungsnummern wiederverwenden (GR-01).
 *
 * <p>Die Nummerngeneratoren leiten ihre Zähler erst beim ersten Zugriff aus
 * dem Bestand ab, also nach dieser Übernahme (GR-01).
 */
public class JsonDatenUebernahme {

    private static final Logger LOG = LoggerFactory.getLogger(JsonDatenUebernahme.class);

    /** Endung der übernommenen JSON-Dateien. */
    public static final String ENDUNG_UEBERNOMMEN = ".uebernommen";

    private final Path datenVerzeichnis;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaktion;
    private final KundenRepository kundenRepository;
    private final ProduktRepository produktRepository;
    private final DokumentRepository dokumentRepository;

    public JsonDatenUebernahme(Path datenVerzeichnis, JdbcTemplate jdbc,
                               PlatformTransactionManager transaktionsManager,
                               KundenRepository kundenRepository,
                               ProduktRepository produktRepository,
                               DokumentRepository dokumentRepository) {
        this.datenVerzeichnis = datenVerzeichnis;
        this.jdbc = jdbc;
        this.transaktion = new TransactionTemplate(transaktionsManager);
        this.kundenRepository = kundenRepository;
        this.produktRepository = produktRepository;
        this.dokumentRepository = dokumentRepository;
    }

    public void fuehreAusFallsNoetig() {
        Path kundenJson = datenVerzeichnis.resolve("kunden.json");
        Path produkteJson = datenVerzeichnis.resolve("produkte.json");
        Path dokumenteJson = datenVerzeichnis.resolve("dokumente.json");
        boolean jsonVorhanden = Files.exists(kundenJson) || Files.exists(produkteJson)
                || Files.exists(dokumenteJson);
        if (!jsonVorhanden || !datenbankIstLeer()) {
            return;
        }

        // Erst vollständig lesen: Ein Lesefehler bricht ab, bevor geschrieben wird
        List<Kunde> kunden = Files.exists(kundenJson)
                ? new JsonKundenRepository(kundenJson).alleSortiertNachName() : List.of();
        List<Produkt> produkte = Files.exists(produkteJson)
                ? new JsonProduktRepository(produkteJson).alleSortiertNachBezeichnung() : List.of();
        List<Dokument> dokumente = Files.exists(dokumenteJson)
                ? new JsonDokumentRepository(dokumenteJson).alle() : List.of();

        transaktion.executeWithoutResult(status -> {
            kunden.forEach(kundenRepository::speichere);
            produkte.forEach(produktRepository::speichere);
            dokumente.forEach(dokumentRepository::speichere);
        });

        for (Path datei : List.of(kundenJson, produkteJson, dokumenteJson)) {
            markiereAlsUebernommen(datei);
        }
        LOG.info("JSON-Bestand in SQLite übernommen: {} Kunden, {} Produkte, {} Belege"
                        + " (Dateien als *{} aufbewahrt)",
                kunden.size(), produkte.size(), dokumente.size(), ENDUNG_UEBERNOMMEN);
    }

    private void markiereAlsUebernommen(Path datei) {
        if (!Files.exists(datei)) {
            return;
        }
        Path ziel = datei.resolveSibling(datei.getFileName() + ENDUNG_UEBERNOMMEN);
        try {
            Files.move(datei, ziel, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Die Daten sind übernommen; die Datenbank ist nicht mehr leer, ein
            // erneuter Import findet also ohnehin nicht statt
            LOG.warn("JSON-Datei {} konnte nicht umbenannt werden", datei, e);
        }
    }

    private boolean datenbankIstLeer() {
        Integer summe = jdbc.queryForObject(
                "SELECT (SELECT COUNT(*) FROM kunde) + (SELECT COUNT(*) FROM produkt)"
                        + " + (SELECT COUNT(*) FROM dokument) + (SELECT COUNT(*) FROM nummernkreis)",
                Integer.class);
        return summe == null || summe == 0;
    }
}
