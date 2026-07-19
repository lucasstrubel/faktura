package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.gemeinsam.LoeschAbgelehntException;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import javafx.scene.control.Alert;
import javafx.scene.control.Control;
import javafx.scene.control.ButtonType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Einheitliche Darstellung von Fehler- und Erfolgsmeldungen der
 * JavaFX-Oberfläche (D-F-16, F-17): Das betroffene Eingabefeld wird über die
 * Stilklasse {@code fehler-feld} rot markiert UND die Meldung benennt das
 * Feld namentlich (Q-09).
 */
public final class FxMeldung {

    private static final Logger LOG = LoggerFactory.getLogger(FxMeldung.class);

    /** Stilklasse für ungültige Eingabefelder (css/faktura.css). */
    public static final String FEHLER_STIL = "fehler-feld";

    private FxMeldung() {
    }

    /**
     * Zeigt die Meldung als Dialog an und markiert bei Validierungsfehlern
     * das betroffene Feld rot; alle übrigen Felder werden zurückgesetzt.
     */
    public static void zeige(Meldung meldung, Map<String, Control> felder) {
        if (felder != null) {
            felder.values().forEach(feld -> feld.getStyleClass().remove(FEHLER_STIL));
            if (meldung != null && meldung.feldname() != null) {
                Control feld = felder.get(meldung.feldname());
                if (feld != null) {
                    feld.getStyleClass().add(FEHLER_STIL);
                    feld.requestFocus();
                }
            }
        }
        if (meldung == null) {
            return;
        }
        if (meldung.typ() == MeldungsTyp.FEHLER) {
            Alert alert = new Alert(Alert.AlertType.ERROR, meldung.text());
            alert.setHeaderText(meldung.feldname() != null
                    ? "Eingabe unvollständig: " + meldung.feldname() : "Fehler");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, meldung.text());
            alert.setHeaderText("Erfolg");
            alert.showAndWait();
        }
    }

    /**
     * Zentrale Fehlerbehandlung der Modulansichten: führt die Aktion aus und
     * zeigt fachliche Fehler einheitlich an — Validierungsfehler mit
     * Feldmarkierung (Q-09), abgelehnte Löschvorgänge (GR-04), unzulässige
     * Statuswechsel (GR-02) und Persistenzfehler beim Speichern (IF-01).
     *
     * @return {@code true}, wenn die Aktion ohne Fehler durchlief
     */
    public static boolean mitFehlerbehandlung(Map<String, Control> felder, Runnable aktion) {
        try {
            aktion.run();
            return true;
        } catch (ValidierungsException e) {
            zeige(Meldung.fehler(e.getFeldname(), e.getMessage()), felder);
        } catch (LoeschAbgelehntException | IllegalStateException e) {
            zeige(Meldung.fehler(null, e.getMessage()), felder);
        } catch (UncheckedIOException e) {
            LOG.error("Persistenzfehler (IF-01)", e);
            zeige(Meldung.fehler(null,
                    "Die Daten konnten nicht gespeichert werden: " + e.getMessage()), felder);
        } catch (RuntimeException e) {
            // Letztes Netz: kein stilles Scheitern auf dem FX-Application-Thread
            LOG.error("Unerwarteter Fehler auf dem FX-Application-Thread", e);
            zeige(Meldung.fehler(null, "Unerwarteter Fehler: " + e), felder);
        }
        return false;
    }

    /** Ja/Nein-Bestätigungsdialog; {@code true} nur bei ausdrücklicher Zustimmung. */
    public static boolean bestaetige(String titel, String frage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, frage, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(titel);
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }
}
