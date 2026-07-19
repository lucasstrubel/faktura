package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.Belegtyp;
import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentCsvExport;
import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.dokumente.DokumentStatus;
import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.ProduktService;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Modulansicht Dokumente (D-F-06 bis F-08, F-14, F-15): Dokumentliste mit
 * Statusfilter, Belegaktionen (PDF-Export, optional Druck und E-Mail),
 * geführte Rechnungserstellung (Wizard) und Stornierung mit
 * Bestätigungsdialog. Die Aktionsfreigabe je Status liefert der GUI-freie
 * {@link DokumentListenController}.
 */
public class DokumentAnsichtController {

    /** Anzeige des Statusfilters für "kein Filter" (F-06). */
    private static final String ALLE = "Alle";

    private final DokumentService dokumentService;
    private final KundenService kundenService;
    private final ProduktService produktService;
    private final DokumentCsvExport datenExport;
    private final EreignisBus ereignisBus;
    private final DokumentListenController controller;

    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Dokument> tabelle;
    @FXML private TableColumn<Dokument, String> nummerSpalte;
    @FXML private TableColumn<Dokument, String> typSpalte;
    @FXML private TableColumn<Dokument, String> datumSpalte;
    @FXML private TableColumn<Dokument, String> kundeSpalte;
    @FXML private TableColumn<Dokument, String> bruttoSpalte;
    @FXML private TableColumn<Dokument, String> statusSpalte;
    @FXML private Label trefferAnzeige;
    @FXML private Button folgebelegKnopf;
    @FXML private Button versendenKnopf;
    @FXML private Button stornierenKnopf;
    @FXML private Button pdfKnopf;
    @FXML private Button druckenKnopf;
    @FXML private Button mailKnopf;

    public DokumentAnsichtController(DokumentService dokumentService,
                                     KundenService kundenService,
                                     ProduktService produktService,
                                     DokumentCsvExport datenExport,
                                     EreignisBus ereignisBus) {
        this.dokumentService = dokumentService;
        this.kundenService = kundenService;
        this.produktService = produktService;
        this.datenExport = datenExport;
        this.ereignisBus = ereignisBus;
        this.controller = new DokumentListenController(dokumentService);
    }

    @FXML
    private void initialize() {
        statusFilter.getItems().add(ALLE);
        for (DokumentStatus status : DokumentStatus.values()) {
            statusFilter.getItems().add(status.name());
        }
        statusFilter.getSelectionModel().selectFirst();
        statusFilter.valueProperty().addListener((beobachtbar, alt, neu) -> aktualisiere());

        nummerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getBelegnummer()));
        typSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().belegtyp().anzeigename()));
        datumSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                TabellenFormat.datum(z.getValue().getDatum())));
        kundeSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                z.getValue().getKundeName() + " (" + z.getValue().getKundenReferenz() + ")"));
        bruttoSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                TabellenFormat.betrag(z.getValue().getSummeBrutto())));
        statusSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getStatus().name()));
        statusSpalte.setCellFactory(spalte -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean leer) {
                super.updateItem(status, leer);
                getStyleClass().removeIf(stil -> stil.startsWith("status-"));
                setText(leer ? null : status);
                if (!leer && status != null) {
                    getStyleClass().add("status-" + status);
                }
            }
        });

        tabelle.getSelectionModel().selectedItemProperty()
                .addListener((beobachtbar, alt, neu) -> aktualisiereAktionen());

        aktualisiere();
        ereignisBus.abonniere(DatenBereich.DOKUMENTE, this::aktualisiere);
        // Kundenname und -nummer werden in der Belegliste angezeigt
        ereignisBus.abonniere(DatenBereich.KUNDEN, this::aktualisiere);
    }

    private Dokument auswahl() {
        return tabelle.getSelectionModel().getSelectedItem();
    }

    private DokumentStatus gewaehlterStatus() {
        String wert = statusFilter.getValue();
        return wert == null || ALLE.equals(wert) ? null : DokumentStatus.valueOf(wert);
    }

    private void aktualisiere() {
        DokumentStatus status = gewaehlterStatus();
        List<Dokument> liste = controller.gefiltert(status);
        tabelle.getItems().setAll(liste);
        int gesamt = status == null ? liste.size() : controller.gefiltert(null).size();
        if (gesamt == 0) {
            trefferAnzeige.setText("Noch keine Belege vorhanden");
        } else if (liste.isEmpty()) {
            trefferAnzeige.setText("Keine Belege im Status " + status);
        } else {
            trefferAnzeige.setText(liste.size() + " von " + gesamt + " Belegen");
        }
        aktualisiereAktionen();
    }

    /** Aktiviert/deaktiviert die Belegaktionen gemäß Status (F-08, F-14). */
    private void aktualisiereAktionen() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            for (Button knopf : List.of(folgebelegKnopf, versendenKnopf, stornierenKnopf,
                    pdfKnopf, druckenKnopf, mailKnopf)) {
                knopf.setDisable(true);
            }
            return;
        }
        BelegAktionen verfuegbar = controller.aktionenFuer(dokument);
        folgebelegKnopf.setDisable(dokument.belegtyp() == Belegtyp.RECHNUNG);
        versendenKnopf.setDisable(!verfuegbar.aenderbar());
        stornierenKnopf.setDisable(!verfuegbar.stornierbar());
        pdfKnopf.setDisable(!verfuegbar.pdfExport());
        druckenKnopf.setDisable(!verfuegbar.pdfExport());
        mailKnopf.setDisable(!verfuegbar.pdfExport());
    }

    @FXML
    private void oeffneWizard() {
        RechnungsWizardController wizardController =
                new RechnungsWizardController(dokumentService, kundenService, produktService);
        new RechnungsWizardDialog(tabelle.getScene().getWindow(), wizardController,
                kundenService, produktService).showAndWait();
    }

    @FXML
    private void oeffneBelegDialog() {
        new BelegDialog(tabelle.getScene().getWindow(), dokumentService,
                kundenService, produktService).showAndWait();
    }

    @FXML
    private void erzeugeFolgebeleg() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        FxMeldung.mitFehlerbehandlung(null, () -> {
            Dokument folgebeleg = dokumentService.erzeugeFolgebeleg(dokument.getBelegnummer());
            FxMeldung.zeige(Meldung.erfolg(folgebeleg.belegtyp().anzeigename() + " "
                    + folgebeleg.getBelegnummer() + " wurde aus " + dokument.getBelegnummer()
                    + " erzeugt."), null);
        });
    }

    @FXML
    private void versende() {
        Dokument dokument = auswahl();
        if (dokument == null || !FxMeldung.bestaetige("Versenden",
                "Beleg " + dokument.getBelegnummer() + " als versendet markieren?\n"
                        + "Danach sind keine inhaltlichen Änderungen mehr möglich (GR-02).")) {
            return;
        }
        FxMeldung.mitFehlerbehandlung(null, () -> {
            dokumentService.versende(dokument.getBelegnummer());
            FxMeldung.zeige(Meldung.erfolg("Der Beleg " + dokument.getBelegnummer()
                    + " ist jetzt im Status VERSENDET."), null);
        });
    }

    /** Stornierung mit Bestätigungsdialog: Rechnungsnummer und Bruttosumme (F-15). */
    @FXML
    private void storniere() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        boolean bestaetigt = FxMeldung.bestaetige("Rechnung stornieren",
                "Rechnung " + dokument.getBelegnummer() + " über "
                        + TabellenFormat.betrag(dokument.getSummeBrutto())
                        + " (brutto) wirklich stornieren?");
        FxMeldung.zeige(controller.storniere(dokument.getBelegnummer(), bestaetigt), null);
    }

    @FXML
    private void exportierePdf() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        FileChooser auswahlDialog = new FileChooser();
        auswahlDialog.setInitialFileName(dokument.getBelegnummer() + ".pdf");
        File ziel = auswahlDialog.showSaveDialog(tabelle.getScene().getWindow());
        if (ziel != null) {
            FxMeldung.mitFehlerbehandlung(null, () -> {
                dokumentService.exportierePdf(dokument.getBelegnummer(), ziel.toPath());
                FxMeldung.zeige(Meldung.erfolg("Das PDF wurde exportiert nach " + ziel), null);
            });
        }
    }

    /** Vollständiger Datenexport aller Belege als CSV (Q-08, IF-04). */
    @FXML
    private void exportiereDaten() {
        FileChooser auswahlDialog = new FileChooser();
        auswahlDialog.setInitialFileName("dokumente.csv");
        File ziel = auswahlDialog.showSaveDialog(tabelle.getScene().getWindow());
        if (ziel != null) {
            FxMeldung.mitFehlerbehandlung(null, () -> {
                datenExport.exportiereCsv(ziel.toPath());
                FxMeldung.zeige(Meldung.erfolg(
                        "Die Belegdaten wurden exportiert nach " + ziel), null);
            });
        }
    }

    /** Optionaler Druck über das Betriebssystem (IF-02). */
    @FXML
    private void drucke() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        try {
            Path temp = Files.createTempFile(dokument.getBelegnummer() + "-", ".pdf");
            dokumentService.exportierePdf(dokument.getBelegnummer(), temp);
            Desktop.getDesktop().print(temp.toFile());
        } catch (Exception e) {
            FxMeldung.zeige(Meldung.fehler(null,
                    "Drucken nicht möglich: " + e.getMessage()), null);
        }
    }

    /** Optionaler Versand über den Standard-E-Mail-Client (IF-03). */
    @FXML
    private void sendePerMail() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        try {
            Path temp = Files.createTempFile(dokument.getBelegnummer() + "-", ".pdf");
            dokumentService.exportierePdf(dokument.getBelegnummer(), temp);
            String betreff = URLEncoder.encode(dokument.belegtyp().anzeigename() + " "
                    + dokument.getBelegnummer(), StandardCharsets.UTF_8).replace("+", "%20");
            String text = URLEncoder.encode("Bitte das exportierte PDF anhängen:\n" + temp,
                    StandardCharsets.UTF_8).replace("+", "%20");
            Desktop.getDesktop().mail(new URI("mailto:?subject=" + betreff + "&body=" + text));
        } catch (Exception e) {
            FxMeldung.zeige(Meldung.fehler(null,
                    "E-Mail-Client konnte nicht geöffnet werden: " + e.getMessage()), null);
        }
    }
}
