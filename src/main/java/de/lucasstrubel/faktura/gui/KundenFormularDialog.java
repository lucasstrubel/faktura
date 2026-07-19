package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.gemeinsam.Validierung;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenVerwaltungsService;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Modale Formular-Maske zum Anlegen und Bearbeiten eines Kunden (D-F-04,
 * F-05): alle Pflicht- und optionalen Felder mit Kennzeichnung,
 * <b>Live-Validierung während der Eingabe</b> (rote Feldmarkierung und
 * Meldung unter dem Formular, Q-09) und Schutz vor unbeabsichtigtem
 * Verwerfen (D-F-02).
 */
public class KundenFormularDialog extends Stage {

    private final KundenVerwaltungsService service;
    /** Kundennummer des bearbeiteten Kunden; {@code null} = Neuanlage. */
    private final String vorhandeneNummer;

    private final TextField nameFeld = new TextField();
    private final TextField strasseFeld = new TextField();
    private final TextField plzFeld = new TextField();
    private final TextField ortFeld = new TextField();
    private final TextField eMailFeld = new TextField();
    private final TextField telefonFeld = new TextField();
    private final TextField ustIdNrFeld = new TextField();
    private final Label liveMeldung = new Label();
    private final Map<String, Control> felder = new LinkedHashMap<>();

    private boolean ungespeichert;

    public KundenFormularDialog(Window besitzer, KundenVerwaltungsService service,
                                Kunde vorhandener) {
        this.service = service;
        this.vorhandeneNummer = vorhandener == null ? null : vorhandener.getKundennummer();
        initModality(Modality.APPLICATION_MODAL);
        initOwner(besitzer);
        setTitle(vorhandener == null ? "Neuen Kunden anlegen"
                : "Kunde " + vorhandener.getKundennummer() + " bearbeiten");

        felder.put("Name", nameFeld);
        felder.put("Straße", strasseFeld);
        felder.put("PLZ", plzFeld);
        felder.put("Ort", ortFeld);
        felder.put("E-Mail", eMailFeld);
        felder.put("Telefon", telefonFeld);
        felder.put("USt-IdNr.", ustIdNrFeld);

        Scene szene = new Scene(baueOberflaeche());
        szene.getStylesheets().addAll(besitzer.getScene().getStylesheets());
        setScene(szene);

        if (vorhandener != null) {
            fuelleFelder(vorhandener);
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
        plzFeld.setTooltip(new Tooltip("Pflichtfeld — Postleitzahl, z. B. 68163"));
        eMailFeld.setTooltip(new Tooltip("Optional — Format: name@domain.de"));

        int zeile = 0;
        zeile = formularZeile(formular, zeile, "Name: *", nameFeld);
        zeile = formularZeile(formular, zeile, "Straße: *", strasseFeld);
        zeile = formularZeile(formular, zeile, "PLZ: *", plzFeld);
        zeile = formularZeile(formular, zeile, "Ort: *", ortFeld);
        zeile = formularZeile(formular, zeile, "E-Mail:", eMailFeld);
        zeile = formularZeile(formular, zeile, "Telefon:", telefonFeld);
        zeile = formularZeile(formular, zeile, "USt-IdNr.:", ustIdNrFeld);
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

    private void fuelleFelder(Kunde kunde) {
        nameFeld.setText(kunde.getName());
        strasseFeld.setText(kunde.getStrasse());
        plzFeld.setText(kunde.getPlz());
        ortFeld.setText(kunde.getOrt());
        eMailFeld.setText(kunde.getEMail() == null ? "" : kunde.getEMail());
        telefonFeld.setText(kunde.getTelefon() == null ? "" : kunde.getTelefon());
        ustIdNrFeld.setText(kunde.getUstIdNr() == null ? "" : kunde.getUstIdNr());
    }

    /** Erst nach dem Vorbefüllen anmelden, damit nur Nutzereingaben zählen. */
    private void beobachteAenderungen() {
        for (TextField feld : new TextField[]{nameFeld, strasseFeld, plzFeld, ortFeld,
                eMailFeld, telefonFeld, ustIdNrFeld}) {
            feld.textProperty().addListener((beobachtbar, alt, neu) -> {
                ungespeichert = true;
                pruefeLive();
            });
        }
    }

    /**
     * Live-Validierung während der Eingabe (Q-09, C-F-16 bis F-18): prüft die
     * Formatregeln der zentralen {@link Validierung}, markiert das erste
     * ungültige Feld rot und benennt es unter dem Formular; Pflichtfelder
     * werden erst beim Speichern erzwungen, damit leere Felder beim Ausfüllen
     * nicht sofort rot aufleuchten.
     */
    private void pruefeLive() {
        felder.values().forEach(feld -> feld.getStyleClass().remove(FxMeldung.FEHLER_STIL));
        liveMeldung.setText("");
        try {
            if (!plzFeld.getText().isBlank()) {
                Validierung.pruefePlz(plzFeld.getText());
            }
            Validierung.pruefeEMail(eMailFeld.getText());
            Validierung.pruefeTelefon(telefonFeld.getText());
            Validierung.pruefeUstIdNr(ustIdNrFeld.getText());
        } catch (ValidierungsException e) {
            Control feld = felder.get(e.getFeldname());
            if (feld != null) {
                feld.getStyleClass().add(FxMeldung.FEHLER_STIL);
            }
            liveMeldung.setText(e.getMessage());
        }
    }

    private void speichere() {
        boolean erfolgreich = FxMeldung.mitFehlerbehandlung(felder, () -> {
            Kunde kunde = vorhandeneNummer == null
                    ? new Kunde()
                    : service.findeKunde(vorhandeneNummer);
            kunde.setName(nameFeld.getText().strip());
            kunde.setStrasse(strasseFeld.getText().strip());
            kunde.setPlz(plzFeld.getText().strip());
            kunde.setOrt(ortFeld.getText().strip());
            kunde.setEMail(leerZuNull(eMailFeld.getText()));
            kunde.setTelefon(leerZuNull(telefonFeld.getText()));
            kunde.setUstIdNr(leerZuNull(ustIdNrFeld.getText()));

            Kunde gespeichert = vorhandeneNummer == null
                    ? service.legeAn(kunde)
                    : service.aendere(kunde);
            ungespeichert = false;
            FxMeldung.zeige(Meldung.erfolg("Der Kunde wurde gespeichert. Kundennummer: "
                    + gespeichert.getKundennummer()), felder);
        });
        if (erfolgreich) {
            close();
        }
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
