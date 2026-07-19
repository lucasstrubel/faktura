package de.lucasstrubel.faktura;

import de.lucasstrubel.faktura.dokumente.BelegnummernGenerator;
import de.lucasstrubel.faktura.dokumente.DokumentRepository;
import de.lucasstrubel.faktura.dokumente.EinfacherBelegnummernGenerator;
import de.lucasstrubel.faktura.kunden.EinfacherKundennummernGenerator;
import de.lucasstrubel.faktura.kunden.KundenRepository;
import de.lucasstrubel.faktura.kunden.KundennummernGenerator;
import de.lucasstrubel.faktura.produkte.EinfacherProduktnummernGenerator;
import de.lucasstrubel.faktura.produkte.ProduktRepository;
import de.lucasstrubel.faktura.produkte.ProduktnummernGenerator;

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
 * Datenverzeichnis (Flyway verwaltet das Schema), einmalige Übernahme eines
 * vorhandenen JSON-Bestands sowie die Nummerngeneratoren, deren Zähler nach
 * der Übernahme aus dem Bestand abgeleitet werden (GR-01).
 */
@Configuration
@EnableConfigurationProperties(FakturaEigenschaften.class)
public class PersistenzKonfiguration {

    /**
     * Einzelplatzbetrieb: einfache {@link SQLiteDataSource} ohne
     * Verbindungspool; Fremdschlüsselprüfung ist je Verbindung aktiviert.
     * Flyway migriert das Schema unmittelbar hier — damit ist es garantiert
     * vorhanden, bevor irgendein anderer Bean die Datenbank anspricht.
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

    @Bean
    public KundennummernGenerator kundennummernGenerator(KundenRepository repository,
                                                         JsonDatenUebernahme uebernahme) {
        return EinfacherKundennummernGenerator.ausRepository(repository);
    }

    @Bean
    public ProduktnummernGenerator produktnummernGenerator(ProduktRepository repository,
                                                           JsonDatenUebernahme uebernahme) {
        return EinfacherProduktnummernGenerator.ausRepository(repository);
    }

    @Bean
    public BelegnummernGenerator belegnummernGenerator(DokumentRepository repository,
                                                       JsonDatenUebernahme uebernahme) {
        return EinfacherBelegnummernGenerator.ausRepository(repository);
    }
}
