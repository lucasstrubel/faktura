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
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import java.math.BigDecimal;

/**
 * Übersichtsansicht (D-F-18): beantwortet beim Öffnen die Fragen, die ein
 * Kleinstunternehmer zuerst stellt — wie viel Geld steht offen, was ist
 * überfällig, wie viel wurde dieses Jahr fakturiert — und bietet die drei
 * häufigsten Aktionen direkt an.
 *
 * <p>Die Auswertung selbst liegt im GUI-freien {@link KennzahlenDienst} und
 * läuft über {@link HintergrundAufgaben}, damit sie den Start nicht aufhält (Q-04).
 */
public class UebersichtAnsichtController {

    private final KennzahlenDienst kennzahlenDienst;
    private final DokumentService dokumentService;
    private final KundenService kundenService;
    private final KundenVerwaltungsService kundenVerwaltung;
    private final ProduktService produktService;
    private final EreignisBus ereignisBus;
    private final HintergrundAufgaben hintergrund;

    /** Leerzustand der Belegliste; erst nach der ersten Auswertung gezeigt. */
    private final Node leerzustand = Bausteine.leerzustand(Feather.FILE_TEXT,
            "Noch keine Belege",
            "Erstellen Sie mit „Neue Rechnung“ Ihren ersten Beleg.");

    @FXML private Label untertitel;
    @FXML private FlowPane kennzahlen;
    @FXML private TableView<Dokument> letzteBelege;
    @FXML private TableColumn<Dokument, String> nummerSpalte;
    @FXML private TableColumn<Dokument, String> typSpalte;
    @FXML private TableColumn<Dokument, LocalDate> datumSpalte;
    @FXML private TableColumn<Dokument, String> kundeSpalte;
    @FXML private TableColumn<Dokument, BigDecimal> bruttoSpalte;
    @FXML private TableColumn<Dokument, String> statusSpalte;
    @FXML private Button neueRechnungKnopf;

    public UebersichtAnsichtController(KennzahlenDienst kennzahlenDienst,
                                       DokumentService dokumentService,
                                       KundenService kundenService,
                                       KundenVerwaltungsService kundenVerwaltung,
                                       ProduktService produktService,
                                       EreignisBus ereignisBus,
                                       HintergrundAufgaben hintergrund) {
        this.kennzahlenDienst = kennzahlenDienst;
        this.dokumentService = dokumentService;
        this.kundenService = kundenService;
        this.kundenVerwaltung = kundenVerwaltung;
        this.produktService = produktService;
        this.ereignisBus = ereignisBus;
        this.hintergrund = hintergrund;
    }

    @FXML
    private void initialize() {
        nummerSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getBelegnummer()));
        typSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                Bausteine.belegart(z.getValue())));
        datumSpalte.setCellValueFactory(z -> new ReadOnlyObjectWrapper<>(z.getValue().getDatum()));
        datumSpalte.setCellFactory(Bausteine.formatierteZelle(TabellenFormat::datum));
        kundeSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().getKundeName()));
        bruttoSpalte.setCellValueFactory(z -> new ReadOnlyObjectWrapper<>(z.getValue().getSummeBrutto()));
        bruttoSpalte.setCellFactory(Bausteine.formatierteZelle(TabellenFormat::betrag));
        statusSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(
                Bausteine.anzeigestatus(z.getValue())));
        statusSpalte.setCellFactory(Bausteine.statusZelle());
        Bausteine.alsNummernspalte(nummerSpalte);
        Bausteine.alsBetragsspalte(bruttoSpalte);
        Bausteine.passeSpaltenAn(letzteBelege);

        letzteBelege.setPlaceholder(new Label("Belege werden geladen…"));

        // Ohne dieses Minimum zwingt die Kartenreihe das Fenster auf ihre
        // Gesamtbreite, statt in eine zweite Reihe umzubrechen.
        kennzahlen.setMinWidth(0);

        zeigeLadezustand();
        aktualisiere();
        // Kennzahlen hängen an Belegen; Kundennamen erscheinen in der Liste
        ereignisBus.abonniere(DatenBereich.DOKUMENTE, this::aktualisiere);
        ereignisBus.abonniere(DatenBereich.KUNDEN, this::aktualisiere);
    }

    /**
     * Die Auswertung liest den gesamten Belegbestand und läuft deshalb im
     * Hintergrund (Q-04): Das Fenster ist sofort bedienbar, die Karten füllen
     * sich, sobald das Ergebnis vorliegt. Die Vorgänge laufen nacheinander,
     * das zuletzt angestoßene Ergebnis wird also auch zuletzt angezeigt.
     */
    private void aktualisiere() {
        LocalDate heute = LocalDate.now();
        hintergrund.starte("Übersicht wird aktualisiert…", null,
                () -> kennzahlenDienst.ermittle(heute), this::zeige);
    }

    /** Platzhalterkarten, bis die erste Auswertung vorliegt. */
    private void zeigeLadezustand() {
        untertitel.setText("Übersicht wird geladen…");
        kennzahlen.getChildren().setAll(
                karte(Feather.CLOCK, "Offener Betrag", "…", "", null),
                karte(Feather.ALERT_TRIANGLE, "Überfällig", "…", "", null),
                karte(Feather.TRENDING_UP, "Umsatz " + LocalDate.now().getYear(), "…", "", null),
                karte(Feather.FILE_TEXT, "Belege gesamt", "…", "", null));
    }

    private void zeige(Kennzahlen zahlen) {
        letzteBelege.setPlaceholder(leerzustand);

        untertitel.setText(zahlen.belegeGesamt() == 0
                ? "Willkommen — legen Sie zunächst Kunden und Produkte an."
                : zahlen.belegeGesamt() + " Belege insgesamt");

        kennzahlen.getChildren().setAll(
                betragskarte(Feather.CLOCK, "Offener Betrag",
                        zahlen.offenerBetrag(), zahlen.offeneUnbekannteAnzahl(),
                        zahlen.offeneAnzahl() + " unbezahlte Rechnungen", "betonend"),
                betragskarte(Feather.ALERT_TRIANGLE, "Überfällig",
                        zahlen.ueberfaelligBetrag(), zahlen.ueberfaelligeUnbekannteAnzahl(),
                        zahlen.ueberfaelligAnzahl() + " Rechnungen über Zahlungsziel",
                        zahlen.ueberfaelligAnzahl() > 0 ? "warnend" : null),
                betragskarte(Feather.TRENDING_UP, "Umsatz " + LocalDate.now().getYear(),
                        zahlen.umsatzJahr(), zahlen.umsatzJahrUnbekannteAnzahl(),
                        "brutto, ohne stornierte Rechnungen", null),
                karte(Feather.FILE_TEXT, "Belege gesamt",
                        String.valueOf(zahlen.belegeGesamt()),
                        "über alle Belegarten", null));

        letzteBelege.getItems().setAll(zahlen.letzteBelege());
    }

    /** Zeigt unbekannte Beträge mit der vom Dienst ermittelten Rechnungsanzahl (GR-02, A-F-25). */
    private static VBox betragskarte(Feather symbol, String titel, BigDecimal betrag,
                                     int unbekannteAnzahl, String zusatz, String zusatzStil) {
        String wert = betrag == null ? "unbekannt" : TabellenFormat.betrag(betrag);
        String zusatzText = unbekannteAnzahl == 0 ? zusatz
                : zusatz + "; " + unbekannteAnzahl + (unbekannteAnzahl == 1 ? " Rechnung" : " Rechnungen")
                        + " mit unbekanntem Betrag";
        return karte(symbol, titel, wert, zusatzText, zusatzStil);
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
