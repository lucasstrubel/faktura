package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.BelegAusgabe;
import de.lucasstrubel.faktura.dokumente.Belegtyp;
import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentCsvExport;
import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.dokumente.DokumentStatus;
import de.lucasstrubel.faktura.dokumente.Dokumentposition;
import de.lucasstrubel.faktura.dokumente.ERechnungExport;
import de.lucasstrubel.faktura.dokumente.Rechnung;
import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.ProduktService;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;

import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.util.List;

/**
 * Modulansicht Belege (D-F-06 bis F-08, F-14, F-15) als Master-Detail:
 * gefilterte Belegliste links, Einzelheiten des gewählten Belegs rechts.
 * Statuswechsel liegen in der Werkzeugleiste, die vier Ausgabewege (PDF,
 * E-Rechnung, Druck, Mail) in einem Menü — zuvor standen alle sieben Aktionen
 * gleichrangig nebeneinander.
 *
 * <p>Die Aktionsfreigabe je Status liefert unverändert der GUI-freie
 * {@link DokumentListenController}; die Ausgabe selbst übernimmt
 * {@link BelegAusgabe}, ausgeführt über {@link HintergrundAufgaben}.
 */
public class DokumentAnsichtController {

    /** Anzeige des Statusfilters für „kein Filter“ (F-06). */
    private static final String ALLE = "Alle";

    private final DokumentService dokumentService;
    private final KundenService kundenService;
    private final ProduktService produktService;
    private final DokumentCsvExport datenExport;
    private final ERechnungExport eRechnungExport;
    private final BelegAusgabe belegAusgabe;
    private final HintergrundAufgaben hintergrund;
    private final EreignisBus ereignisBus;
    private final DokumentListenController controller;

    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField suchfeld;
    @FXML private SplitPane aufteilung;
    @FXML private TableView<Dokument> tabelle;
    @FXML private TableColumn<Dokument, String> nummerSpalte;
    @FXML private TableColumn<Dokument, String> datumSpalte;
    @FXML private TableColumn<Dokument, String> kundeSpalte;
    @FXML private TableColumn<Dokument, String> bruttoSpalte;
    @FXML private TableColumn<Dokument, String> statusSpalte;
    @FXML private Label trefferAnzeige;
    @FXML private VBox detailBereich;
    @FXML private Button folgebelegKnopf;
    @FXML private Button versendenKnopf;
    @FXML private Button stornierenKnopf;
    @FXML private MenuButton ausgabeMenue;
    @FXML private MenuItem pdfEintrag;
    @FXML private MenuItem eRechnungEintrag;
    @FXML private MenuItem druckenEintrag;
    @FXML private MenuItem mailEintrag;

    public DokumentAnsichtController(DokumentService dokumentService,
                                     KundenService kundenService,
                                     ProduktService produktService,
                                     DokumentCsvExport datenExport,
                                     ERechnungExport eRechnungExport,
                                     BelegAusgabe belegAusgabe,
                                     HintergrundAufgaben hintergrund,
                                     EreignisBus ereignisBus) {
        this.dokumentService = dokumentService;
        this.kundenService = kundenService;
        this.produktService = produktService;
        this.datenExport = datenExport;
        this.eRechnungExport = eRechnungExport;
        this.belegAusgabe = belegAusgabe;
        this.hintergrund = hintergrund;
        this.ereignisBus = ereignisBus;
        this.controller = new DokumentListenController(dokumentService);
    }

    @FXML
    private void initialize() {
        richteFilterEin();
        richteTabelleEin();
        richteKontextmenueEin();

        tabelle.getSelectionModel().selectedItemProperty()
                .addListener((beobachtbar, alt, neu) -> {
                    aktualisiereAktionen();
                    zeigeEinzelheiten(neu);
                });

        aktualisiere();
        ereignisBus.abonniere(DatenBereich.DOKUMENTE, this::aktualisiere);
        // Kundenname und -nummer werden in der Belegliste angezeigt
        ereignisBus.abonniere(DatenBereich.KUNDEN, this::aktualisiere);

        tabelle.sceneProperty().addListener((beobachtbar, alt, szene) -> {
            if (szene != null) {
                registriereTastenkuerzel(szene);
            }
        });
    }

    private void richteFilterEin() {
        statusFilter.getItems().add(ALLE);
        for (DokumentStatus status : DokumentStatus.values()) {
            statusFilter.getItems().add(status.name());
        }
        statusFilter.getSelectionModel().selectFirst();
        statusFilter.valueProperty().addListener((beobachtbar, alt, neu) -> aktualisiere());
        suchfeld.textProperty().addListener((beobachtbar, alt, neu) -> aktualisiere());
    }

    private void richteTabelleEin() {
        nummerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getBelegnummer()));
        datumSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                TabellenFormat.datum(z.getValue().getDatum())));
        kundeSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getKundeName()));
        bruttoSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                TabellenFormat.betrag(z.getValue().getSummeBrutto())));
        statusSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                z.getValue().getStatus().name()));
        statusSpalte.setCellFactory(Bausteine.statusZelle());
        Bausteine.alsNummernspalte(nummerSpalte);
        Bausteine.alsBetragsspalte(bruttoSpalte);
        Bausteine.passeSpaltenAn(tabelle);

        tabelle.setPlaceholder(Bausteine.leerzustand(Feather.FILE_TEXT,
                "Keine Belege",
                "Mit „Neue Rechnung“ starten Sie die geführte Erstellung."));
    }

    /** Dieselben Aktionen wie in der Werkzeugleiste auch per Rechtsklick. */
    private void richteKontextmenueEin() {
        MenuItem folgebeleg = new MenuItem("Folgebeleg erzeugen");
        folgebeleg.setOnAction(e -> erzeugeFolgebeleg());
        MenuItem versenden = new MenuItem("Versenden");
        versenden.setOnAction(e -> versende());
        MenuItem stornieren = new MenuItem("Stornieren");
        stornieren.setOnAction(e -> storniere());
        MenuItem pdf = new MenuItem("Als PDF speichern…");
        pdf.setOnAction(e -> exportierePdf());

        ContextMenu menue = new ContextMenu(folgebeleg, versenden, stornieren,
                new SeparatorMenuItem(), pdf);
        // Nur bei ausgewähltem Beleg anbieten
        tabelle.setContextMenu(menue);
        menue.setOnShowing(e -> {
            boolean ohneAuswahl = auswahl() == null;
            menue.getItems().forEach(eintrag -> eintrag.setDisable(ohneAuswahl));
        });
    }

    private void registriereTastenkuerzel(Scene szene) {
        szene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                this::oeffneWizard);
        szene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN),
                () -> suchfeld.requestFocus());
        szene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), this::aktualisiere);
    }

    private Dokument auswahl() {
        return tabelle.getSelectionModel().getSelectedItem();
    }

    private DokumentStatus gewaehlterStatus() {
        String wert = statusFilter.getValue();
        return wert == null || ALLE.equals(wert) ? null : DokumentStatus.valueOf(wert);
    }

    private void aktualisiere() {
        Dokument vorherAusgewaehlt = auswahl();
        DokumentStatus status = gewaehlterStatus();
        String suchbegriff = suchfeld.getText() == null ? "" : suchfeld.getText().strip();

        List<Dokument> liste = controller.gefiltert(status, suchbegriff);
        tabelle.getItems().setAll(liste);
        int gesamt = controller.gefiltert(null, null).size();

        if (gesamt == 0) {
            trefferAnzeige.setText("Noch keine Belege vorhanden");
        } else if (liste.isEmpty()) {
            trefferAnzeige.setText("Kein Beleg passt zur Auswahl");
        } else if (liste.size() == gesamt) {
            trefferAnzeige.setText(gesamt + " Belege");
        } else {
            trefferAnzeige.setText(liste.size() + " von " + gesamt + " Belegen");
        }

        // Auswahl über eine Aktualisierung hinweg halten (die Objekte sind neu geladen)
        stelleAuswahlWiederHer(vorherAusgewaehlt);
        aktualisiereAktionen();
        zeigeEinzelheiten(auswahl());
    }

    private void stelleAuswahlWiederHer(Dokument vorher) {
        if (vorher == null) {
            return;
        }
        tabelle.getItems().stream()
                .filter(d -> d.getBelegnummer().equals(vorher.getBelegnummer()))
                .findFirst()
                .ifPresent(d -> tabelle.getSelectionModel().select(d));
    }

    /** Aktiviert/deaktiviert die Belegaktionen gemäß Status (F-08, F-14). */
    private void aktualisiereAktionen() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            folgebelegKnopf.setDisable(true);
            versendenKnopf.setDisable(true);
            stornierenKnopf.setDisable(true);
            ausgabeMenue.setDisable(true);
            return;
        }
        BelegAktionen verfuegbar = controller.aktionenFuer(dokument);
        folgebelegKnopf.setDisable(dokument.belegtyp() == Belegtyp.RECHNUNG);
        versendenKnopf.setDisable(!verfuegbar.aenderbar());
        stornierenKnopf.setDisable(!verfuegbar.stornierbar());

        ausgabeMenue.setDisable(false);
        pdfEintrag.setDisable(!verfuegbar.pdfExport());
        druckenEintrag.setDisable(!verfuegbar.pdfExport());
        mailEintrag.setDisable(!verfuegbar.pdfExport());
        // E-Rechnung nur für offene oder versendete Rechnungen (EN 16931)
        eRechnungEintrag.setDisable(!(dokument instanceof Rechnung
                && (dokument.getStatus() == DokumentStatus.OFFEN
                        || dokument.getStatus() == DokumentStatus.VERSENDET)));
    }

    /** Baut den Detailbereich zum gewählten Beleg auf (Kopfdaten, Positionen, Summen). */
    private void zeigeEinzelheiten(Dokument dokument) {
        detailBereich.getChildren().clear();
        if (dokument == null) {
            detailBereich.getChildren().add(Bausteine.leerzustand(Feather.INFO,
                    "Kein Beleg gewählt",
                    "Wählen Sie links einen Beleg, um seine Einzelheiten zu sehen."));
            return;
        }

        Label titel = new Label(dokument.belegtyp().anzeigename() + " " + dokument.getBelegnummer());
        titel.getStyleClass().add("detail-titel");
        titel.setWrapText(true);

        Label status = new Label(dokument.getStatus().name());
        status.getStyleClass().addAll("status-abzeichen", "status-" + dokument.getStatus().name());

        detailBereich.getChildren().addAll(titel, status,
                zeile("Datum", TabellenFormat.datum(dokument.getDatum())),
                zeile("Kunde", dokument.getKundeName()),
                zeile("Anschrift", dokument.getKundeAnschrift()));

        if (dokument.getVorgaengerNr() != null) {
            detailBereich.getChildren().add(zeile("Vorgänger", dokument.getVorgaengerNr()));
        }
        if (dokument instanceof Rechnung rechnung && rechnung.getZahlungsziel() != null) {
            detailBereich.getChildren().add(
                    zeile("Zahlungsziel", TabellenFormat.datum(rechnung.getZahlungsziel())));
        }

        Label positionenTitel = new Label("Positionen");
        positionenTitel.getStyleClass().add("detail-beschriftung");
        detailBereich.getChildren().add(positionenTitel);
        for (Dokumentposition position : dokument.getPositionen()) {
            detailBereich.getChildren().add(positionszeile(
                    position.getMenge() + " × " + position.getBezeichnung(),
                    TabellenFormat.betrag(position.getPositionssummeNetto())));
        }

        detailBereich.getChildren().addAll(
                zeile("Netto", TabellenFormat.betrag(dokument.getSummeNetto())),
                zeile("Steuer", TabellenFormat.betrag(dokument.getSummeSteuer())),
                summenzeile("Brutto", TabellenFormat.betrag(dokument.getSummeBrutto())));
    }

    /**
     * Beschriftung links, Wert rechts. Die Beschriftung darf nie schrumpfen
     * (sonst steht dort „K…“ statt „Kunde“); umbrochen wird ausschließlich der
     * Wert, der den restlichen Platz bekommt.
     */
    private static HBox zeile(String beschriftung, String wert) {
        Label links = new Label(beschriftung);
        links.getStyleClass().add("detail-beschriftung");
        links.setMinWidth(Region.USE_PREF_SIZE);

        Label rechts = new Label(wert == null || wert.isBlank() ? "—" : wert);
        rechts.getStyleClass().add("detail-wert");
        rechts.setWrapText(true);
        rechts.setMaxWidth(Double.MAX_VALUE);
        rechts.setAlignment(Pos.TOP_RIGHT);
        rechts.setTextAlignment(TextAlignment.RIGHT);
        HBox.setHgrow(rechts, Priority.ALWAYS);

        HBox zeile = new HBox(10, links, rechts);
        zeile.setAlignment(Pos.TOP_LEFT);
        return zeile;
    }

    /**
     * Positionszeile: Hier ist die Bezeichnung lang und der Betrag kurz —
     * also genau umgekehrt. Umbrochen wird die Bezeichnung, der Betrag bleibt
     * in einem Stück.
     */
    private static HBox positionszeile(String bezeichnung, String betrag) {
        Label links = new Label(bezeichnung);
        links.getStyleClass().add("detail-beschriftung");
        links.setWrapText(true);
        links.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(links, Priority.ALWAYS);

        Label rechts = new Label(betrag);
        rechts.getStyleClass().add("detail-wert");
        rechts.setMinWidth(Region.USE_PREF_SIZE);

        HBox zeile = new HBox(10, links, rechts);
        zeile.setAlignment(Pos.TOP_LEFT);
        return zeile;
    }

    private static HBox summenzeile(String beschriftung, String wert) {
        HBox zeile = zeile(beschriftung, wert);
        zeile.getChildren().forEach(knoten -> knoten.getStyleClass().add("detail-summe"));
        return zeile;
    }

    @FXML
    private void oeffneWizard() {
        RechnungsWizardController wizardController =
                new RechnungsWizardController(dokumentService, kundenService, produktService);
        new RechnungsWizardDialog(fenster(), wizardController, kundenService, produktService)
                .showAndWait();
    }

    @FXML
    private void oeffneBelegDialog() {
        new BelegDialog(fenster(), dokumentService, kundenService, produktService).showAndWait();
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
        File ziel = zielDatei(dokument.getBelegnummer() + ".pdf");
        if (ziel != null) {
            hintergrund.starte("PDF wird exportiert…", ausgabeMenue,
                    () -> belegAusgabe.exportierePdf(dokument.getBelegnummer(), ziel.toPath()),
                    () -> FxMeldung.zeige(
                            Meldung.erfolg("Das PDF wurde exportiert nach " + ziel), null));
        }
    }

    /** Strukturierte E-Rechnung nach EN 16931 als CII-XML (Roadmap E-Rechnungspflicht). */
    @FXML
    private void exportiereERechnung() {
        Dokument dokument = auswahl();
        if (!(dokument instanceof Rechnung rechnung)) {
            return;
        }
        File ziel = zielDatei(rechnung.getBelegnummer() + "-erechnung.xml");
        if (ziel != null) {
            hintergrund.starte("E-Rechnung wird erzeugt…", ausgabeMenue,
                    () -> eRechnungExport.exportiereXml(rechnung, ziel.toPath()),
                    () -> FxMeldung.zeige(Meldung.erfolg(
                            "Die E-Rechnung (EN 16931) wurde exportiert nach " + ziel), null));
        }
    }

    /** Vollständiger Datenexport aller Belege als CSV (Q-08, IF-04). */
    @FXML
    private void exportiereDaten() {
        File ziel = zielDatei("dokumente.csv");
        if (ziel != null) {
            hintergrund.starte("Belegdaten werden exportiert…", null,
                    () -> datenExport.exportiereCsv(ziel.toPath()),
                    () -> FxMeldung.zeige(Meldung.erfolg(
                            "Die Belegdaten wurden exportiert nach " + ziel), null));
        }
    }

    /** Optionaler Druck über das Betriebssystem (IF-02). */
    @FXML
    private void drucke() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        hintergrund.starte("Beleg wird gedruckt…", ausgabeMenue,
                () -> belegAusgabe.drucke(dokument.getBelegnummer()), null);
    }

    /** Optionaler Versand über den Standard-E-Mail-Client (IF-03). */
    @FXML
    private void sendePerMail() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        hintergrund.starte("Mailprogramm wird geöffnet…", ausgabeMenue,
                () -> belegAusgabe.sendePerMail(dokument.getBelegnummer()), null);
    }

    /** Speicherdialog mit vorbelegtem Dateinamen; {@code null}, wenn abgebrochen. */
    private File zielDatei(String vorschlag) {
        FileChooser auswahlDialog = new FileChooser();
        auswahlDialog.setInitialFileName(vorschlag);
        return auswahlDialog.showSaveDialog(fenster());
    }

    private javafx.stage.Window fenster() {
        return tabelle.getScene().getWindow();
    }
}
