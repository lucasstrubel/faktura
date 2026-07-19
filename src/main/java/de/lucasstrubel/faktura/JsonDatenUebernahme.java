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

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Einmalige Übernahme des bisherigen JSON-Bestands (kunden.json,
 * produkte.json, dokumente.json) in die SQLite-Datenbank: Sie läuft nur,
 * wenn die Datenbank vollständig leer ist und mindestens eine JSON-Datei
 * existiert. Die JSON-Dateien bleiben unverändert als Sicherung liegen.
 *
 * <p>Die Nummerngeneratoren werden erst nach dieser Übernahme initialisiert
 * (Bean-Abhängigkeit in {@code PersistenzKonfiguration}), damit ihre Zähler
 * aus dem übernommenen Bestand abgeleitet werden (GR-01).
 */
public class JsonDatenUebernahme {

    private static final Logger LOG = LoggerFactory.getLogger(JsonDatenUebernahme.class);

    private final Path datenVerzeichnis;
    private final JdbcTemplate jdbc;
    private final KundenRepository kundenRepository;
    private final ProduktRepository produktRepository;
    private final DokumentRepository dokumentRepository;

    public JsonDatenUebernahme(Path datenVerzeichnis, JdbcTemplate jdbc,
                               KundenRepository kundenRepository,
                               ProduktRepository produktRepository,
                               DokumentRepository dokumentRepository) {
        this.datenVerzeichnis = datenVerzeichnis;
        this.jdbc = jdbc;
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

        int kunden = 0;
        int produkte = 0;
        int dokumente = 0;
        if (Files.exists(kundenJson)) {
            for (Kunde kunde : new JsonKundenRepository(kundenJson).alleSortiertNachName()) {
                kundenRepository.speichere(kunde);
                kunden++;
            }
        }
        if (Files.exists(produkteJson)) {
            for (Produkt produkt : new JsonProduktRepository(produkteJson)
                    .alleSortiertNachBezeichnung()) {
                produktRepository.speichere(produkt);
                produkte++;
            }
        }
        if (Files.exists(dokumenteJson)) {
            for (Dokument dokument : new JsonDokumentRepository(dokumenteJson).alle()) {
                dokumentRepository.speichere(dokument);
                dokumente++;
            }
        }
        LOG.info("JSON-Bestand in SQLite übernommen: {} Kunden, {} Produkte, {} Belege"
                + " (JSON-Dateien bleiben als Sicherung erhalten)", kunden, produkte, dokumente);
    }

    private boolean datenbankIstLeer() {
        Integer summe = jdbc.queryForObject(
                "SELECT (SELECT COUNT(*) FROM kunde) + (SELECT COUNT(*) FROM produkt)"
                        + " + (SELECT COUNT(*) FROM dokument)", Integer.class);
        return summe == null || summe == 0;
    }
}
