package de.lucasstrubel.faktura.gui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Nicht blockierende Erfolgsmeldung („Toast“) am oberen Rand des Fensters
 * (D-F-17).
 *
 * <p>Zuvor bestätigte die Anwendung <em>jede</em> erfolgreiche Aktion mit
 * einem modalen Dialog, den der Anwender wegklicken musste — bei einem
 * Arbeitsablauf aus mehreren Schritten wird das schnell lästig und lässt die
 * Anwendung zäh wirken. Erfolge erscheinen jetzt kurz und verschwinden von
 * selbst; Fehler und Rückfragen bleiben bewusst modal, weil sie eine
 * Entscheidung verlangen.
 */
public final class Benachrichtigung {

    /** Standzeit, bevor die Meldung ausblendet. */
    private static final Duration STANDZEIT = Duration.seconds(3.5);

    private static final Duration EINBLENDEN = Duration.millis(180);
    private static final Duration AUSBLENDEN = Duration.millis(400);

    /** Ebene über dem Inhalt, in der die Meldungen erscheinen. */
    private static StackPane ebene;

    private Benachrichtigung() {
    }

    /**
     * Hinterlegt die Ebene, in der Meldungen erscheinen; setzt die
     * Hauptansicht beim Aufbau der Oberfläche.
     */
    static void setzeEbene(StackPane benachrichtigungsEbene) {
        ebene = benachrichtigungsEbene;
    }

    /** {@code true}, wenn eine Ebene bereitsteht (in Tests ohne Oberfläche nicht). */
    static boolean istBereit() {
        return ebene != null;
    }

    /** Zeigt den Text kurz an und blendet ihn anschließend aus. */
    static void zeige(String text) {
        if (ebene == null) {
            return;
        }
        HBox meldung = new HBox(new FontIcon(Feather.CHECK_CIRCLE), textLabel(text));
        meldung.getStyleClass().add("benachrichtigung");
        meldung.setAlignment(Pos.CENTER_LEFT);
        // Ein StackPane zieht seine Kinder sonst auf die volle Fläche auf: Die
        // Meldung füllte damit das halbe Fenster. Die Größenbegrenzung gehört
        // hierher und nicht ins Stylesheet -- -fx-max-* greift bei einer
        // gestreckten HBox nicht zuverlässig.
        meldung.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        // Nur der Kasten selbst reagiert auf die Maus, die Ebene bleibt durchlässig
        meldung.setMouseTransparent(true);

        ebene.getChildren().add(meldung);
        StackPane.setAlignment(meldung, Pos.TOP_RIGHT);

        spieleEinblendung(meldung);
    }

    /** Breitenbegrenzung, damit lange Meldungen umbrechen statt sich zu strecken. */
    private static final double TEXT_MAX_BREITE = 380;

    private static Label textLabel(String text) {
        Label beschriftung = new Label(text);
        beschriftung.getStyleClass().add("benachrichtigungs-text");
        beschriftung.setWrapText(true);
        beschriftung.setMaxWidth(TEXT_MAX_BREITE);
        return beschriftung;
    }

    private static void spieleEinblendung(HBox meldung) {
        FadeTransition ein = new FadeTransition(EINBLENDEN, meldung);
        ein.setFromValue(0);
        ein.setToValue(1);

        FadeTransition aus = new FadeTransition(AUSBLENDEN, meldung);
        aus.setFromValue(1);
        aus.setToValue(0);

        SequentialTransition ablauf =
                new SequentialTransition(ein, new PauseTransition(STANDZEIT), aus);
        // Nach dem Ausblenden entfernen, sonst sammeln sich unsichtbare Knoten
        ablauf.setOnFinished(ereignis -> {
            if (ebene != null) {
                ebene.getChildren().remove(meldung);
            }
        });
        ablauf.play();
    }
}
