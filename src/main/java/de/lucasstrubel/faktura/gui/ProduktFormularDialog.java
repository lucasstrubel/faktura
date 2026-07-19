package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktVerwaltungsService;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Modale Formular-Maske zum Anlegen und Bearbeiten eines Produkts (D-F-04,
 * F-05): Pflicht- und optionale Felder mit Kennzeichnung, Live-Prüfung des
 * Preisformats (Q-09) und Schutz vor unbeabsichtigtem Verwerfen (D-F-02).
 */
public class ProduktFormularDialog extends Stage {

    private static final String[] STEUERSAETZE = {"19 %", "7 %", "0 %"};

    private final ProduktVerwaltungsService service;
    /** Produktnummer des bearbeiteten Produkts; {@code null} = Neuanlage. */
    private final String vorhandeneNummer;

    private final TextField bezeichnungFeld = new TextField();
    private final TextField beschreibungFeld = new TextField();
    private final TextField preisFeld = new TextField();
    private final ComboBox<String> steuersatzWahl = new ComboBox<>();
    private final TextField einheitFeld = new TextField();
    private final Label liveMeldung = new Label();
    private final Map<String, Control> felder = new LinkedHashMap<>();

    private boolean ungespeichert;

    public ProduktFormularDialog(Window besitzer, ProduktVerwaltungsService service,
                                 Produkt vorhandenes) {
        this.service = service;
        this.vorhandeneNummer = vorhandenes == null ? null : vorhandenes.getProduktnummer();
        initModality(Modality.APPLICATION_MODAL);
        initOwner(besitzer);
        setTitle(vorhandenes == null ? "Neues Produkt anlegen"
                : "Produkt " + vorhandenes.getProduktnummer() + " bearbeiten");

        steuersatzWahl.getItems().addAll(STEUERSAETZE);
        steuersatzWahl.getSelectionModel().selectFirst();

        felder.put("Bezeichnung", bezeichnungFeld);
        felder.put("Einzelpreis", preisFeld);
        felder.put("Steuersatz", steuersatzWahl);

        Scene szene = new Scene(baueOberflaeche());
        szene.getStylesheets().addAll(besitzer.getScene().getStylesheets());
        setScene(szene);

        if (vorhandenes != null) {
            fuelleFelder(vorhandenes);
        }
        beobachteAenderungen();
        setOnCloseRequest(ereignis -> {
            if (!darfVerwerfen()) {
                ereignis.consume();
            }
        });
    }

    private BorderPane baueOberflaeche() {
        GridPane formular = new GridPane();
        formular.setHgap(8);
        formular.setVgap(6);
        preisFeld.setTooltip(new Tooltip(
                "Pflichtfeld — z. B. 19,90 (Komma oder Punkt als Dezimaltrennzeichen)"));

        int zeile = 0;
        zeile = formularZeile(formular, zeile, "Bezeichnung: *", bezeichnungFeld);
        zeile = formularZeile(formular, zeile, "Beschreibung:", beschreibungFeld);
        zeile = formularZeile(formular, zeile, "Einzelpreis (netto): *", preisFeld);
        zeile = formularZeile(formular, zeile, "Steuersatz: *", steuersatzWahl);
        zeile = formularZeile(formular, zeile, "Einheit:", einheitFeld);
        Label legende = new Label("* Pflichtfeld");
        legende.getStyleClass().add("pflichtfeld-legende");
        formular.add(legende, 0, zeile, 2, 1);

        liveMeldung.getStyleClass().add("feld-meldung");
        liveMeldung.setWrapText(true);

        Button abbrechen = new Button("Abbrechen");
        abbrechen.setCancelButton(true);
        abbrechen.setOnAction(e -> {
            if (darfVerwerfen()) {
                close();
            }
        });
        Button speichern = new Button("Speichern");
        speichern.setDefaultButton(true);
        speichern.setOnAction(e -> speichere());
        ButtonBar knoepfe = new ButtonBar();
        ButtonBar.setButtonData(abbrechen, ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonBar.setButtonData(speichern, ButtonBar.ButtonData.OK_DONE);
        knoepfe.getButtons().addAll(abbrechen, speichern);

        BorderPane wurzel = new BorderPane();
        wurzel.setPadding(new Insets(12));
        wurzel.setCenter(formular);
        BorderPane fuss = new BorderPane();
        fuss.setPadding(new Insets(10, 0, 0, 0));
        fuss.setLeft(liveMeldung);
        fuss.setRight(knoepfe);
        wurzel.setBottom(fuss);
        return wurzel;
    }

    private int formularZeile(GridPane formular, int zeile, String beschriftung, Control feld) {
        formular.add(new Label(beschriftung), 0, zeile);
        formular.add(feld, 1, zeile);
        return zeile + 1;
    }

    private void fuelleFelder(Produkt produkt) {
        bezeichnungFeld.setText(produkt.getBezeichnung());
        beschreibungFeld.setText(produkt.getBeschreibung() == null ? "" : produkt.getBeschreibung());
        preisFeld.setText(produkt.getEinzelpreisNetto().toPlainString());
        steuersatzWahl.getSelectionModel().select(
                switch (produkt.getSteuersatz().stripTrailingZeros().toPlainString()) {
                    case "0.19" -> 0;
                    case "0.07" -> 1;
                    default -> 2;
                });
        einheitFeld.setText(produkt.getEinheit() == null ? "" : produkt.getEinheit());
    }

    /** Erst nach dem Vorbefüllen anmelden, damit nur Nutzereingaben zählen. */
    private void beobachteAenderungen() {
        for (TextField feld : new TextField[]{bezeichnungFeld, beschreibungFeld,
                preisFeld, einheitFeld}) {
            feld.textProperty().addListener((beobachtbar, alt, neu) -> {
                ungespeichert = true;
                pruefeLive();
            });
        }
        steuersatzWahl.valueProperty().addListener(
                (beobachtbar, alt, neu) -> ungespeichert = true);
    }

    /** Live-Prüfung des Preisformats während der Eingabe (Q-09). */
    private void pruefeLive() {
        preisFeld.getStyleClass().remove(FxMeldung.FEHLER_STIL);
        liveMeldung.setText("");
        String wert = preisFeld.getText().strip().replace(',', '.');
        if (wert.isEmpty()) {
            return;
        }
        try {
            new BigDecimal(wert);
        } catch (NumberFormatException e) {
            preisFeld.getStyleClass().add(FxMeldung.FEHLER_STIL);
            liveMeldung.setText("Der 'Einzelpreis (netto)' ist keine gültige Zahl: "
                    + preisFeld.getText());
        }
    }

    private void speichere() {
        boolean erfolgreich = FxMeldung.mitFehlerbehandlung(felder, () -> {
            Produkt produkt = vorhandeneNummer == null
                    ? new Produkt()
                    : service.findeProdukt(vorhandeneNummer);
            produkt.setBezeichnung(bezeichnungFeld.getText().strip());
            produkt.setBeschreibung(leerZuNull(beschreibungFeld.getText()));
            produkt.setEinzelpreisNetto(parsePreis(preisFeld.getText()));
            produkt.setSteuersatz(gewaehlterSteuersatz());
            produkt.setEinheit(leerZuNull(einheitFeld.getText()));

            Produkt gespeichert = vorhandeneNummer == null
                    ? service.legeAn(produkt)
                    : service.aendere(produkt);
            ungespeichert = false;
            FxMeldung.zeige(Meldung.erfolg("Das Produkt wurde gespeichert. Produktnummer: "
                    + gespeichert.getProduktnummer()), felder);
        });
        if (erfolgreich) {
            close();
        }
    }

    /** Komma oder Punkt als Dezimaltrennzeichen; kaufmännische Rundung auf Scale 2. */
    private static BigDecimal parsePreis(String text) {
        String wert = text.strip().replace(',', '.');
        if (wert.isEmpty()) {
            throw new ValidierungsException("Einzelpreis",
                    "Das Pflichtfeld 'Einzelpreis (netto)' fehlt.");
        }
        try {
            return new BigDecimal(wert).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new ValidierungsException("Einzelpreis",
                    "Der 'Einzelpreis (netto)' ist keine gültige Zahl: " + text);
        }
    }

    private BigDecimal gewaehlterSteuersatz() {
        return switch (steuersatzWahl.getSelectionModel().getSelectedIndex()) {
            case 0 -> new BigDecimal("0.19");
            case 1 -> new BigDecimal("0.07");
            default -> new BigDecimal("0.00");
        };
    }

    /** Schutz vor Datenverlust: Nachfrage, wenn bereits Eingaben geändert wurden (D-F-02). */
    private boolean darfVerwerfen() {
        return !ungespeichert || FxMeldung.bestaetige("Eingaben verwerfen",
                "Die Eingaben gehen verloren. Maske wirklich schließen?");
    }

    private static String leerZuNull(String text) {
        String wert = text.strip();
        return wert.isEmpty() ? null : wert;
    }
}
