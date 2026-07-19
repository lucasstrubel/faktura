package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktCsvExport;
import de.lucasstrubel.faktura.produkte.ProduktVerwaltungsService;

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
import java.math.BigDecimal;
import java.util.List;

/**
 * Modulansicht Produktverwaltung (D-F-03 bis F-05): sortierte Liste mit
 * Suchfeld; Anlegen und Bearbeiten erfolgen über die modale Formular-Maske
 * {@link ProduktFormularDialog} (Knopf oder Doppelklick).
 */
public class ProduktAnsichtController {

    private final ProduktVerwaltungsService service;
    private final StammdatenController controller;
    private final ProduktCsvExport csvExport;
    private final EreignisBus ereignisBus;

    @FXML private TextField suchfeld;
    @FXML private TableView<Produkt> tabelle;
    @FXML private TableColumn<Produkt, String> nummerSpalte;
    @FXML private TableColumn<Produkt, String> bezeichnungSpalte;
    @FXML private TableColumn<Produkt, String> preisSpalte;
    @FXML private TableColumn<Produkt, String> steuersatzSpalte;
    @FXML private TableColumn<Produkt, String> einheitSpalte;
    @FXML private Label trefferAnzeige;
    @FXML private Button bearbeitenKnopf;
    @FXML private Button loeschenKnopf;

    public ProduktAnsichtController(ProduktVerwaltungsService service,
                                    StammdatenController controller,
                                    ProduktCsvExport csvExport,
                                    EreignisBus ereignisBus) {
        this.service = service;
        this.controller = controller;
        this.csvExport = csvExport;
        this.ereignisBus = ereignisBus;
    }

    @FXML
    private void initialize() {
        nummerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getProduktnummer()));
        bezeichnungSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getBezeichnung()));
        preisSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                TabellenFormat.betrag(z.getValue().getEinzelpreisNetto())));
        steuersatzSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                prozent(z.getValue().getSteuersatz())));
        einheitSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                z.getValue().getEinheit() == null ? "" : z.getValue().getEinheit()));

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
        ereignisBus.abonniere(DatenBereich.PRODUKTE, this::aktualisiere);
    }

    private void aktualisiere() {
        List<Produkt> liste = controller.produkteListe(suchfeld.getText());
        tabelle.getItems().setAll(liste);
        int gesamt = controller.produkteListe("").size();
        String suchbegriff = suchfeld.getText() == null ? "" : suchfeld.getText().trim();
        trefferAnzeige.setText(suchbegriff.isEmpty()
                ? gesamt + " Produkte"
                : liste.size() + " von " + gesamt + " Produkten für \"" + suchbegriff + "\"");
    }

    @FXML
    private void legeNeuAn() {
        new ProduktFormularDialog(tabelle.getScene().getWindow(), service, null).showAndWait();
    }

    @FXML
    private void bearbeite() {
        Produkt produkt = tabelle.getSelectionModel().getSelectedItem();
        if (produkt != null) {
            new ProduktFormularDialog(tabelle.getScene().getWindow(), service, produkt).showAndWait();
        }
    }

    @FXML
    private void loesche() {
        Produkt produkt = tabelle.getSelectionModel().getSelectedItem();
        if (produkt == null || !FxMeldung.bestaetige("Produkt löschen",
                "Produkt " + produkt.getProduktnummer() + " wirklich dauerhaft löschen?")) {
            return;
        }
        FxMeldung.mitFehlerbehandlung(null, () -> {
            service.loescheProdukt(produkt.getProduktnummer());
            FxMeldung.zeige(Meldung.erfolg("Das Produkt wurde gelöscht."), null);
        });
    }

    @FXML
    private void exportiere() {
        FileChooser auswahl = new FileChooser();
        auswahl.setInitialFileName("produkte.csv");
        File ziel = auswahl.showSaveDialog(tabelle.getScene().getWindow());
        if (ziel != null) {
            FxMeldung.mitFehlerbehandlung(null, () -> {
                csvExport.exportiereCsv(ziel.toPath());
                FxMeldung.zeige(Meldung.erfolg(
                        "Die Produktstammdaten wurden exportiert nach " + ziel), null);
            });
        }
    }

    private static String prozent(BigDecimal steuersatz) {
        return steuersatz.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + " %";
    }
}
