package de.lucasstrubel.faktura.gemeinsam;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Zentral konfigurierter Jackson-ObjectMapper für die lokale
 * JSON-Persistenz (IF-01). Datumswerte werden als ISO-Strings
 * geschrieben (offenes, dokumentiertes Format, Q-08).
 */
public final class JsonPersistenz {

    private JsonPersistenz() {
    }

    public static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    /**
     * Schreibt den Bestand atomar: erst vollständig in eine temporäre Datei,
     * dann per Move ersetzen. Ein Absturz während des Schreibens kann so den
     * vorhandenen Bestand nicht korrumpieren (GR-01/GR-02: der Belegbestand
     * ist Grundlage der lückenlosen Nummernvergabe und Unveränderlichkeit).
     */
    public static void schreibeAtomar(Path datei, ObjectWriter writer, Object daten)
            throws IOException {
        if (datei.getParent() != null) {
            Files.createDirectories(datei.getParent());
        }
        Path temp = datei.resolveSibling(datei.getFileName() + ".tmp");
        writer.writeValue(temp.toFile(), daten);
        try {
            Files.move(temp, datei, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, datei, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
