package de.lucasstrubel.faktura.kunden;

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
 * JSON-Datei-Persistenz der Kunden (IF-01). Der Bestand wird vollständig
 * im Speicher gehalten (Q-01/Q-02: bis 5.000 Kunden, Suche ≤ 1 s) und bei
 * jeder Änderung in die Datei geschrieben.
 */
public class JsonKundenRepository implements KundenRepository {

    private final Path datei;
    private final ObjectMapper mapper = JsonPersistenz.mapper();
    private final List<Kunde> kunden = new ArrayList<>();

    public JsonKundenRepository(Path datei) {
        this.datei = datei;
        lade();
    }

    private void lade() {
        if (!Files.exists(datei)) {
            return;
        }
        try {
            kunden.addAll(mapper.readValue(datei.toFile(), new TypeReference<List<Kunde>>() { }));
        } catch (IOException e) {
            throw new UncheckedIOException("Kundenbestand konnte nicht gelesen werden: " + datei, e);
        }
    }

    private void schreibe() {
        try {
            JsonPersistenz.schreibeAtomar(datei, mapper.writer(), kunden);
        } catch (IOException e) {
            throw new UncheckedIOException("Kundenbestand konnte nicht gespeichert werden: " + datei, e);
        }
    }

    @Override
    public Kunde speichere(Kunde kunde) {
        kunden.removeIf(k -> k.getKundennummer().equals(kunde.getKundennummer()));
        kunden.add(kunde);
        schreibe();
        return kunde;
    }

    @Override
    public void loesche(String kundennummer) {
        kunden.removeIf(k -> k.getKundennummer().equals(kundennummer));
        schreibe();
    }

    @Override
    public Kunde findeNachNummer(String kundennummer) {
        return kunden.stream()
                .filter(k -> k.getKundennummer().equals(kundennummer))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Kunde> alleSortiertNachName() {
        return kunden.stream()
                .sorted(Comparator.comparing(Kunde::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<Kunde> suche(String suchbegriff) {
        String begriff = suchbegriff == null ? "" : suchbegriff.toLowerCase(Locale.ROOT);
        return kunden.stream()
                .filter(k -> k.getName().toLowerCase(Locale.ROOT).contains(begriff)
                        || k.getKundennummer().toLowerCase(Locale.ROOT).contains(begriff))
                .sorted(Comparator.comparing(Kunde::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
