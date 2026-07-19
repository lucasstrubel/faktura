package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenCsvExport;
import de.lucasstrubel.faktura.kunden.KundenVerwaltungsService;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * Modulansicht Kundenverwaltung (D-F-03 bis F-05): sortierte Liste mit
 * Suchfeld; Anlegen und Bearbeiten erfolgen über die modale Formular-Maske
 * {@link KundenFormularDialog} (Knopf oder Doppelklick).
 */
public class KundenAnsichtController {

    private final KundenVerwaltungsService service;
    private final StammdatenController controller;
    private final KundenCsvExport csvExport;
    private final EreignisBus ereignisBus;

    @FXML private TextField suchfeld;
    @FXML private TableView<Kunde> tabelle;
    @FXML private TableColumn<Kunde, String> nummerSpalte;
    @FXML private TableColumn<Kunde, String> nameSpalte;
    @FXML private TableColumn<Kunde, String> strasseSpalte;
    @FXML private TableColumn<Kunde, String> plzSpalte;
    @FXML private TableColumn<Kunde, String> ortSpalte;
    @FXML private Label trefferAnzeige;
    @FXML private Button bearbeitenKnopf;
    @FXML private Button loeschenKnopf;

    public KundenAnsichtController(KundenVerwaltungsService service,
                                   StammdatenController controller,
                                   KundenCsvExport csvExport,
                                   EreignisBus ereignisBus) {
        this.service = service;
        this.controller = controller;
        this.csvExport = csvExport;
        this.ereignisBus = ereignisBus;
    }

    @FXML
    private void initialize() {
        nummerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getKundennummer()));
        nameSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getName()));
        strasseSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getStrasse()));
        plzSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getPlz()));
        ortSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getOrt()));

        bearbeitenKnopf.disableProperty().bind(
                tabelle.getSelectionModel().selectedItemProperty().isNull());
        loeschenKnopf.disableProperty().bind(
                tabelle.getSelectionModel().selectedItemProperty().isNull());
        tabelle.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                bearbeite();
            }
        });
        suchfeld.textProperty().addListener((beobachtbar, alt, neu) -> aktualisiere());

        aktualisiere();
        ereignisBus.abonniere(DatenBereich.KUNDEN, this::aktualisiere);
    }

    private void aktualisiere() {
        List<Kunde> liste = controller.kundenListe(suchfeld.getText());
        tabelle.getItems().setAll(liste);
        int gesamt = controller.kundenListe("").size();
        String suchbegriff = suchfeld.getText() == null ? "" : suchfeld.getText().trim();
        trefferAnzeige.setText(suchbegriff.isEmpty()
                ? gesamt + " Kunden"
                : liste.size() + " von " + gesamt + " Kunden für \"" + suchbegriff + "\"");
    }

    @FXML
    private void legeNeuAn() {
        new KundenFormularDialog(tabelle.getScene().getWindow(), service, null).showAndWait();
    }

    @FXML
    private void bearbeite() {
        Kunde kunde = tabelle.getSelectionModel().getSelectedItem();
        if (kunde != null) {
            new KundenFormularDialog(tabelle.getScene().getWindow(), service, kunde).showAndWait();
        }
    }

    @FXML
    private void loesche() {
        Kunde kunde = tabelle.getSelectionModel().getSelectedItem();
        if (kunde == null || !FxMeldung.bestaetige("Kunde löschen",
                "Kunde " + kunde.getKundennummer() + " wirklich dauerhaft löschen?")) {
            return;
        }
        FxMeldung.mitFehlerbehandlung(null, () -> {
            service.loescheKunde(kunde.getKundennummer());
            FxMeldung.zeige(Meldung.erfolg("Der Kunde wurde gelöscht."), null);
        });
    }

    @FXML
    private void exportiere() {
        FileChooser auswahl = new FileChooser();
        auswahl.setInitialFileName("kunden.csv");
        File ziel = auswahl.showSaveDialog(tabelle.getScene().getWindow());
        if (ziel != null) {
            FxMeldung.mitFehlerbehandlung(null, () -> {
                csvExport.exportiereCsv(ziel.toPath());
                FxMeldung.zeige(Meldung.erfolg(
                        "Die Kundenstammdaten wurden exportiert nach " + ziel), null);
            });
        }
    }
}
