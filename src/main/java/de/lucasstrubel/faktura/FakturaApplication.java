package de.lucasstrubel.faktura;

import javafx.application.Application;

import org.springframework.boot.autoconfigure.SpringBootApplication;

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
        Application.launch(FxAnwendung.class, args);
    }
}
