package de.lucasstrubel.faktura;

import de.lucasstrubel.faktura.gui.FxmlLader;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

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
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Scene szene = new Scene(kontext.getBean(FxmlLader.class).lade("haupt_ansicht"), 1080, 680);
        szene.getStylesheets().add(getClass().getResource("/css/faktura.css").toExternalForm());
        buehne.getIcons().addAll(
                new Image(getClass().getResourceAsStream("/icon/faktura.png")),
                new Image(getClass().getResourceAsStream("/icon/faktura-256.png")));
        buehne.setTitle("Faktura");
        buehne.setScene(szene);
        buehne.show();
        LOG.info("Faktura ist bedienbereit (Q-04)");
    }

    @Override
    public void stop() {
        kontext.close();
    }
}
