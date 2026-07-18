package de.lucasstrubel.faktura.dokumente;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.lucasstrubel.faktura.gemeinsam.JsonPersistenz;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * JSON-Datei-Persistenz der Belege (IF-01). Die Polymorphie der Belegtypen
 * wird über das {@code typ}-Feld abgebildet (siehe {@link Dokument}).
 */
public class JsonDokumentRepository implements DokumentRepository {

    private final Path datei;
    private final ObjectMapper mapper = JsonPersistenz.mapper();
    private final List<Dokument> dokumente = new ArrayList<>();

    public JsonDokumentRepository(Path datei) {
        this.datei = datei;
        lade();
    }

    private void lade() {
        if (!Files.exists(datei) || datei.toFile().length() == 0) {
            return;
        }
        try {
            dokumente.addAll(mapper.readValue(datei.toFile(), new TypeReference<List<Dokument>>() { }));
        } catch (IOException e) {
            throw new UncheckedIOException("Belegbestand konnte nicht gelesen werden: " + datei, e);
        }
    }

    private void schreibe() {
        try {
            // Über den Basistyp schreiben, damit die polymorphe Typ-ID ('typ')
            // erhalten bleibt und Belege beim Neustart wieder geladen werden (IF-01).
            JsonPersistenz.schreibeAtomar(datei,
                    mapper.writerFor(new TypeReference<List<Dokument>>() { }), dokumente);
        } catch (IOException e) {
            throw new UncheckedIOException("Belegbestand konnte nicht gespeichert werden: " + datei, e);
        }
    }

    @Override
    public Dokument speichere(Dokument dokument) {
        dokumente.removeIf(d -> d.getBelegnummer().equals(dokument.getBelegnummer()));
        dokumente.add(dokument);
        schreibe();
        return dokument;
    }

    @Override
    public Dokument findeNachNummer(String belegnummer) {
        return dokumente.stream()
                .filter(d -> d.getBelegnummer().equals(belegnummer))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Dokument> alle() {
        return dokumente.stream()
                .sorted(Comparator.comparing(Dokument::getBelegnummer))
                .toList();
    }
}
