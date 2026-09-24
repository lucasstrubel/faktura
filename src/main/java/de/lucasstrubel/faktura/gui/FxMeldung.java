package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.gemeinsam.AusgabeException;
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
            // Fehler bleiben modal: Sie verlangen eine Kenntnisnahme
            Alert alert = new Alert(Alert.AlertType.ERROR, meldung.text());
            alert.setHeaderText(meldung.feldname() != null
                    ? "Eingabe unvollständig: " + meldung.feldname() : "Fehler");
            richteEin(alert);
            alert.showAndWait();
        } else if (Benachrichtigung.istBereit()) {
            // Erfolge stören den Arbeitsfluss nicht und blenden von selbst aus
            Benachrichtigung.zeige(meldung.text());
        } else {
            // Ohne aufgebaute Oberfläche (z. B. in Dialogen vor dem Anzeigen)
            Alert alert = new Alert(Alert.AlertType.INFORMATION, meldung.text());
            alert.setHeaderText("Erfolg");
            richteEin(alert);
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
        } catch (RuntimeException e) {
            zeige(zuMeldung(e), felder);
            return false;
        }
    }

    /**
     * Übersetzt eine Ausnahme in eine Meldung für den Anwender: fachliche
     * Fehler mit Feldmarkierung (Q-09), abgelehnte Löschvorgänge (GR-04),
     * unzulässige Statuswechsel (GR-02) und Persistenzfehler (IF-01).
     *
     * <p>Unerwartete Ausnahmen werden protokolliert, aber nicht im Wortlaut
     * angezeigt: Ein Stacktrace-Fragment hilft dem Anwender nicht und wirkt
     * unfertig. Die Meldung verweist stattdessen auf die Logdatei.
     *
     * <p>Öffentlich, damit die Hintergrundausführung
     * ({@link HintergrundAufgaben}) dieselbe Zuordnung verwendet, statt sie zu
     * wiederholen.
     */
    public static Meldung zuMeldung(Throwable fehler) {
        // Aus einer Hintergrundaufgabe kommt die Ursache verpackt an
        Throwable ursache = fehler instanceof java.util.concurrent.ExecutionException
                && fehler.getCause() != null ? fehler.getCause() : fehler;
        if (ursache instanceof ValidierungsException e) {
            return Meldung.fehler(e.getFeldname(), e.getMessage());
        }
        if (ursache instanceof LoeschAbgelehntException
                || ursache instanceof AusgabeException
                || ursache instanceof IllegalStateException) {
            return Meldung.fehler(null, ursache.getMessage());
        }
        if (ursache instanceof UncheckedIOException e) {
            LOG.error("Persistenzfehler (IF-01)", e);
            return Meldung.fehler(null,
                    "Die Daten konnten nicht gespeichert werden: " + e.getMessage());
        }
        // Letztes Netz: kein stilles Scheitern
        LOG.error("Unerwarteter Fehler", ursache);
        return Meldung.fehler(null, "Es ist ein unerwarteter Fehler aufgetreten. "
                + "Einzelheiten stehen in der Logdatei faktura.log.");
    }

    /**
     * Zeigt die Meldung auf dem FX-Application-Thread — auch wenn der Aufruf
     * aus einem anderen Faden kommt (globale Fehlerbehandlung).
     */
    public static void zeigeAufFxThread(Meldung meldung) {
        if (javafx.application.Platform.isFxApplicationThread()) {
            zeige(meldung, null);
        } else {
            javafx.application.Platform.runLater(() -> zeige(meldung, null));
        }
    }

    /** Ja/Nein-Bestätigungsdialog; {@code true} nur bei ausdrücklicher Zustimmung. */
    public static boolean bestaetige(String titel, String frage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, frage, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(titel);
        richteEin(alert);
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    /**
     * Hängt den Hinweis an das gerade aktive Fenster: Ohne Besitzer erschien
     * er mitunter auf einem anderen Bildschirm, ohne Anwendungssymbol, und
     * gab den Fokus danach an das Hauptfenster statt an den offenen Dialog
     * zurück. Stil und Symbol werden vom Besitzer übernommen.
     */
    private static void richteEin(Alert alert) {
        javafx.stage.Window.getWindows().stream()
                .filter(javafx.stage.Window::isFocused)
                .findFirst()
                .ifPresent(besitzer -> {
                    alert.initOwner(besitzer);
                    Dialoge.uebernimmStil(alert.getDialogPane().getScene(), besitzer);
                });
    }
}
