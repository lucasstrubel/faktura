package de.lucasstrubel.faktura.gemeinsam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonPersistenzTest {

    private final ObjectMapper mapper = JsonPersistenz.mapper();

    @Test
    @DisplayName("INF-04: schreibeAtomar legt die Zieldatei an und lässt keine Temp-Datei zurück")
    void schreibtOhneTempDateiRueckstand(@TempDir Path verzeichnis) throws IOException {
        Path datei = verzeichnis.resolve("bestand.json");

        JsonPersistenz.schreibeAtomar(datei, mapper.writer(), List.of("a", "b"));

        assertTrue(Files.exists(datei));
        assertFalse(Files.exists(verzeichnis.resolve("bestand.json.tmp")),
                "Temp-Datei darf nach dem Move nicht zurückbleiben");
        List<?> gelesen = mapper.readValue(datei.toFile(), List.class);
        assertEquals(List.of("a", "b"), gelesen);
    }

    @Test
    @DisplayName("INF-05: schreibeAtomar ersetzt einen vorhandenen Bestand vollständig")
    void ersetztVorhandenenBestand(@TempDir Path verzeichnis) throws IOException {
        Path datei = verzeichnis.resolve("bestand.json");
        JsonPersistenz.schreibeAtomar(datei, mapper.writer(), List.of("alt"));

        JsonPersistenz.schreibeAtomar(datei, mapper.writer(), List.of("neu1", "neu2"));

        List<?> gelesen = mapper.readValue(datei.toFile(), List.class);
        assertEquals(List.of("neu1", "neu2"), gelesen);
    }

    @Test
    @DisplayName("INF-06: schreibeAtomar legt fehlende Elternverzeichnisse an")
    void legtElternverzeichnisseAn(@TempDir Path verzeichnis) throws IOException {
        Path datei = verzeichnis.resolve("unterordner").resolve("bestand.json");

        JsonPersistenz.schreibeAtomar(datei, mapper.writer(), List.of("a"));

        assertTrue(Files.exists(datei));
    }
}
