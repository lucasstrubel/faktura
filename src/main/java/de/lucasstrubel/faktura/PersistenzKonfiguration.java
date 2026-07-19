package de.lucasstrubel.faktura;

import de.lucasstrubel.faktura.dokumente.BelegnummernGenerator;
import de.lucasstrubel.faktura.dokumente.EinfacherBelegnummernGenerator;
import de.lucasstrubel.faktura.dokumente.JsonDokumentRepository;
import de.lucasstrubel.faktura.kunden.EinfacherKundennummernGenerator;
import de.lucasstrubel.faktura.kunden.JsonKundenRepository;
import de.lucasstrubel.faktura.kunden.KundenRepository;
import de.lucasstrubel.faktura.kunden.KundennummernGenerator;
import de.lucasstrubel.faktura.produkte.EinfacherProduktnummernGenerator;
import de.lucasstrubel.faktura.produkte.JsonProduktRepository;
import de.lucasstrubel.faktura.produkte.ProduktRepository;
import de.lucasstrubel.faktura.produkte.ProduktnummernGenerator;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Verdrahtung der Persistenzschicht (IF-01): JSON-Repositories im lokalen
 * Datenverzeichnis sowie die Nummerngeneratoren, deren Zähler beim Start aus
 * dem Bestand abgeleitet werden (GR-01).
 */
@Configuration
@EnableConfigurationProperties(FakturaEigenschaften.class)
public class PersistenzKonfiguration {

    @Bean
    public JsonKundenRepository kundenRepository(FakturaEigenschaften eigenschaften) {
        return new JsonKundenRepository(eigenschaften.datenVerzeichnis().resolve("kunden.json"));
    }

    @Bean
    public JsonProduktRepository produktRepository(FakturaEigenschaften eigenschaften) {
        return new JsonProduktRepository(eigenschaften.datenVerzeichnis().resolve("produkte.json"));
    }

    @Bean
    public JsonDokumentRepository dokumentRepository(FakturaEigenschaften eigenschaften) {
        return new JsonDokumentRepository(eigenschaften.datenVerzeichnis().resolve("dokumente.json"));
    }

    @Bean
    public KundennummernGenerator kundennummernGenerator(KundenRepository repository) {
        return EinfacherKundennummernGenerator.ausRepository(repository);
    }

    @Bean
    public ProduktnummernGenerator produktnummernGenerator(ProduktRepository repository) {
        return EinfacherProduktnummernGenerator.ausRepository(repository);
    }

    @Bean
    public BelegnummernGenerator belegnummernGenerator(JsonDokumentRepository repository) {
        return EinfacherBelegnummernGenerator.ausRepository(repository);
    }
}
