package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentStatus;
import de.lucasstrubel.faktura.dokumente.Rechnung;

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
                Label abzeichen = new Label(statusText(status));
                abzeichen.getStyleClass().addAll("status-abzeichen", "status-" + status);
                setGraphic(abzeichen);
                setText(null);
            }
        };
    }

    /** Anzeigestatus für bezahlte Rechnungen (A-F-28); kein eigener {@link DokumentStatus}. */
    public static final String BEZAHLT = "BEZAHLT";

    /**
     * Lesbare Bezeichnung eines Status statt des Enum-Namens in
     * Großbuchstaben; versteht zusätzlich den Anzeigestatus {@link #BEZAHLT}.
     */
    public static String statusText(String status) {
        if (BEZAHLT.equals(status)) {
            return "Bezahlt";
        }
        return statusText(DokumentStatus.valueOf(status));
    }

    public static String statusText(DokumentStatus status) {
        return switch (status) {
            case ENTWURF -> "Entwurf";
            case OFFEN -> "Offen";
            case VERSENDET -> "Versendet";
            case STORNIERT -> "Storniert";
        };
    }

    /**
     * Status, wie er in Listen erscheint: eine bezahlte Rechnung zeigt
     * „Bezahlt“ statt „Versendet“ — die Frage „ist das Geld da?“ ist in der
     * Liste wichtiger als der Versandstatus.
     */
    public static String anzeigestatus(Dokument dokument) {
        if (dokument instanceof Rechnung rechnung && rechnung.istBezahlt()) {
            return BEZAHLT;
        }
        return dokument.getStatus().name();
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
