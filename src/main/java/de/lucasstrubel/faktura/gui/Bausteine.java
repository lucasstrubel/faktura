package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.DokumentStatus;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Wiederkehrende Bausteine der Oberfläche (Q-05): Statusabzeichen,
 * Leerzustände und Spaltenformate. An einer Stelle gebündelt, damit alle
 * Ansichten gleich aussehen und eine Änderung überall greift.
 */
public final class Bausteine {

    private Bausteine() {
    }

    /**
     * Zellenfabrik für die Statusspalte: zeigt den Status als farbiges
     * Abzeichen statt als rohen Enum-Text (D-F-06).
     */
    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> statusZelle() {
        return spalte -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean leer) {
                super.updateItem(status, leer);
                if (leer || status == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label abzeichen = new Label(anzeigename(status));
                abzeichen.getStyleClass().addAll("status-abzeichen", "status-" + status);
                setGraphic(abzeichen);
                setText(null);
            }
        };
    }

    /** Lesbare Bezeichnung statt {@code AUFTRAGSBESTAETIGUNG} in Großbuchstaben. */
    private static String anzeigename(String status) {
        return switch (DokumentStatus.valueOf(status)) {
            case ENTWURF -> "Entwurf";
            case OFFEN -> "Offen";
            case VERSENDET -> "Versendet";
            case STORNIERT -> "Storniert";
        };
    }

    /**
     * Platzhalter für eine leere Tabelle (D-F-23): Symbol, Überschrift und ein
     * Satz, der erklärt, was als Nächstes zu tun ist — statt einer leeren Fläche.
     */
    public static VBox leerzustand(Feather symbol, String titel, String hinweis) {
        Label ueberschrift = new Label(titel);
        ueberschrift.getStyleClass().add("leerzustand-titel");
        Label text = new Label(hinweis);
        text.getStyleClass().add("leerzustand-text");
        text.setWrapText(true);

        VBox bereich = new VBox(new FontIcon(symbol), ueberschrift, text);
        bereich.getStyleClass().add("leerzustand");
        return bereich;
    }

    /** Suchfeld mit Lupe und Löschsymbol. */
    public static TextField suchfeld(String platzhalter) {
        TextField feld = new TextField();
        feld.setPromptText(platzhalter);
        feld.getStyleClass().add("left-icon");
        return feld;
    }

    /** Rechtsbündige Betragsspalte (kaufmännische Darstellung). */
    public static void alsBetragsspalte(TableColumn<?, ?> spalte) {
        spalte.getStyleClass().add("tabelle-betrag");
    }

    /**
     * Verteilt die Spalten auf die verfügbare Breite, statt bei zu schmalem
     * Fenster einen waagerechten Rollbalken einzublenden und die letzte Spalte
     * abzuschneiden. Die Spaltenbreiten im FXML wirken dabei als Gewichtung.
     */
    public static void passeSpaltenAn(TableView<?> tabelle) {
        tabelle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    /** Belegnummernspalte in Festbreitenschrift, damit Nummern untereinander stehen. */
    public static void alsNummernspalte(TableColumn<?, ?> spalte) {
        spalte.getStyleClass().add("tabelle-nummer");
    }
}
