package de.lucasstrubel.faktura.gui;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.EnumMap;
import java.util.Map;

/**
 * Controller des Hauptfensters (D-F-01): Seitennavigation, bedarfsweises
 * Laden der Ansichten, Statuszeile und Erscheinungsbild.
 *
 * <p>Zuvor war diese Klasse leer — die Navigation erledigte ein {@code TabPane}
 * mit vier {@code fx:include}. Das lud beim Start sämtliche Ansichten samt
 * ihrer Datenbankabfragen, obwohl nur eine sichtbar ist. Jetzt wird eine
 * Ansicht erst beim ersten Aufruf geladen und danach zwischengespeichert.
 */
public class HauptAnsichtController {

    private final FxmlLader lader;
    private final Erscheinungsbild erscheinungsbild;
    private final HintergrundAufgaben hintergrund;

    /** Einmal geladene Ansichten; der Zustand bleibt beim Wechseln erhalten. */
    private final Map<Navigationsziel, Parent> ansichten = new EnumMap<>(Navigationsziel.class);

    @FXML private ListView<Navigationsziel> navigation;
    @FXML private StackPane inhaltsbereich;
    @FXML private StackPane benachrichtigungsEbene;
    @FXML private Label statusText;
    @FXML private ProgressIndicator fortschritt;
    @FXML private Button themaKnopf;

    public HauptAnsichtController(FxmlLader lader,
                                  Erscheinungsbild erscheinungsbild,
                                  HintergrundAufgaben hintergrund) {
        this.lader = lader;
        this.erscheinungsbild = erscheinungsbild;
        this.hintergrund = hintergrund;
    }

    @FXML
    private void initialize() {
        Benachrichtigung.setzeEbene(benachrichtigungsEbene);

        navigation.setCellFactory(liste -> new NavigationsZelle());
        navigation.getItems().setAll(Navigationsziel.values());
        navigation.getSelectionModel().selectedItemProperty()
                .addListener((beobachtbar, alt, ziel) -> zeige(ziel));
        navigation.getSelectionModel().selectFirst();

        statusText.textProperty().bind(Bindings.createStringBinding(
                () -> hintergrund.laeuft().get() ? hintergrund.beschreibung().get() : "Bereit",
                hintergrund.laeuft(), hintergrund.beschreibung()));
        fortschritt.visibleProperty().bind(hintergrund.laeuft());

        aktualisiereThemaKnopf();
        // Die Szene steht erst nach dem Anzeigen bereit
        inhaltsbereich.sceneProperty().addListener((beobachtbar, alt, szene) -> {
            if (szene != null) {
                registriereTastenkuerzel(szene);
            }
        });
    }

    /** Lädt die Ansicht beim ersten Aufruf und zeigt sie an. */
    private void zeige(Navigationsziel ziel) {
        if (ziel == null) {
            return;
        }
        Parent ansicht = ansichten.computeIfAbsent(ziel, z -> lader.lade(z.ansicht()));
        inhaltsbereich.getChildren().setAll(ansicht);
    }

    /**
     * Tastenkürzel für die Navigation ({@code Strg+1} bis {@code Strg+5}) und
     * das Erscheinungsbild ({@code Strg+D}). Die ansichtseigenen Kürzel
     * (Neu, Suche, Aktualisieren) registrieren die jeweiligen Controller.
     */
    private void registriereTastenkuerzel(Scene szene) {
        Navigationsziel[] ziele = Navigationsziel.values();
        for (int i = 0; i < ziele.length; i++) {
            Navigationsziel ziel = ziele[i];
            szene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.valueOf("DIGIT" + (i + 1)),
                            KeyCombination.CONTROL_DOWN),
                    () -> navigation.getSelectionModel().select(ziel));
        }
        szene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
                this::wechsleThema);
    }

    @FXML
    private void wechsleThema() {
        erscheinungsbild.wechsle();
        aktualisiereThemaKnopf();
    }

    private void aktualisiereThemaKnopf() {
        boolean dunkel = erscheinungsbild.dunkel().get();
        themaKnopf.setGraphic(new FontIcon(dunkel ? Feather.SUN : Feather.MOON));
        themaKnopf.setText(dunkel ? "Heller Modus" : "Dunkler Modus");
        themaKnopf.setTooltip(new Tooltip("Erscheinungsbild wechseln (Strg+D)"));
    }

    /** Navigationseintrag mit Symbol und Beschriftung. */
    private static class NavigationsZelle extends ListCell<Navigationsziel> {

        @Override
        protected void updateItem(Navigationsziel ziel, boolean leer) {
            super.updateItem(ziel, leer);
            if (leer || ziel == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            HBox inhalt = new HBox(10, new FontIcon(ziel.symbol()), new Label(ziel.beschriftung()));
            inhalt.setAlignment(Pos.CENTER_LEFT);
            setGraphic(inhalt);
            setText(null);
        }
    }

    /** Wechselt zur angegebenen Ansicht; genutzt von den Schnellaktionen der Übersicht. */
    void navigiereZu(Navigationsziel ziel) {
        navigation.getSelectionModel().select(ziel);
    }

    /** Der aktuell gezeigte Inhalt; für Tests der Navigation. */
    Node aktuelleAnsicht() {
        return inhaltsbereich.getChildren().isEmpty()
                ? null : inhaltsbereich.getChildren().get(0);
    }
}
