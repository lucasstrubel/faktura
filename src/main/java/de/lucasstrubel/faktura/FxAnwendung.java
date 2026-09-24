package de.lucasstrubel.faktura;

import de.lucasstrubel.faktura.gui.Erscheinungsbild;
import de.lucasstrubel.faktura.gui.FxMeldung;
import de.lucasstrubel.faktura.gui.FxmlLader;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.InputStream;
import java.net.URL;

/**
 * JavaFX-Einstieg (Komponente D): {@code init()} startet den
 * Spring-Container, {@code start()} baut die Oberfläche auf dem
 * FX-Application-Thread aus den Beans auf, {@code stop()} fährt den
 * Container mit dem letzten Fenster herunter.
 */
public class FxAnwendung extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(FxAnwendung.class);

    private ConfigurableApplicationContext kontext;

    @Override
    public void init() {
        kontext = new SpringApplicationBuilder(FakturaApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
        LOG.info("Faktura startet; Datenverzeichnis: {}",
                kontext.getBean(FakturaEigenschaften.class).datenVerzeichnis().toAbsolutePath());
    }

    @Override
    public void start(Stage buehne) {
        setzeFehlerbehandlung();
        // Gespeichertes Erscheinungsbild anwenden, bevor die Szene entsteht
        Erscheinungsbild erscheinungsbild = kontext.getBean(Erscheinungsbild.class);
        erscheinungsbild.wendeAn();

        Scene szene = new Scene(kontext.getBean(FxmlLader.class).lade("haupt_ansicht"));
        szene.getStylesheets().add(ressource("/css/faktura.css").toExternalForm());
        buehne.getIcons().addAll(
                new Image(ressourcenStrom("/icon/faktura.png")),
                new Image(ressourcenStrom("/icon/faktura-256.png")));
        buehne.setTitle("Faktura");
        buehne.setScene(szene);
        // Größe und Position aus der letzten Sitzung wiederherstellen
        erscheinungsbild.bindeFenster(buehne);
        buehne.show();
        LOG.info("Faktura ist bedienbereit (Q-04)");
    }

    /**
     * Letztes Netz für Ausnahmen außerhalb der bekannten Pfade: Ohne diesen
     * Handler scheitert alles, was nicht durch
     * {@code FxMeldung.mitFehlerbehandlung} oder eine Hintergrundaufgabe
     * läuft, stillschweigend — die Oberfläche wirkt dann einfach kaputt.
     */
    private static void setzeFehlerbehandlung() {
        Thread.setDefaultUncaughtExceptionHandler((faden, fehler) -> {
            LOG.error("Unbehandelter Fehler im Faden {}", faden.getName(), fehler);
            // Immer nachgelagert anzeigen: Ein showAndWait() mitten in einem
            // Layout- oder Animationsdurchlauf würde selbst scheitern
            javafx.application.Platform.runLater(
                    () -> FxMeldung.zeige(FxMeldung.zuMeldung(fehler), null));
        });
    }

    /**
     * Lädt eine Ressource aus dem Klassenpfad und scheitert mit klarer
     * Meldung, statt später mit einer nichtssagenden
     * {@code NullPointerException} umzufallen.
     */
    private URL ressource(String pfad) {
        URL gefunden = getClass().getResource(pfad);
        if (gefunden == null) {
            throw new IllegalStateException(
                    "Ressource fehlt im Klassenpfad (fehlerhafter Build?): " + pfad);
        }
        return gefunden;
    }

    private InputStream ressourcenStrom(String pfad) {
        InputStream strom = getClass().getResourceAsStream(pfad);
        if (strom == null) {
            throw new IllegalStateException(
                    "Ressource fehlt im Klassenpfad (fehlerhafter Build?): " + pfad);
        }
        return strom;
    }

    /**
     * Schließt den Container; dessen {@code AutoCloseable}-Beans räumen dabei
     * auf — die Hintergrundausführung wird beendet und das Sitzungsverzeichnis
     * mit den Zwischen-PDFs gelöscht (Q-06).
     */
    @Override
    public void stop() {
        kontext.close();
        LOG.info("Faktura wurde beendet");
    }
}
