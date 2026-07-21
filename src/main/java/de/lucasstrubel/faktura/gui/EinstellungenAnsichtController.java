package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.FakturaEigenschaften;
import de.lucasstrubel.faktura.firma.Firmenprofil;
import de.lucasstrubel.faktura.firma.FirmenprofilService;
import de.lucasstrubel.faktura.gemeinsam.Datensicherung;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Einstellungen-Ansicht: Pflege des Firmenprofils (§ 14 UStG; Briefkopf,
 * Zahlungshinweis, E-Rechnung) und Datensicherung des lokalen Bestands.
 */
public class EinstellungenAnsichtController {

    private final FirmenprofilService service;
    private final FakturaEigenschaften eigenschaften;
    private final Datensicherung datensicherung;
    private final HintergrundAufgaben hintergrund;

    @FXML private TextField nameFeld;
    @FXML private TextField strasseFeld;
    @FXML private TextField plzFeld;
    @FXML private TextField ortFeld;
    @FXML private TextField ustIdNrFeld;
    @FXML private TextField telefonFeld;
    @FXML private TextField eMailFeld;
    @FXML private TextField ibanFeld;
    @FXML private TextField bicFeld;
    @FXML private TextField bankFeld;
    @FXML private Button sicherungsKnopf;

    private final Map<String, Control> felder = new LinkedHashMap<>();

    public EinstellungenAnsichtController(FirmenprofilService service,
                                          FakturaEigenschaften eigenschaften,
                                          Datensicherung datensicherung,
                                          HintergrundAufgaben hintergrund) {
        this.service = service;
        this.eigenschaften = eigenschaften;
        this.datensicherung = datensicherung;
        this.hintergrund = hintergrund;
    }

    @FXML
    private void initialize() {
        felder.put("Name", nameFeld);
        felder.put("Straße", strasseFeld);
        felder.put("PLZ", plzFeld);
        felder.put("Ort", ortFeld);
        felder.put("USt-IdNr.", ustIdNrFeld);
        felder.put("Telefon", telefonFeld);
        felder.put("E-Mail", eMailFeld);
        felder.put("IBAN", ibanFeld);
        fuelleFelder(service.lade());
    }

    private void fuelleFelder(Firmenprofil profil) {
        nameFeld.setText(profil.name());
        strasseFeld.setText(profil.strasse());
        plzFeld.setText(profil.plz());
        ortFeld.setText(profil.ort());
        ustIdNrFeld.setText(leerFuerNull(profil.ustIdNr()));
        telefonFeld.setText(leerFuerNull(profil.telefon()));
        eMailFeld.setText(leerFuerNull(profil.eMail()));
        ibanFeld.setText(leerFuerNull(profil.iban()));
        bicFeld.setText(leerFuerNull(profil.bic()));
        bankFeld.setText(leerFuerNull(profil.bank()));
    }

    @FXML
    private void speichere() {
        FxMeldung.mitFehlerbehandlung(felder, () -> {
            service.speichere(new Firmenprofil(
                    nameFeld.getText().strip(), strasseFeld.getText().strip(),
                    plzFeld.getText().strip(), ortFeld.getText().strip(),
                    leerZuNull(ustIdNrFeld.getText()), leerZuNull(telefonFeld.getText()),
                    leerZuNull(eMailFeld.getText()), leerZuNull(ibanFeld.getText()),
                    leerZuNull(bicFeld.getText()), leerZuNull(bankFeld.getText())));
            FxMeldung.zeige(Meldung.erfolg(
                    "Das Firmenprofil wurde gespeichert; neue Belege verwenden es sofort."), felder);
        });
    }

    /** Datensicherung im Hintergrund: Das ZIP kann je nach Bestand dauern (Q-05). */
    @FXML
    private void sichere() {
        FileChooser auswahl = new FileChooser();
        auswahl.setInitialFileName("faktura-sicherung-" + LocalDate.now() + ".zip");
        File ziel = auswahl.showSaveDialog(nameFeld.getScene().getWindow());
        if (ziel == null) {
            return;
        }
        hintergrund.starte("Datensicherung wird erstellt…", sicherungsKnopf,
                () -> datensicherung.erstelle(eigenschaften.datenVerzeichnis(), ziel.toPath()),
                () -> FxMeldung.zeige(Meldung.erfolg(
                        "Die Datensicherung wurde erstellt: " + ziel), null));
    }

    private static String leerFuerNull(String wert) {
        return wert == null ? "" : wert;
    }

    private static String leerZuNull(String text) {
        String wert = text.strip();
        return wert.isEmpty() ? null : wert;
    }
}
