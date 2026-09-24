package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.dokumente.Kennzahlen;
import de.lucasstrubel.faktura.dokumente.KennzahlenDienst;
import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.kunden.KundenVerwaltungsService;
import de.lucasstrubel.faktura.produkte.ProduktService;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;

/**
 * Übersichtsansicht (D-F-18): beantwortet beim Öffnen die Fragen, die ein
 * Kleinstunternehmer zuerst stellt — wie viel Geld steht offen, was ist
 * überfällig, wie viel wurde dieses Jahr fakturiert — und bietet die drei
 * häufigsten Aktionen direkt an.
 *
 * <p>Die Auswertung selbst liegt im GUI-freien {@link KennzahlenDienst}.
 */
public class UebersichtAnsichtController {

    private final KennzahlenDienst kennzahlenDienst;
    private final DokumentService dokumentService;
    private final KundenService kundenService;
    private final KundenVerwaltungsService kundenVerwaltung;
    private final ProduktService produktService;
    private final EreignisBus ereignisBus;

    @FXML private Label untertitel;
    @FXML private FlowPane kennzahlen;
    @FXML private TableView<Dokument> letzteBelege;
    @FXML private TableColumn<Dokument, String> nummerSpalte;
    @FXML private TableColumn<Dokument, String> typSpalte;
    @FXML private TableColumn<Dokument, String> datumSpalte;
    @FXML private TableColumn<Dokument, String> kundeSpalte;
    @FXML private TableColumn<Dokument, String> bruttoSpalte;
    @FXML private TableColumn<Dokument, String> statusSpalte;
    @FXML private Button neueRechnungKnopf;

    public UebersichtAnsichtController(KennzahlenDienst kennzahlenDienst,
                                       DokumentService dokumentService,
                                       KundenService kundenService,
                                       KundenVerwaltungsService kundenVerwaltung,
                                       ProduktService produktService,
                                       EreignisBus ereignisBus) {
        this.kennzahlenDienst = kennzahlenDienst;
        this.dokumentService = dokumentService;
        this.kundenService = kundenService;
        this.kundenVerwaltung = kundenVerwaltung;
        this.produktService = produktService;
        this.ereignisBus = ereignisBus;
    }

    @FXML
    private void initialize() {
        nummerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getBelegnummer()));
        typSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                Bausteine.belegart(z.getValue())));
        datumSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                TabellenFormat.datum(z.getValue().getDatum())));
        kundeSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getKundeName()));
        bruttoSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                TabellenFormat.betrag(z.getValue().getSummeBrutto())));
        statusSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                Bausteine.anzeigestatus(z.getValue())));
        statusSpalte.setCellFactory(Bausteine.statusZelle());
        Bausteine.alsNummernspalte(nummerSpalte);
        Bausteine.alsBetragsspalte(bruttoSpalte);
        Bausteine.passeSpaltenAn(letzteBelege);

        letzteBelege.setPlaceholder(Bausteine.leerzustand(Feather.FILE_TEXT,
                "Noch keine Belege",
                "Erstellen Sie mit „Neue Rechnung“ Ihren ersten Beleg."));

        // Ohne dieses Minimum zwingt die Kartenreihe das Fenster auf ihre
        // Gesamtbreite, statt in eine zweite Reihe umzubrechen.
        kennzahlen.setMinWidth(0);

        aktualisiere();
        // Kennzahlen hängen an Belegen; Kundennamen erscheinen in der Liste
        ereignisBus.abonniere(DatenBereich.DOKUMENTE, this::aktualisiere);
        ereignisBus.abonniere(DatenBereich.KUNDEN, this::aktualisiere);
    }

    private void aktualisiere() {
        Kennzahlen zahlen = kennzahlenDienst.ermittle(LocalDate.now());

        untertitel.setText(zahlen.belegeGesamt() == 0
                ? "Willkommen — legen Sie zunächst Kunden und Produkte an."
                : zahlen.belegeGesamt() + " Belege insgesamt");

        kennzahlen.getChildren().setAll(
                karte(Feather.CLOCK, "Offener Betrag",
                        TabellenFormat.betrag(zahlen.offenerBetrag()),
                        zahlen.offeneAnzahl() + " unbezahlte Rechnungen", "betonend"),
                karte(Feather.ALERT_TRIANGLE, "Überfällig",
                        TabellenFormat.betrag(zahlen.ueberfaelligBetrag()),
                        zahlen.ueberfaelligAnzahl() + " Rechnungen über Zahlungsziel",
                        zahlen.ueberfaelligAnzahl() > 0 ? "warnend" : null),
                karte(Feather.TRENDING_UP, "Umsatz " + LocalDate.now().getYear(),
                        TabellenFormat.betrag(zahlen.umsatzJahr()),
                        "brutto, ohne stornierte Rechnungen", null),
                karte(Feather.FILE_TEXT, "Belege gesamt",
                        String.valueOf(zahlen.belegeGesamt()),
                        "über alle Belegarten", null));

        letzteBelege.getItems().setAll(zahlen.letzteBelege());
    }

    /**
     * Eine Kennzahlenkarte; {@code zusatzStil} hebt sie hervor
     * ({@code warnend} für Überfälliges, {@code betonend} für Offenes).
     */
    private static VBox karte(Feather symbol, String titel, String wert,
                              String zusatz, String zusatzStil) {
        Label titelLabel = new Label(titel);
        titelLabel.getStyleClass().add("kennzahl-titel");
        HBox kopf = new HBox(8, new FontIcon(symbol), titelLabel);
        kopf.setAlignment(Pos.CENTER_LEFT);

        Label wertLabel = new Label(wert);
        wertLabel.getStyleClass().add("kennzahl-wert");
        Label zusatzLabel = new Label(zusatz);
        zusatzLabel.getStyleClass().add("kennzahl-zusatz");

        VBox karte = new VBox(4, kopf, wertLabel, zusatzLabel);
        karte.getStyleClass().add("kennzahl-karte");
        if (zusatzStil != null) {
            karte.getStyleClass().add(zusatzStil);
        }
        return karte;
    }

    @FXML
    private void neueRechnung() {
        RechnungsWizardController wizardController =
                new RechnungsWizardController(dokumentService, kundenService, produktService);
        new RechnungsWizardDialog(fenster(), wizardController, kundenService, produktService)
                .showAndWait();
    }

    @FXML
    private void neuerBeleg() {
        new BelegDialog(fenster(), dokumentService, kundenService, produktService).showAndWait();
    }

    @FXML
    private void neuerKunde() {
        new KundenFormularDialog(fenster(), kundenVerwaltung, null).showAndWait();
    }

    private javafx.stage.Window fenster() {
        return neueRechnungKnopf.getScene().getWindow();
    }
}
