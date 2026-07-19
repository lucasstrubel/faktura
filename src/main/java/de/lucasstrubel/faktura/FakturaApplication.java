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

    public static void main(String[] args) {
        setzeDatenverzeichnisFuerInstallierteAnwendung();
        Application.launch(FxAnwendung.class, args);
    }

    /**
     * Die installierte Anwendung (jpackage setzt {@code jpackage.app-path})
     * darf nicht in ihr Programmverzeichnis schreiben; ohne ausdrückliche
     * Konfiguration liegen die Daten dann unter {@code <Benutzer>/Faktura/daten}.
     * Muss vor dem ersten Logger-Zugriff laufen, da auch Logback die
     * Property auswertet.
     */
    private static void setzeDatenverzeichnisFuerInstallierteAnwendung() {
        if (System.getProperty("jpackage.app-path") != null
                && System.getProperty("faktura.daten-verzeichnis") == null) {
            System.setProperty("faktura.daten-verzeichnis",
                    Path.of(System.getProperty("user.home"), "Faktura", "daten").toString());
        }
    }
}
