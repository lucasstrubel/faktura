package de.lucasstrubel.faktura.gemeinsam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Veröffentlicht nur vollständig erzeugte Dateien (Q-06): Der Inhalt entsteht
 * in einer temporären Datei im Zielverzeichnis und ersetzt das Ziel erst nach
 * Erfolg per atomarem Verschieben. Es gibt bewusst keinen unsicheren
 * Ersatzpfad — unterstützt das Dateisystem kein atomares Verschieben, scheitert
 * der Vorgang und das bisherige Ziel bleibt unverändert.
 */
public final class AtomareDatei {

    private static final Logger LOG = LoggerFactory.getLogger(AtomareDatei.class);

    private AtomareDatei() {
    }

    @FunctionalInterface
    public interface Schreiber {
        void schreibe(Path temporaer) throws IOException;
    }

    public static void schreibe(Path ziel, Schreiber schreiber) throws IOException {
        Path absolut = ziel.toAbsolutePath().normalize();
        Path eltern = absolut.getParent();
        if (eltern == null) {
            throw new IOException("Das Dateiziel benötigt ein Elternverzeichnis.");
        }
        Files.createDirectories(eltern);
        Path temporaer = Files.createTempFile(eltern, ".faktura-", ".tmp");
        try {
            schreiber.schreibe(temporaer);
            Files.move(temporaer, absolut, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaer);
        }
    }

    /**
     * Entfernt ein flaches temporäres Arbeitsverzeichnis. Scheitert das (etwa
     * weil Windows eine Datei noch sperrt), bleibt nur Temp-Müll zurück — das
     * darf weder den Programmstart verhindern noch einen eigentlichen Fehler
     * überdecken, deshalb nur eine Warnung.
     */
    public static void entferneTemporaerVerzeichnis(Path verzeichnis) {
        try {
            try (var dateien = Files.list(verzeichnis)) {
                for (Path datei : dateien.toList()) {
                    Files.deleteIfExists(datei);
                }
            }
            Files.deleteIfExists(verzeichnis);
        } catch (IOException | UncheckedIOException e) {
            LOG.warn("Temporäres Verzeichnis konnte nicht entfernt werden: {}", verzeichnis, e);
        }
    }
}
