package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.Belegtyp;
import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.dokumente.Positionsangabe;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktService;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog zur direkten Erstellung von Angebot, Auftragsbestätigung oder
 * Lieferschein (BA-09 bis BA-11). Rechnungen werden über die geführte
 * Erstellung (Wizard, D-F-09) angelegt; Folgebelege über die Dokumentliste.
 */
public class BelegDialog extends Stage {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DokumentService dokumentService;

    private final ComboBox<Belegtyp> typWahl = new ComboBox<>();
    private final ComboBox<Kunde> kundenWahl = new ComboBox<>();
    private final ComboBox<Produkt> produktWahl = new ComboBox<>();
    private final Spinner<Integer> mengeWahl = new Spinner<>(1, 99999, 1);
    private final ListView<String> positionsListe = new ListView<>();
    private final Label datumBeschriftung = new Label("Gültig bis (leer = +30 Tage):");
    private final TextField datumFeld = new TextField();

    private final List<Positionsangabe> positionen = new ArrayList<>();

    public BelegDialog(Window besitzer, DokumentService dokumentService,
                       KundenService kundenService, ProduktService produktService) {
        this.dokumentService = dokumentService;
        initModality(Modality.APPLICATION_MODAL);
        initOwner(besitzer);
        setTitle("Neuen Beleg erstellen");

        typWahl.getItems().addAll(Belegtyp.ANGEBOT, Belegtyp.AUFTRAGSBESTAETIGUNG,
                Belegtyp.LIEFERSCHEIN);
        typWahl.setConverter(new StringConverter<>() {
            @Override
            public String toString(Belegtyp typ) {
                return typ == null ? "" : typ.anzeigename();
            }

            @Override
            public Belegtyp fromString(String text) {
                return null;
            }
        });
        typWahl.getSelectionModel().selectFirst();
        typWahl.valueProperty().addListener((beobachtbar, alt, neu) -> aktualisiereDatumsfeld());

        kundenWahl.getItems().addAll(kundenService.suche(""));
        kundenWahl.setConverter(new StringConverter<>() {
            @Override
            public String toString(Kunde kunde) {
                return kunde == null ? ""
                        : kunde.getName() + " (" + kunde.getKundennummer() + ")";
            }

            @Override
            public Kunde fromString(String text) {
                return null;
            }
        });
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

        Scene szene = new Scene(baueOberflaeche(), 640, 480);
        szene.getStylesheets().addAll(besitzer.getScene().getStylesheets());
        setScene(szene);
        setOnCloseRequest(ereignis -> {
            if (!darfVerwerfen()) {
                ereignis.consume();
            }
        });
    }

    private BorderPane baueOberflaeche() {
        GridPane kopf = new GridPane();
        kopf.setHgap(8);
        kopf.setVgap(6);
        kopf.add(new Label("Belegtyp: *"), 0, 0);
        kopf.add(typWahl, 1, 0);
        kopf.add(new Label("Kunde: *"), 0, 1);
        kopf.add(kundenWahl, 1, 1);
        kopf.add(datumBeschriftung, 0, 2);
        datumFeld.setTooltip(new Tooltip("Optional — Format: TT.MM.JJJJ"));
        kopf.add(datumFeld, 1, 2);
        Label legende = new Label("* Pflichtfeld");
        legende.getStyleClass().add("pflichtfeld-legende");
        kopf.add(legende, 0, 3, 2, 1);

        Button hinzufuegen = new Button("Hinzufügen");
        hinzufuegen.setOnAction(e -> fuegePositionHinzu());
        Button entfernen = new Button("Entfernen");
        entfernen.setOnAction(e -> entfernePosition());
        HBox eingabe = new HBox(8, new Label("Produkt:"), produktWahl,
                new Label("Menge:"), mengeWahl, hinzufuegen, entfernen);
        eingabe.setPadding(new Insets(8, 0, 4, 0));
        VBox mitte = new VBox(4, eingabe, positionsListe);
        VBox.setVgrow(positionsListe, javafx.scene.layout.Priority.ALWAYS);

        Button abbrechen = new Button("Abbrechen");
        abbrechen.setCancelButton(true);
        abbrechen.setOnAction(e -> {
            if (darfVerwerfen()) {
                close();
            }
        });
        Button erstellen = new Button("Erstellen");
        erstellen.setDefaultButton(true);
        erstellen.setOnAction(e -> erstelle());
        ButtonBar knoepfe = new ButtonBar();
        ButtonBar.setButtonData(abbrechen, ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonBar.setButtonData(erstellen, ButtonBar.ButtonData.OK_DONE);
        knoepfe.getButtons().addAll(abbrechen, erstellen);
        knoepfe.setPadding(new Insets(10, 0, 0, 0));

        BorderPane wurzel = new BorderPane();
        wurzel.setPadding(new Insets(12));
        wurzel.setTop(kopf);
        wurzel.setCenter(mitte);
        wurzel.setBottom(knoepfe);
        return wurzel;
    }

    /** Schutz vor Datenverlust: Nachfrage, wenn bereits Positionen erfasst wurden (D-F-02). */
    private boolean darfVerwerfen() {
        return positionen.isEmpty() || FxMeldung.bestaetige("Eingaben verwerfen",
                "Die erfassten Positionen gehen verloren. Dialog wirklich schließen?");
    }

    private void aktualisiereDatumsfeld() {
        Belegtyp typ = typWahl.getValue();
        datumBeschriftung.setText(switch (typ) {
            case ANGEBOT -> "Gültig bis (leer = +30 Tage):";
            case LIEFERSCHEIN -> "Lieferdatum (leer = heute):";
            default -> "Datum (entfällt):";
        });
        datumFeld.setDisable(typ != Belegtyp.ANGEBOT && typ != Belegtyp.LIEFERSCHEIN);
    }

    private void fuegePositionHinzu() {
        Produkt produkt = produktWahl.getValue();
        if (produkt == null) {
            return;
        }
        int menge = mengeWahl.getValue();
        positionen.add(new Positionsangabe(produkt.getProduktnummer(), menge));
        positionsListe.getItems().add(menge + " x " + produkt.getBezeichnung()
                + " (" + produkt.getProduktnummer() + ")");
    }

    private void entfernePosition() {
        int index = positionsListe.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            positionen.remove(index);
            positionsListe.getItems().remove(index);
        }
    }

    private void erstelle() {
        Kunde kunde = kundenWahl.getValue();
        String kundenNr = kunde == null ? null : kunde.getKundennummer();
        LocalDate datum;
        try {
            String text = datumFeld.getText().strip();
            datum = text.isEmpty() ? null : LocalDate.parse(text, DATUM);
        } catch (DateTimeParseException e) {
            FxMeldung.zeige(Meldung.fehler("Datum",
                    "Das Datum ist ungültig. Format: TT.MM.JJJJ"), null);
            return;
        }
        try {
            Belegtyp typ = typWahl.getValue();
            Dokument beleg = switch (typ) {
                case ANGEBOT -> dokumentService.erstelleAngebot(kundenNr, positionen, datum);
                case AUFTRAGSBESTAETIGUNG -> dokumentService.erstelleAuftragsbestaetigung(kundenNr, positionen);
                case LIEFERSCHEIN -> dokumentService.erstelleLieferschein(kundenNr, positionen, datum);
                default -> throw new IllegalStateException("Unerwarteter Belegtyp: " + typ);
            };
            FxMeldung.zeige(Meldung.erfolg(beleg.belegtyp().anzeigename() + " "
                    + beleg.getBelegnummer() + " wurde erstellt."), null);
            positionen.clear();
            close();
        } catch (ValidierungsException e) {
            FxMeldung.zeige(Meldung.fehler(e.getFeldname(), e.getMessage()), null);
        }
    }
}
