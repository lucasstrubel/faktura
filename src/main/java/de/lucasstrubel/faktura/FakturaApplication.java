package de.lucasstrubel.faktura;

import javafx.application.Application;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;

/**
 * Einstiegspunkt der Desktop-Fakturierungsanwendung: startet die
 * JavaFX-Laufzeit ({@link FxAnwendung}), die ihrerseits den
 * Spring-IoC-Container hochfährt und die vier Komponenten
 * (A: Dokumentenzyklus, B: Produkte, C: Kunden, D: Oberfläche) verdrahtet.
 * Alle Daten liegen ausschließlich lokal im konfigurierten
 * Datenverzeichnis (Q-06, IF-01).
 */
@SpringBootApplication
public class FakturaApplication {

    /** Name der Eigenschaft für das Datenverzeichnis (application.yml, Startargument). */
    static final String DATENVERZEICHNIS = "faktura.daten-verzeichnis";

    public static void main(String[] args) {
        setzeDatenverzeichnis(args);
        Application.launch(FxAnwendung.class, args);
    }

    /**
     * Legt das Datenverzeichnis als System-Property fest, bevor der erste
     * Logger entsteht — Logback wertet die Property für den Ort der Logdatei
     * aus, sieht aber keine Spring-Startargumente. Vorrang: Startargument
     * {@code --faktura.daten-verzeichnis=…}, dann eine bereits gesetzte
     * System-Property, sonst {@code <Benutzer>/Faktura/daten} (wie in
     * {@code application.yml}). Die installierte Anwendung schreibt damit
     * nie in ihr Programmverzeichnis.
     */
    static void setzeDatenverzeichnis(String[] args) {
        String praefix = "--" + DATENVERZEICHNIS + "=";
        for (String arg : args) {
            if (arg.startsWith(praefix)) {
                System.setProperty(DATENVERZEICHNIS, arg.substring(praefix.length()));
                return;
            }
        }
        if (System.getProperty(DATENVERZEICHNIS) == null) {
            System.setProperty(DATENVERZEICHNIS,
                    Path.of(System.getProperty("user.home"), "Faktura", "daten").toString());
        }
    }
}
