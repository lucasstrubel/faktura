package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktService;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Wizard-Dialog der geführten Rechnungserstellung mit genau fünf Schritten
 * (D-F-09 bis F-13); Dialogführung und Validierung liegen im GUI-freien
 * {@link RechnungsWizardController}.
 */
public class RechnungsWizardDialog extends Stage {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RechnungsWizardController controller;
    private final KundenService kundenService;

    private final StackPane karten = new StackPane();
    private final Map<WizardSchritt, Node> kartenJeSchritt = new EnumMap<>(WizardSchritt.class);

    private final TextField kundenSuche = new TextField();
    private final ListView<Kunde> kundenListe = new ListView<>();

    private final ComboBox<Produkt> produktWahl = new ComboBox<>();
    private final Spinner<Integer> mengeWahl = new Spinner<>(1, 99999, 1);
    private final ListView<String> positionsListe = new ListView<>();

    private final TextField rechnungsdatumFeld = new TextField();
    private final TextField zahlungszielFeld = new TextField();

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
        initModality(Modality.APPLICATION_MODAL);
        initOwner(besitzer);
        setTitle("Geführte Rechnungserstellung");

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

        Scene szene = new Scene(baueOberflaeche(), 720, 520);
        szene.getStylesheets().addAll(besitzer.getScene().getStylesheets());
        setScene(szene);

        ladeKunden("");
        zeigeSchritt();
        setOnCloseRequest(ereignis -> {
            if (!darfVerwerfen()) {
                ereignis.consume();
            }
        });
    }

    private BorderPane baueOberflaeche() {
        // Schrittindikator: alle fünf Schritte sichtbar, aktueller hervorgehoben (Q-05)
        HBox schritte = new HBox(6);
        WizardSchritt[] alleSchritte = WizardSchritt.values();
        for (int i = 0; i < alleSchritte.length; i++) {
            if (i > 0) {
                schritte.getChildren().add(new Label("›"));
            }
            schrittMarkierungen[i] = new Label((i + 1) + ". " + schrittName(alleSchritte[i]));
            schritte.getChildren().add(schrittMarkierungen[i]);
        }
        VBox kopf = new VBox(4, schritte, schrittAnzeige);
        kopf.setPadding(new Insets(0, 0, 8, 0));

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
        HBox suche = new HBox(8, new Label("Suche:"), kundenSuche);
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
        HBox eingabe = new HBox(8, new Label("Produkt:"), produktWahl,
                new Label("Menge:"), mengeWahl, hinzufuegen, entfernen);
        VBox panel = new VBox(6, eingabe, positionsListe);
        VBox.setVgrow(positionsListe, Priority.ALWAYS);
        return panel;
    }

    /** Schritt 3: Rechnungsdatum und Zahlungsziel bestätigen (F-09). */
    private Node baueSchrittDaten() {
        rechnungsdatumFeld.setText(DATUM.format(LocalDate.now()));
        rechnungsdatumFeld.setTooltip(new Tooltip("Pflichtfeld — Format: TT.MM.JJJJ"));
        zahlungszielFeld.setTooltip(new Tooltip(
                "Optional — Format: TT.MM.JJJJ, leer = 14 Tage nach Rechnungsdatum"));
        Label legende = new Label("* Pflichtfeld");
        legende.getStyleClass().add("pflichtfeld-legende");
        HBox felder = new HBox(8,
                new Label("Rechnungsdatum (TT.MM.JJJJ): *"), rechnungsdatumFeld,
                new Label("Zahlungsziel (leer = 14 Tage):"), zahlungszielFeld);
        return new VBox(8, felder, legende);
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

    /** Übernimmt die Datumsfelder in das Modell; bei Formatfehlern Meldung (F-10, Q-09). */
    private boolean uebernehmeDaten() {
        try {
            controller.getModel().setRechnungsdatum(
                    LocalDate.parse(rechnungsdatumFeld.getText().strip(), DATUM));
        } catch (DateTimeParseException e) {
            FxMeldung.zeige(Meldung.fehler("Rechnungsdatum",
                    "Das 'Rechnungsdatum' ist ungültig. Format: TT.MM.JJJJ"), null);
            return false;
        }
        String zahlungsziel = zahlungszielFeld.getText().strip();
        if (zahlungsziel.isEmpty()) {
            controller.getModel().setZahlungsziel(null);
            return true;
        }
        try {
            controller.getModel().setZahlungsziel(LocalDate.parse(zahlungsziel, DATUM));
        } catch (DateTimeParseException e) {
            FxMeldung.zeige(Meldung.fehler("Zahlungsziel",
                    "Das 'Zahlungsziel' ist ungültig. Format: TT.MM.JJJJ"), null);
            return false;
        }
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
        schrittAnzeige.setText("Schritt " + (schritt.ordinal() + 1) + " von 5: "
                + schrittName(schritt));
        for (int i = 0; i < schrittMarkierungen.length; i++) {
            boolean aktuell = i == schritt.ordinal();
            schrittMarkierungen[i].getStyleClass().removeAll("schritt-aktuell", "schritt-inaktiv");
            schrittMarkierungen[i].getStyleClass().add(aktuell ? "schritt-aktuell" : "schritt-inaktiv");
        }
        zurueckKnopf.setDisable(schritt.ordinal() == 0);
        weiterKnopf.setDisable(schritt == WizardSchritt.SPEICHERN);
        speichernKnopf.setDisable(schritt != WizardSchritt.SPEICHERN);
        // Enter führt immer die sinnvollste Aktion des Schritts aus
        weiterKnopf.setDefaultButton(schritt != WizardSchritt.SPEICHERN);
        speichernKnopf.setDefaultButton(schritt == WizardSchritt.SPEICHERN);
    }

    private static String schrittName(WizardSchritt schritt) {
        return switch (schritt) {
            case KUNDE_WAEHLEN -> "Kunde auswählen";
            case POSITIONEN_ERFASSEN -> "Positionen erfassen";
            case DATEN_BESTAETIGEN -> "Rechnungsdatum und Zahlungsziel";
            case ZUSAMMENFASSUNG -> "Zusammenfassung prüfen";
            case SPEICHERN -> "Speichern";
        };
    }

    private void ladeKunden(String suchbegriff) {
        kundenListe.getItems().setAll(kundenService.suche(suchbegriff));
    }
}
