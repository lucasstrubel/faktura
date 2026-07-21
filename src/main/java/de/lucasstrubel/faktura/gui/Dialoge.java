package de.lucasstrubel.faktura.gui;

import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Gemeinsame Bausteine der modalen Dialoge — das Gegenstück zu
 * {@link Bausteine} für die Ansichten.
 *
 * <p>Zuvor richtete jeder der vier Dialoge Modalität, Besitzer und
 * Stylesheets selbst ein und vergaß dabei durchweg das Anwendungssymbol; die
 * Datumsfelder waren Freitext mit eigener Parse-Behandlung, zweimal
 * unabhängig ausgeführt. Beides liegt jetzt an einer Stelle.
 */
public final class Dialoge {

    /** Anzeigeformat der Datumsfelder (D-F-10). */
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private Dialoge() {
    }

    /**
     * Richtet einen Dialog einheitlich ein: modal zum Besitzerfenster, mit
     * dessen Stylesheets (damit heller wie dunkler Modus greifen) und dessen
     * Anwendungssymbol.
     */
    public static void richteEin(Stage dialog, Window besitzer, String titel) {
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(besitzer);
        dialog.setTitle(titel);
        if (besitzer instanceof Stage besitzerFenster) {
            // Symbole sind bereits in FxAnwendung geladen; keine zweite Quelle
            dialog.getIcons().addAll(besitzerFenster.getIcons());
        }
    }

    /** Überträgt die Stylesheets des Besitzerfensters auf die Dialogszene. */
    public static void uebernimmStil(javafx.scene.Scene szene, Window besitzer) {
        if (besitzer != null && besitzer.getScene() != null) {
            szene.getStylesheets().addAll(besitzer.getScene().getStylesheets());
        }
    }

    /** Kopfzeile eines Dialogs: Titel und erläuternder Satz. */
    public static VBox kopf(String titel, String hinweis) {
        Label ueberschrift = new Label(titel);
        ueberschrift.getStyleClass().add("dialog-titel");
        Label text = new Label(hinweis);
        text.getStyleClass().add("ansicht-untertitel");
        text.setWrapText(true);

        VBox bereich = new VBox(2, ueberschrift, text);
        bereich.getStyleClass().add("dialog-kopf");
        return bereich;
    }

    /**
     * Datumsfeld mit Kalenderauswahl und deutschem Format. Ersetzt die
     * früheren Freitextfelder: Eine ungültige Eingabe kann dadurch gar nicht
     * erst entstehen, und die Parse-Fehlerbehandlung entfällt.
     *
     * @param vorgabe vorbelegtes Datum; {@code null} lässt das Feld leer
     */
    public static DatePicker datumsfeld(LocalDate vorgabe) {
        DatePicker feld = new DatePicker(vorgabe);
        feld.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate datum) {
                return datum == null ? "" : DATUM.format(datum);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(text.strip(), DATUM);
                } catch (DateTimeParseException e) {
                    // Getippter Unsinn setzt das Feld zurück, statt zu scheitern
                    return null;
                }
            }
        });
        feld.setPromptText("TT.MM.JJJJ");
        return feld;
    }
}
