package de.lucasstrubel.faktura.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Lädt FXML-Ansichten und lässt ihre Controller vom Spring-Container
 * erzeugen und injizieren (Controller-Factory): Die Controller sind damit
 * keine registrierten Beans, erhalten aber volle Konstruktor-Injektion —
 * Ansichten existieren so nur, wenn die Oberfläche tatsächlich läuft.
 */
@Component
public class FxmlLader {

    private final ApplicationContext kontext;

    public FxmlLader(ApplicationContext kontext) {
        this.kontext = kontext;
    }

    /** Lädt {@code /fxml/<name>.fxml} und liefert die Wurzel der Ansicht. */
    public Parent lade(String name) {
        FXMLLoader lader = new FXMLLoader(getClass().getResource("/fxml/" + name + ".fxml"));
        lader.setControllerFactory(typ ->
                kontext.getAutowireCapableBeanFactory().createBean(typ));
        try {
            return lader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("FXML-Ansicht konnte nicht geladen werden: " + name, e);
        }
    }
}
