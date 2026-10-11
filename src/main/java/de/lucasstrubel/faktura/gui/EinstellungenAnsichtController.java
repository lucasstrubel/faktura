package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.FakturaEigenschaften;
import de.lucasstrubel.faktura.firma.Firmenprofil;
import de.lucasstrubel.faktura.firma.FirmenprofilService;
import de.lucasstrubel.faktura.gemeinsam.Datensicherung;
import de.lucasstrubel.faktura.gemeinsam.GesamtExport;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Einstellungen-Ansicht: Pflege des Firmenprofils (§ 14 UStG; Briefkopf,
 * Zahlungshinweis, E-Rechnung) und Datensicherung des lokalen Bestands.
 */
public class EinstellungenAnsichtController {

    private final FirmenprofilService service;
    private final FakturaEigenschaften eigenschaften;
    private final Datensicherung datensicherung;
    private final HintergrundAufgaben hintergrund;
    private final SchliessSchutz schliessSchutz;
    private final GesamtExport gesamtExport;
    private List<String> gespeicherteFelder;
    @FXML private Label ungespeichertAnzeige;

    @FXML private TextField nameFeld;
    @FXML private TextField strasseFeld;
    @FXML private TextField plzFeld;
    @FXML private TextField ortFeld;
    @FXML private TextField ustIdNrFeld;
    @FXML private TextField steuernummerFeld;
    @FXML private TextField telefonFeld;
    @FXML private TextField eMailFeld;
    @FXML private TextField ibanFeld;
    @FXML private TextField bicFeld;
    @FXML private TextField bankFeld;
    @FXML private Button sicherungsKnopf;
    @FXML private Button exportKnopf;
    @FXML private Button steuerExportKnopf;
    @FXML private Button profilExportKnopf;

    private final Map<String, Control> felder = new LinkedHashMap<>();

    public EinstellungenAnsichtController(FirmenprofilService service,
                                          FakturaEigenschaften eigenschaften,
                                          Datensicherung datensicherung,
                                          HintergrundAufgaben hintergrund,
                                          SchliessSchutz schliessSchutz,
                                          GesamtExport gesamtExport) {
        this.service = service;
        this.eigenschaften = eigenschaften;
        this.datensicherung = datensicherung;
        this.hintergrund = hintergrund;
        this.schliessSchutz = schliessSchutz;
        this.gesamtExport = gesamtExport;
    }

    @FXML
    private void initialize() {
        felder.put("Name", nameFeld);
        felder.put("Straße", strasseFeld);
        felder.put("PLZ", plzFeld);
        felder.put("Ort", ortFeld);
        felder.put("USt-IdNr.", ustIdNrFeld);
        felder.put("Steuernummer", steuernummerFeld);
        felder.put("Telefon", telefonFeld);
        felder.put("E-Mail", eMailFeld);
        felder.put("IBAN", ibanFeld);
        felder.put("BIC", bicFeld);
        felder.put("Bank", bankFeld);
        // Ohne gespeichertes Profil bleiben die Felder leer (A-F-26)
        service.lade().ifPresent(this::fuelleFelder);
        gespeicherteFelder = feldstand();
        felder.values().forEach(feld -> ((TextField) feld).textProperty().addListener(
                (beobachtbar, alt, neu) -> aktualisiereAenderungsanzeige()));
        schliessSchutz.registriere(this::darfSchliessen);
    }

    private List<String> feldstand() {
        return felder.values().stream().map(feld -> ((TextField) feld).getText()).toList();
    }

    private void aktualisiereAenderungsanzeige() {
        ungespeichertAnzeige.setText(feldstand().equals(gespeicherteFelder)
                ? "" : "Ungespeicherte Änderungen");
    }

    /**
     * Rückfrage beim Beenden (SchliessSchutz). Die Ansicht kann dabei aus dem
     * Szenengraphen gelöst sein, wenn gerade eine andere Seite angezeigt wird —
     * dann gehört der Dialog zum geöffneten Hauptfenster.
     */
    private boolean darfSchliessen() {
        if (feldstand().equals(gespeicherteFelder)) {
            return true;
        }
        ButtonType speichern = new ButtonType("Speichern");
        ButtonType verwerfen = new ButtonType("Verwerfen");
        ButtonType abbrechen = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION,
                "Das Firmenprofil enthält ungespeicherte Änderungen.", speichern, verwerfen, abbrechen);
        dialog.setHeaderText("Einstellungen nicht gespeichert");
        Window besitzer = besitzerFenster();
        if (besitzer != null) {
            dialog.initOwner(besitzer);
        }
        ButtonType auswahl = dialog.showAndWait().orElse(abbrechen);
        return auswahl == verwerfen || (auswahl == speichern && speichereProfil());
    }

    private Window besitzerFenster() {
        Scene szene = nameFeld.getScene();
        if (szene != null && szene.getWindow() != null) {
            return szene.getWindow();
        }
        return Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
    }

    private void fuelleFelder(Firmenprofil profil) {
        nameFeld.setText(profil.name());
        strasseFeld.setText(profil.strasse());
        plzFeld.setText(profil.plz());
        ortFeld.setText(profil.ort());
        ustIdNrFeld.setText(leerFuerNull(profil.ustIdNr()));
        steuernummerFeld.setText(leerFuerNull(profil.steuernummer()));
        telefonFeld.setText(leerFuerNull(profil.telefon()));
        eMailFeld.setText(leerFuerNull(profil.eMail()));
        ibanFeld.setText(leerFuerNull(profil.iban()));
        bicFeld.setText(leerFuerNull(profil.bic()));
        bankFeld.setText(leerFuerNull(profil.bank()));
    }

    @FXML
    private void speichere() {
        speichereProfil();
    }

    private boolean speichereProfil() {
        return FxMeldung.mitFehlerbehandlung(felder, () -> {
            Firmenprofil gespeichert = service.speichere(new Firmenprofil(
                    nameFeld.getText(), strasseFeld.getText(), plzFeld.getText(), ortFeld.getText(),
                    ustIdNrFeld.getText(), steuernummerFeld.getText(), telefonFeld.getText(),
                    eMailFeld.getText(), ibanFeld.getText(), bicFeld.getText(), bankFeld.getText()));
            // Gespeicherte Schreibweise zurückspielen (z. B. IBAN in Vierergruppen)
            fuelleFelder(gespeichert);
            gespeicherteFelder = feldstand();
            aktualisiereAenderungsanzeige();
            FxMeldung.zeige(Meldung.erfolg(
                    "Das Firmenprofil wurde gespeichert; neue Belege verwenden es sofort."), felder);
        });
    }

    /** Datensicherung im Hintergrund: Das ZIP kann je nach Bestand dauern (Q-05). */
    @FXML
    private void sichere() {
        FileChooser auswahl = new FileChooser();
        auswahl.setInitialFileName("faktura-sicherung-" + LocalDate.now() + ".zip");
        File ziel = auswahl.showSaveDialog(besitzerFenster());
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

    /** Steueraufschlüsselung je Beleg als CSV (A-F-25). */
    @FXML
    private void exportiereSteuern() {
        exportiereCsv("Steueraufschlüsselung", "steuern.csv", steuerExportKnopf, gesamtExport::exportiereSteuern);
    }

    /** Gespeichertes Firmenprofil als CSV; ungespeicherte Eingaben bleiben außen vor (A-F-26). */
    @FXML
    private void exportiereFirmenprofil() {
        exportiereCsv("Gespeichertes Firmenprofil", "firmenprofil.csv", profilExportKnopf, gesamtExport::exportiereFirmenprofil);
    }

    private void exportiereCsv(String titel, String dateiname, Button knopf,
                               Consumer<Path> export) {
        FileChooser auswahl = new FileChooser();
        auswahl.setTitle(titel + " exportieren");
        auswahl.setInitialFileName(dateiname);
        auswahl.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-Datei", "*.csv"));
        File ziel = auswahl.showSaveDialog(besitzerFenster());
        if (ziel == null) {
            return;
        }
        hintergrund.starte(titel + " wird exportiert…", knopf,
                () -> export.accept(ziel.toPath()),
                () -> FxMeldung.zeige(Meldung.erfolg(titel + " wurde exportiert: " + ziel), null));
    }

    /** Gesamtbestand als ZIP aus einer Lesetransaktion (Q-06). */
    @FXML
    private void exportiereAlles() {
        FileChooser auswahl = new FileChooser();
        auswahl.setTitle("Alle Daten exportieren");
        auswahl.setInitialFileName("faktura-export-v1-" + LocalDate.now() + ".zip");
        auswahl.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP-Archiv", "*.zip"));
        File ziel = auswahl.showSaveDialog(besitzerFenster());
        if (ziel == null) {
            return;
        }
        hintergrund.starte("Alle Daten werden exportiert…", exportKnopf,
                () -> gesamtExport.exportiere(ziel.toPath()),
                () -> FxMeldung.zeige(Meldung.erfolg("Alle Daten wurden exportiert: " + ziel), null));
    }
}
