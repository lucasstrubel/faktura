package de.lucasstrubel.faktura;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Zeitmarken des Anwendungsstarts (Q-04, NF-PERF-01): Jede Phase wird in
 * Millisekunden seit dem Prozessstart festgehalten, damit die Startmessung
 * ({@code tools/paket-pruefen.ps1}) nicht nur die Gesamtzeit, sondern auch
 * ihre Aufteilung auf JVM, Spring-Kontext und Oberfläche ausweist.
 */
public final class Startphasen {

    /** Präfix der Protokollzeile, nach dem die Startmessung sucht. */
    static final String MARKER = "FAKTURA_STARTPHASEN";

    /** Zeitmarken des laufenden Prozesses. */
    static final Startphasen PROZESS = new Startphasen(prozessstart());

    private final long startMillis;
    private final Map<String, Long> marken = new LinkedHashMap<>();

    Startphasen(long startMillis) {
        this.startMillis = startMillis;
    }

    /** Hält das Ende der Phase {@code phase} zum aktuellen Zeitpunkt fest. */
    void markiere(String phase) {
        markiere(phase, System.currentTimeMillis());
    }

    /** Hält das Ende der Phase {@code phase} zum Zeitpunkt {@code zeitpunktMillis} fest. */
    synchronized void markiere(String phase, long zeitpunktMillis) {
        marken.put(phase, zeitpunktMillis - startMillis);
    }

    /** Protokollzeile, z. B. {@code FAKTURA_STARTPHASEN main=310 spring=2480 bereit=4120}. */
    synchronized String protokollzeile() {
        StringJoiner zeile = new StringJoiner(" ", MARKER + " ", "");
        marken.forEach((phase, millis) -> zeile.add(phase + "=" + millis));
        return zeile.toString();
    }

    /**
     * Prozessstart laut Betriebssystem, damit auch der Start der JVM selbst
     * mitzählt; ist er nicht ermittelbar, zählt ab dem Laden dieser Klasse.
     */
    private static long prozessstart() {
        return ProcessHandle.current().info().startInstant()
                .map(Instant::toEpochMilli)
                .orElseGet(System::currentTimeMillis);
    }
}
