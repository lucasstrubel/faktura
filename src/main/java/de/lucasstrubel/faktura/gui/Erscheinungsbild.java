package de.lucasstrubel.faktura.gui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Theme;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.prefs.Preferences;

/**
 * Erscheinungsbild und Fensterzustand über Sitzungen hinweg: heller oder
 * dunkler Modus sowie Größe und Position des Hauptfensters. Beides liegt in
 * den Benutzereinstellungen des Betriebssystems
 * ({@link Preferences}) — es sind Bedienvorlieben, keine Fachdaten, und hat
 * deshalb in der Datenbank nichts verloren.
 *
 * <p>Der Themenwechsel tauscht das AtlantaFX-Stylesheet aus; das eigene
 * {@code faktura.css} arbeitet ausschließlich mit dessen Farbvariablen und
 * trägt beide Modi ohne Anpassung.
 */
@Component
public class Erscheinungsbild {

    private static final Logger LOG = LoggerFactory.getLogger(Erscheinungsbild.class);

    private static final String SCHLUESSEL_DUNKEL = "dunkelmodus";
    private static final String SCHLUESSEL_BREITE = "fenster-breite";
    private static final String SCHLUESSEL_HOEHE = "fenster-hoehe";
    private static final String SCHLUESSEL_X = "fenster-x";
    private static final String SCHLUESSEL_Y = "fenster-y";
    private static final String SCHLUESSEL_MAXIMIERT = "fenster-maximiert";

    private static final double STANDARD_BREITE = 1180;
    private static final double STANDARD_HOEHE = 720;
    private static final double MINDEST_BREITE = 900;
    private static final double MINDEST_HOEHE = 560;

    /**
     * Unbelegte Position: Das Fenster wird dann zentriert. {@code NaN} statt
     * eines Zahlenwerts, weil sich damit ohne Gleitkommavergleich prüfen lässt,
     * ob überhaupt etwas gespeichert war.
     */
    private static final double OHNE_POSITION = Double.NaN;

    private final Preferences einstellungen =
            Preferences.userNodeForPackage(Erscheinungsbild.class);

    private final ReadOnlyBooleanWrapper dunkel = new ReadOnlyBooleanWrapper();

    public Erscheinungsbild() {
        dunkel.set(einstellungen.getBoolean(SCHLUESSEL_DUNKEL, false));
    }

    /** Setzt das gespeicherte Thema; beim ersten Start den hellen Modus. */
    public void wendeAn() {
        Application.setUserAgentStylesheet(thema().getUserAgentStylesheet());
    }

    /** Schaltet zwischen hellem und dunklem Modus um und merkt sich die Wahl. */
    public void wechsle() {
        dunkel.set(!dunkel.get());
        einstellungen.putBoolean(SCHLUESSEL_DUNKEL, dunkel.get());
        wendeAn();
        LOG.debug("Erscheinungsbild gewechselt auf {}", dunkel.get() ? "dunkel" : "hell");
    }

    /** {@code true}, wenn der dunkle Modus aktiv ist (für Symbol und Beschriftung). */
    public ReadOnlyBooleanProperty dunkel() {
        return dunkel.getReadOnlyProperty();
    }

    private Theme thema() {
        return dunkel.get() ? new PrimerDark() : new PrimerLight();
    }

    /**
     * Stellt Größe und Position des Hauptfensters wieder her und sichert jede
     * Änderung beim Schließen. Eine Position außerhalb aller Bildschirme wird
     * verworfen — sonst startet die Anwendung unsichtbar, wenn ein zweiter
     * Monitor abgezogen wurde.
     */
    public void bindeFenster(Stage buehne) {
        buehne.setMinWidth(MINDEST_BREITE);
        buehne.setMinHeight(MINDEST_HOEHE);
        buehne.setWidth(einstellungen.getDouble(SCHLUESSEL_BREITE, STANDARD_BREITE));
        buehne.setHeight(einstellungen.getDouble(SCHLUESSEL_HOEHE, STANDARD_HOEHE));

        double x = einstellungen.getDouble(SCHLUESSEL_X, OHNE_POSITION);
        double y = einstellungen.getDouble(SCHLUESSEL_Y, OHNE_POSITION);
        if (!Double.isNaN(x) && !Double.isNaN(y) && istSichtbar(x, y)) {
            buehne.setX(x);
            buehne.setY(y);
        } else {
            buehne.centerOnScreen();
        }
        buehne.setMaximized(einstellungen.getBoolean(SCHLUESSEL_MAXIMIERT, false));

        buehne.setOnHiding(ereignis -> sichereFenster(buehne));
    }

    private void sichereFenster(Stage buehne) {
        einstellungen.putBoolean(SCHLUESSEL_MAXIMIERT, buehne.isMaximized());
        // Im maximierten Zustand sind Größe und Position die des Bildschirms;
        // gesichert wird deshalb nur der wiederhergestellte Zustand.
        if (!buehne.isMaximized()) {
            einstellungen.putDouble(SCHLUESSEL_BREITE, buehne.getWidth());
            einstellungen.putDouble(SCHLUESSEL_HOEHE, buehne.getHeight());
            einstellungen.putDouble(SCHLUESSEL_X, buehne.getX());
            einstellungen.putDouble(SCHLUESSEL_Y, buehne.getY());
        }
    }

    /** Prüft, ob der Punkt auf einem aktuell angeschlossenen Bildschirm liegt. */
    private static boolean istSichtbar(double x, double y) {
        return javafx.stage.Screen.getScreens().stream()
                .anyMatch(schirm -> schirm.getVisualBounds().contains(x, y));
    }
}
