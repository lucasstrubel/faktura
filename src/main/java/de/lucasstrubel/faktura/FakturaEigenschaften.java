package de.lucasstrubel.faktura;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Anwendungskonfiguration aus {@code application.yml} (Präfix {@code faktura}).
 * Alle Daten liegen ausschließlich lokal unterhalb des Datenverzeichnisses
 * (Q-06, IF-01).
 *
 * @param datenVerzeichnis Wurzelverzeichnis der lokalen Datenhaltung
 */
@ConfigurationProperties(prefix = "faktura")
public record FakturaEigenschaften(Path datenVerzeichnis) {
}
