package de.lucasstrubel.faktura.gui;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Optional;

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

    /**
     * Eingabeformat: tolerant wie im deutschen Alltag üblich — {@code 1.11.2026},
     * {@code 01.11.26} und {@code 01.11.2026} meinen dasselbe Datum.
     * Zweistellige Jahre werden als 20JJ gelesen.
     */
    private static final DateTimeFormatter EINGABE = new DateTimeFormatterBuilder()
            .appendPattern("d.M.")
            .optionalStart().appendPattern("uuuu").optionalEnd()
            .optionalStart().appendValueReduced(ChronoField.YEAR, 2, 2, 2000).optionalEnd()
            .toFormatter();

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
     * Datumsfeld mit Kalenderauswahl und deutschem Format. Getippte Daten
     * werden tolerant gelesen ({@link #EINGABE}). Früher setzte ein nicht
     * lesbarer Text das Feld stillschweigend auf "kein Datum" — und der
     * Service setzte dann seinen Standardwert ein, ohne dass es auffiel. Jetzt
     * springt die Anzeige sichtbar auf den bisherigen Wert zurück, und
     * {@link #datum(DatePicker, String)} meldet noch nicht übernommenen,
     * unlesbaren Text als Fehler.
     *
     * @param vorgabe vorbelegtes Datum; {@code null} lässt das Feld leer
     */
    public static DatePicker datumsfeld(LocalDate vorgabe) {
        final DatePicker feld = new DatePicker(vorgabe);
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
                    return LocalDate.parse(text.strip(), EINGABE);
                } catch (DateTimeParseException e) {
                    // Unlesbar: bisherigen Wert behalten; die Anzeige springt
                    // sichtbar darauf zurück, statt das Feld zu leeren
                    return feld.getValue();
                }
            }
        });
        feld.setPromptText("TT.MM.JJJJ");
        return feld;
    }

    /**
     * Liest das Datum eines Datumsfelds und übernimmt dabei auch getippten,
     * noch nicht bestätigten Text — ein DatePicker übernimmt Tastatureingaben
     * sonst erst beim Verlassen des Felds, und ein Speichern per Enter liefe
     * mit dem alten Wert. Unlesbarer Text führt zu einer
     * {@link ValidierungsException} mit dem Feldnamen (Q-09).
     *
     * @return das Datum oder {@code null}, wenn das Feld leer ist
     */
    public static LocalDate datum(DatePicker feld, String feldname) {
        try {
            LocalDate wert = liesText(feld);
            feld.setValue(wert);
            return wert;
        } catch (ValidierungsException e) {
            throw new ValidierungsException(feldname, "Das Feld '" + feldname
                    + "' enthält kein gültiges Datum (Format TT.MM.JJJJ): " + feld.getEditor().getText());
        }
    }

    /**
     * Kleiner modaler Dialog, der genau ein Datum abfragt (z. B. den
     * Zahlungseingang, A-F-28).
     *
     * @return das gewählte Datum; leer, wenn abgebrochen wurde
     */
    public static Optional<LocalDate> frageDatum(Window besitzer, String titel, String hinweis,
                                                 String feldname, LocalDate vorgabe) {
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.initOwner(besitzer);
        dialog.setTitle(titel);
        dialog.setHeaderText(hinweis);
        uebernimmStil(dialog.getDialogPane().getScene(), besitzer);

        DatePicker feld = datumsfeld(vorgabe);
        HBox zeile = new HBox(8, new Label(feldname + ":"), feld);
        zeile.setAlignment(Pos.CENTER_LEFT);
        dialog.getDialogPane().setContent(zeile);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Unlesbare Eingabe hält den Dialog offen, statt ihn mit null zu schließen
        Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, ereignis -> {
            try {
                if (datum(feld, feldname) == null) {
                    throw new ValidierungsException(feldname,
                            "Das Pflichtfeld '" + feldname + "' fehlt.");
                }
            } catch (ValidierungsException e) {
                ereignis.consume();
                FxMeldung.zeige(Meldung.fehler(e.getFeldname(), e.getMessage()), null);
            }
        });
        dialog.setResultConverter(knopf -> knopf == ButtonType.OK ? feld.getValue() : null);
        return dialog.showAndWait();
    }

    private static LocalDate liesText(DatePicker feld) {
        String text = feld.getEditor().getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.strip(), EINGABE);
        } catch (DateTimeParseException e) {
            throw new ValidierungsException("Datum", "Kein gültiges Datum: " + text);
        }
    }
}
