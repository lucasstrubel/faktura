package de.lucasstrubel.faktura;

import de.lucasstrubel.faktura.dokumente.DokumentRepository;
import de.lucasstrubel.faktura.kunden.KundenRepository;
import de.lucasstrubel.faktura.produkte.ProduktRepository;

import org.flywaydb.core.Flyway;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verdrahtung der Persistenzschicht (IF-01): SQLite-Datenbank im lokalen
 * Datenverzeichnis (Flyway verwaltet das Schema) und einmalige Übernahme eines
 * vorhandenen JSON-Bestands. Die Nummernvergabe liegt seit Flyway V3 im
 * {@code nummernkreis} der Datenbank; die Generatoren sind eigenständige Beans
 * (GR-01).
 */
@Configuration
@EnableConfigurationProperties(FakturaEigenschaften.class)
public class PersistenzKonfiguration {

    /** Wartezeit auf eine gesperrte Datenbank, bevor SQLite abbricht. */
    private static final int SPERR_ZEITSCHRANKE_MS = 5000;

    /**
     * Einzelplatzbetrieb: einfache {@link SQLiteDataSource} ohne
     * Verbindungspool; Fremdschlüsselprüfung ist je Verbindung aktiviert.
     * Flyway migriert das Schema unmittelbar hier — damit ist es garantiert
     * vorhanden, bevor irgendein anderer Bean die Datenbank anspricht.
     *
     * <p>Der WAL-Journalmodus hält Lesevorgänge während eines Schreibvorgangs
     * offen (die Oberfläche liest jetzt aus Hintergrundaufgaben) und übersteht
     * einen Absturz ohne beschädigte Datei; {@code busy_timeout} lässt eine
     * kurzzeitig gesperrte Datenbank warten statt sofort zu scheitern.
     */
    @Bean
    public DataSource dataSource(FakturaEigenschaften eigenschaften) {
        Path verzeichnis = eigenschaften.datenVerzeichnis();
        try {
            Files.createDirectories(verzeichnis);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Datenverzeichnis konnte nicht angelegt werden: " + verzeichnis, e);
        }
        SQLiteConfig konfiguration = new SQLiteConfig();
        konfiguration.enforceForeignKeys(true);
        konfiguration.setJournalMode(SQLiteConfig.JournalMode.WAL);
        konfiguration.setBusyTimeout(SPERR_ZEITSCHRANKE_MS);
        SQLiteDataSource dataSource = new SQLiteDataSource(konfiguration);
        dataSource.setUrl("jdbc:sqlite:" + verzeichnis.resolve("faktura.db"));
        Flyway.configure().dataSource(dataSource).load().migrate();
        return dataSource;
    }

    /**
     * Übernimmt einen vorhandenen JSON-Bestand einmalig in die leere
     * Datenbank; die Generator-Beans hängen von dieser Bean ab, damit ihre
     * Zähler erst nach der Übernahme abgeleitet werden.
     */
    @Bean
    public JsonDatenUebernahme jsonDatenUebernahme(FakturaEigenschaften eigenschaften,
                                                   JdbcTemplate jdbc,
                                                   KundenRepository kundenRepository,
                                                   ProduktRepository produktRepository,
                                                   DokumentRepository dokumentRepository) {
        JsonDatenUebernahme uebernahme = new JsonDatenUebernahme(
                eigenschaften.datenVerzeichnis(), jdbc,
                kundenRepository, produktRepository, dokumentRepository);
        uebernahme.fuehreAusFallsNoetig();
        return uebernahme;
    }

    /*
     * Die Nummerngeneratoren sind seit der Einführung des Nummernkreises
     * (Flyway V3) eigenständige @Component-Beans und werden nicht mehr hier
     * verdrahtet: Ihre Zähler liegen in der Datenbank und werden beim ersten
     * Zugriff träge aus dem Bestand abgeleitet. Damit entfällt auch die
     * frühere künstliche Bean-Abhängigkeit auf die JSON-Übernahme — die
     * Ableitung findet ohnehin erst zur Laufzeit statt, also lange nach dem
     * Hochfahren des Containers.
     */
}
