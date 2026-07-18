package de.lucasstrubel.faktura.produkte;

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
import java.util.Locale;

/**
 * JSON-Datei-Persistenz der Produkte (IF-01). Der Bestand wird vollständig
 * im Speicher gehalten (Q-01/Q-02: bis 5.000 Produkte, Suche ≤ 1 s) und bei
 * jeder Änderung in die Datei geschrieben.
 */
public class JsonProduktRepository implements ProduktRepository {

    private final Path datei;
    private final ObjectMapper mapper = JsonPersistenz.mapper();
    private final List<Produkt> produkte = new ArrayList<>();

    public JsonProduktRepository(Path datei) {
        this.datei = datei;
        lade();
    }

    private void lade() {
        if (!Files.exists(datei)) {
            return;
        }
        try {
            produkte.addAll(mapper.readValue(datei.toFile(), new TypeReference<List<Produkt>>() { }));
        } catch (IOException e) {
            throw new UncheckedIOException("Produktbestand konnte nicht gelesen werden: " + datei, e);
        }
    }

    private void schreibe() {
        try {
            JsonPersistenz.schreibeAtomar(datei, mapper.writer(), produkte);
        } catch (IOException e) {
            throw new UncheckedIOException("Produktbestand konnte nicht gespeichert werden: " + datei, e);
        }
    }

    @Override
    public Produkt speichere(Produkt produkt) {
        produkte.removeIf(p -> p.getProduktnummer().equals(produkt.getProduktnummer()));
        produkte.add(produkt);
        schreibe();
        return produkt;
    }

    @Override
    public void loesche(String produktnummer) {
        produkte.removeIf(p -> p.getProduktnummer().equals(produktnummer));
        schreibe();
    }

    @Override
    public Produkt findeNachNummer(String produktnummer) {
        return produkte.stream()
                .filter(p -> p.getProduktnummer().equals(produktnummer))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Produkt> alleSortiertNachBezeichnung() {
        return produkte.stream()
                .sorted(Comparator.comparing(Produkt::getBezeichnung, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<Produkt> suche(String suchbegriff) {
        String begriff = suchbegriff == null ? "" : suchbegriff.toLowerCase(Locale.ROOT);
        return produkte.stream()
                .filter(p -> p.getBezeichnung().toLowerCase(Locale.ROOT).contains(begriff)
                        || p.getProduktnummer().toLowerCase(Locale.ROOT).contains(begriff))
                .sorted(Comparator.comparing(Produkt::getBezeichnung, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
