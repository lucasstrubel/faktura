package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.kordamp.ikonli.feather.Feather;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * Wizard-Dialog der geführten Rechnungserstellung mit genau fünf Schritten
 * (D-F-09 bis F-13); Dialogführung und Validierung liegen im GUI-freien
 * {@link RechnungsWizardController}.
 */
public class RechnungsWizardDialog extends Stage {

    private final RechnungsWizardController controller;
    private final KundenService kundenService;

    private final StackPane karten = new StackPane();
    private final Map<WizardSchritt, Node> kartenJeSchritt = new EnumMap<>(WizardSchritt.class);

    private final TextField kundenSuche = new TextField();
    private final ListView<Kunde> kundenListe = new ListView<>();

    private final ComboBox<Produkt> produktWahl = new ComboBox<>();
    private final Spinner<Integer> mengeWahl = new Spinner<>(1, 99999, 1);
    private final ListView<String> positionsListe = new ListView<>();

    private final DatePicker rechnungsdatumFeld = Dialoge.datumsfeld(LocalDate.now());
    private final DatePicker zahlungszielFeld = Dialoge.datumsfeld(null);
    private final ProgressBar fortschritt = new ProgressBar(0);

    private final TextArea zusammenfassung = new TextArea();

    private final Button zurueckKnopf = new Button("< Zurück");
    private final Button weiterKnopf = new Button("Weiter >");
    private final Button speichernKnopf = new Button("Speichern");
    private final Label schrittAnzeige = new Label();
    private final Label[] schrittMarkierungen = new Label[WizardSchritt.values().length];

    public RechnungsWizardDialog(Window besitzer, RechnungsWizardController controller,
                                 KundenService kundenService, ProduktService produktService) {
        this.controller = controller;
        this.kundenService = kundenService;
        Dialoge.richteEin(this, besitzer, "Geführte Rechnungserstellung");

        produktWahl.getItems().addAll(produktService.suche(""));
        produktWahl.setConverter(new StringConverter<>() {
            @Override
            public String toString(Produkt produkt) {
                return produkt == null ? ""
                        : produkt.getBezeichnung() + " (" + produkt.getProduktnummer() + ")";
            }

            @Override
            public Produkt fromString(String text) {
                return null;
            }
        });

        Scene szene = new Scene(baueOberflaeche(), 760, 560);
        Dialoge.uebernimmStil(szene, besitzer);
        setScene(szene);
        setMinWidth(620);
        setMinHeight(480);

        ladeKunden("");
        zeigeSchritt();
        setOnCloseRequest(ereignis -> {
            if (!darfVerwerfen()) {
                ereignis.consume();
            }
        });
    }

    private BorderPane baueOberflaeche() {
        // Schrittindikator: kurze Bezeichnungen, damit nichts abschneidet, und
        // ein Fortschrittsbalken, der die Position auf einen Blick zeigt (Q-05).
        HBox schritte = new HBox(6);
        schritte.setAlignment(Pos.CENTER_LEFT);
        WizardSchritt[] alleSchritte = WizardSchritt.values();
        for (int i = 0; i < alleSchritte.length; i++) {
            if (i > 0) {
                Label pfeil = new Label("›");
                pfeil.getStyleClass().add("schritt-inaktiv");
                schritte.getChildren().add(pfeil);
            }
            schrittMarkierungen[i] = new Label(kurzName(alleSchritte[i]));
            schritte.getChildren().add(schrittMarkierungen[i]);
        }
        fortschritt.getStyleClass().add("assistent-fortschritt");
        fortschritt.setMaxWidth(Double.MAX_VALUE);

        VBox kopf = new VBox(6, schritte, fortschritt, schrittAnzeige);
        kopf.getStyleClass().add("assistent-kopf");

        kartenJeSchritt.put(WizardSchritt.KUNDE_WAEHLEN, baueSchrittKunde());
        kartenJeSchritt.put(WizardSchritt.POSITIONEN_ERFASSEN, baueSchrittPositionen());
        kartenJeSchritt.put(WizardSchritt.DATEN_BESTAETIGEN, baueSchrittDaten());
        kartenJeSchritt.put(WizardSchritt.ZUSAMMENFASSUNG, baueSchrittZusammenfassung());
        kartenJeSchritt.put(WizardSchritt.SPEICHERN, baueSchrittSpeichern());
        karten.getChildren().addAll(kartenJeSchritt.values());

        Button abbrechen = new Button("Abbrechen");
        abbrechen.setCancelButton(true);
        abbrechen.setOnAction(e -> {
            if (darfVerwerfen()) {
                close();
            }
        });
        zurueckKnopf.setOnAction(e -> {
            controller.zurueck();
            zeigeSchritt();
        });
        weiterKnopf.setOnAction(e -> weiter());
        speichernKnopf.setOnAction(e -> speichere());
        ButtonBar knoepfe = new ButtonBar();
        ButtonBar.setButtonData(abbrechen, ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonBar.setButtonData(zurueckKnopf, ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonBar.setButtonData(weiterKnopf, ButtonBar.ButtonData.NEXT_FORWARD);
        ButtonBar.setButtonData(speichernKnopf, ButtonBar.ButtonData.FINISH);
        knoepfe.getButtons().addAll(abbrechen, zurueckKnopf, weiterKnopf, speichernKnopf);
        knoepfe.setPadding(new Insets(10, 0, 0, 0));

        BorderPane wurzel = new BorderPane();
        wurzel.setPadding(new Insets(12));
        wurzel.setTop(kopf);
        wurzel.setCenter(karten);
        wurzel.setBottom(knoepfe);
        return wurzel;
    }

    /** Schutz vor Datenverlust: Nachfrage, wenn bereits Eingaben erfasst wurden (D-F-02). */
    private boolean darfVerwerfen() {
        boolean datenErfasst = !controller.getModel().getPositionen().isEmpty()
                || controller.getModel().getKundenNr() != null;
        return !datenErfasst || FxMeldung.bestaetige("Eingaben verwerfen",
                "Die erfassten Eingaben gehen verloren. Assistent wirklich schließen?");
    }

    /** Schritt 1: Kunde auswählen (F-09). */
    private Node baueSchrittKunde() {
        kundenSuche.setPromptText("Name oder Kundennummer…");
        kundenSuche.textProperty().addListener(
                (beobachtbar, alt, neu) -> ladeKunden(neu.strip()));
        kundenListe.setCellFactory(liste -> new ListCell<>() {
            @Override
            protected void updateItem(Kunde kunde, boolean leer) {
                super.updateItem(kunde, leer);
                setText(leer || kunde == null ? null
                        : kunde.getName() + " (" + kunde.getKundennummer() + ")");
            }
        });
        kundenListe.getSelectionModel().selectedItemProperty()
                .addListener((beobachtbar, alt, kunde) -> controller.getModel()
                        .setKundenNr(kunde == null ? null : kunde.getKundennummer()));
        // Das Suchfeld nimmt die Restbreite ein; mit fester Breite schnitt der
        // Platzhaltertext ab.
        HBox.setHgrow(kundenSuche, Priority.ALWAYS);
        kundenSuche.setMaxWidth(Double.MAX_VALUE);
        Label beschriftung = new Label("Suche:");
        HBox suche = new HBox(8, beschriftung, kundenSuche);
        suche.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(6, suche, kundenListe);
        VBox.setVgrow(kundenListe, Priority.ALWAYS);
        return panel;
    }

    /** Schritt 2: mindestens eine Produktposition mit Menge erfassen (F-09). */
    private Node baueSchrittPositionen() {
        Button hinzufuegen = new Button("Hinzufügen");
        hinzufuegen.setOnAction(e -> {
            Produkt produkt = produktWahl.getValue();
            if (produkt == null) {
                return;
            }
            int menge = mengeWahl.getValue();
            controller.getModel().fuegePositionHinzu(
                    new PositionsEingabe(produkt.getProduktnummer(), menge));
            positionsListe.getItems().add(menge + " x " + produkt.getBezeichnung()
                    + " (" + produkt.getProduktnummer() + ")");
        });
        Button entfernen = new Button("Entfernen");
        entfernen.setOnAction(e -> {
            int index = positionsListe.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                controller.getModel().entfernePosition(index);
                positionsListe.getItems().remove(index);
            }
        });
        // Nur die Produktauswahl darf wachsen; Beschriftungen und Knöpfe
        // behalten ihre Breite, sonst steht dort „Pro…“ und „Hinzuf…“.
        Label produktText = festeBreite(new Label("Produkt:"));
        Label mengeText = festeBreite(new Label("Menge:"));
        festeBreite(hinzufuegen);
        festeBreite(entfernen);
        festeBreite(mengeWahl);
        mengeWahl.setPrefWidth(90);
        produktWahl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(produktWahl, Priority.ALWAYS);

        HBox eingabe = new HBox(8, produktText, produktWahl, mengeText, mengeWahl,
                hinzufuegen, entfernen);
        eingabe.setAlignment(Pos.CENTER_LEFT);

        positionsListe.setPlaceholder(Bausteine.leerzustand(Feather.PACKAGE,
                "Noch keine Positionen",
                "Produkt und Menge wählen und auf „Hinzufügen“ klicken."));

        VBox panel = new VBox(6, eingabe, positionsListe);
        VBox.setVgrow(positionsListe, Priority.ALWAYS);
        return panel;
    }

    /** Verhindert, dass ein Bedienelement in einer HBox schrumpft. */
    private static <T extends javafx.scene.layout.Region> T festeBreite(T element) {
        element.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        return element;
    }

    /** Schritt 3: Rechnungsdatum und Zahlungsziel bestätigen (F-09, F-10). */
    private Node baueSchrittDaten() {
        rechnungsdatumFeld.setTooltip(new Tooltip("Pflichtfeld — über den Kalender wählbar"));
        zahlungszielFeld.setTooltip(new Tooltip(
                "Optional — leer bedeutet 14 Tage nach Rechnungsdatum"));
        Label legende = new Label("* Pflichtfeld");
        legende.getStyleClass().add("pflichtfeld-legende");

        HBox rechnungsdatum = new HBox(8, beschriftung("Rechnungsdatum *"), rechnungsdatumFeld);
        rechnungsdatum.setAlignment(Pos.CENTER_LEFT);
        HBox zahlungsziel = new HBox(8, beschriftung("Zahlungsziel"), zahlungszielFeld);
        zahlungsziel.setAlignment(Pos.CENTER_LEFT);

        return new VBox(10, rechnungsdatum, zahlungsziel, legende);
    }

    /** Beschriftung fester Breite, damit die Felder untereinander stehen. */
    private static Label beschriftung(String text) {
        Label label = new Label(text);
        label.setMinWidth(150);
        return label;
    }

    /** Schritt 4: Zusammenfassung prüfen (F-12). */
    private Node baueSchrittZusammenfassung() {
        zusammenfassung.setEditable(false);
        return zusammenfassung;
    }

    /** Schritt 5: speichern (F-13). */
    private Node baueSchrittSpeichern() {
        Label hinweis = new Label("Alle Angaben sind erfasst. Klicken Sie auf „Speichern“, "
                + "um die Rechnung zu erstellen.");
        hinweis.setWrapText(true);
        return new VBox(hinweis);
    }

    private void weiter() {
        if (controller.getModel().getAktuellerSchritt() == WizardSchritt.DATEN_BESTAETIGEN
                && !uebernehmeDaten()) {
            return;
        }
        if (!controller.weiter()) {
            FxMeldung.zeige(controller.getLetzteMeldung(), null);
            return;
        }
        zeigeSchritt();
    }

    /**
     * Übernimmt die Datumsfelder in das Modell (F-10, Q-09). Ein
     * {@link DatePicker} kann kein ungültiges Datum liefern, deshalb bleibt
     * hier nur die Pflichtfeldprüfung — die frühere doppelte
     * Parse-Fehlerbehandlung ist entfallen.
     */
    private boolean uebernehmeDaten() {
        LocalDate rechnungsdatum = rechnungsdatumFeld.getValue();
        if (rechnungsdatum == null) {
            FxMeldung.zeige(Meldung.fehler("Rechnungsdatum",
                    "Das Pflichtfeld 'Rechnungsdatum' fehlt."), null);
            return false;
        }
        controller.getModel().setRechnungsdatum(rechnungsdatum);
        controller.getModel().setZahlungsziel(zahlungszielFeld.getValue());
        return true;
    }

    private void speichere() {
        Meldung meldung = controller.speichern();
        FxMeldung.zeige(meldung, null);
        if (meldung.typ() == MeldungsTyp.ERFOLG) {
            close();
        }
    }

    private void zeigeSchritt() {
        WizardSchritt schritt = controller.getModel().getAktuellerSchritt();
        if (schritt == WizardSchritt.ZUSAMMENFASSUNG) {
            zusammenfassung.setText(controller.erzeugeZusammenfassung());
        }
        kartenJeSchritt.forEach((s, karte) -> {
            karte.setVisible(s == schritt);
            karte.setManaged(s == schritt);
        });
        int anzahl = WizardSchritt.values().length;
        schrittAnzeige.setText("Schritt " + (schritt.ordinal() + 1) + " von " + anzahl + ": "
                + schrittName(schritt));
        fortschritt.setProgress((schritt.ordinal() + 1.0) / anzahl);
        for (int i = 0; i < schrittMarkierungen.length; i++) {
            // Erledigte Schritte bleiben sichtbar abgehakt, der aktuelle ist hervorgehoben
            String stil = i < schritt.ordinal() ? "schritt-erledigt"
                    : i == schritt.ordinal() ? "schritt-aktuell" : "schritt-inaktiv";
            schrittMarkierungen[i].getStyleClass()
                    .removeAll("schritt-aktuell", "schritt-erledigt", "schritt-inaktiv");
            schrittMarkierungen[i].getStyleClass().add(stil);
        }
        zurueckKnopf.setDisable(schritt.ordinal() == 0);
        weiterKnopf.setDisable(schritt == WizardSchritt.SPEICHERN);
        speichernKnopf.setDisable(schritt != WizardSchritt.SPEICHERN);
        // Enter führt immer die sinnvollste Aktion des Schritts aus
        weiterKnopf.setDefaultButton(schritt != WizardSchritt.SPEICHERN);
        speichernKnopf.setDefaultButton(schritt == WizardSchritt.SPEICHERN);
    }

    /** Ausgeschriebener Name für die Zeile unter dem Fortschrittsbalken. */
    private static String schrittName(WizardSchritt schritt) {
        return switch (schritt) {
            case KUNDE_WAEHLEN -> "Kunde auswählen";
            case POSITIONEN_ERFASSEN -> "Positionen erfassen";
            case DATEN_BESTAETIGEN -> "Rechnungsdatum und Zahlungsziel";
            case ZUSAMMENFASSUNG -> "Zusammenfassung prüfen";
            case SPEICHERN -> "Speichern";
        };
    }

    /**
     * Kurzform für die Schrittkette. Die ausgeschriebenen Namen passten nicht
     * nebeneinander und wurden allesamt abgeschnitten („5. Spei…“).
     */
    private static String kurzName(WizardSchritt schritt) {
        return switch (schritt) {
            case KUNDE_WAEHLEN -> "Kunde";
            case POSITIONEN_ERFASSEN -> "Positionen";
            case DATEN_BESTAETIGEN -> "Daten";
            case ZUSAMMENFASSUNG -> "Prüfen";
            case SPEICHERN -> "Speichern";
        };
    }

    private void ladeKunden(String suchbegriff) {
        kundenListe.getItems().setAll(kundenService.suche(suchbegriff));
    }
}
