package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktService;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Wizard-Dialog der geführten Rechnungserstellung mit genau fünf Schritten
 * (D-F-09 bis F-13); Dialogführung und Validierung liegen im GUI-freien
 * {@link RechnungsWizardController}.
 */
public class RechnungsWizardDialog extends JDialog {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RechnungsWizardController controller;
    private final KundenService kundenService;
    private final ProduktService produktService;

    private final CardLayout karten = new CardLayout();
    private final JPanel kartenPanel = new JPanel(karten);

    private final JTextField kundenSuche = new JTextField(18);
    private final DefaultListModel<Kunde> kundenListenModel = new DefaultListModel<>();
    private final JList<Kunde> kundenListe = new JList<>(kundenListenModel);

    private final JComboBox<Produkt> produktWahl = new JComboBox<>();
    private final JSpinner mengeWahl = new JSpinner(new SpinnerNumberModel(1, 1, 99999, 1));
    private final DefaultListModel<String> positionsListenModel = new DefaultListModel<>();
    private final JList<String> positionsListe = new JList<>(positionsListenModel);

    private final JTextField rechnungsdatumFeld = new JTextField(10);
    private final JTextField zahlungszielFeld = new JTextField(10);

    private final JTextArea zusammenfassung = new JTextArea(14, 50);

    private final JButton zurueckKnopf = new JButton("< Zurück");
    private final JButton weiterKnopf = new JButton("Weiter >");
    private final JButton speichernKnopf = new JButton("Speichern");
    private final JLabel schrittAnzeige = new JLabel();
    private final JLabel[] schrittMarkierungen = new JLabel[WizardSchritt.values().length];

    public RechnungsWizardDialog(Window besitzer, RechnungsWizardController controller,
                                 KundenService kundenService, ProduktService produktService) {
        super(besitzer, "Geführte Rechnungserstellung", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.kundenService = kundenService;
        this.produktService = produktService;
        baueOberflaeche();
        ladeKunden("");
        ladeProdukte();
        zeigeSchritt();
        pack();
        setLocationRelativeTo(besitzer);
    }

    private void baueOberflaeche() {
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Schrittindikator: alle fünf Schritte sichtbar, aktueller Schritt hervorgehoben (Q-05)
        JPanel kopf = new JPanel(new BorderLayout(0, 2));
        JPanel schritte = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        WizardSchritt[] alleSchritte = WizardSchritt.values();
        for (int i = 0; i < alleSchritte.length; i++) {
            if (i > 0) {
                schritte.add(new JLabel("›"));
            }
            schrittMarkierungen[i] = new JLabel((i + 1) + ". " + schrittName(alleSchritte[i]));
            schritte.add(schrittMarkierungen[i]);
        }
        kopf.add(schritte, BorderLayout.NORTH);
        kopf.add(schrittAnzeige, BorderLayout.SOUTH);
        add(kopf, BorderLayout.NORTH);

        kartenPanel.add(baueSchrittKunde(), WizardSchritt.KUNDE_WAEHLEN.name());
        kartenPanel.add(baueSchrittPositionen(), WizardSchritt.POSITIONEN_ERFASSEN.name());
        kartenPanel.add(baueSchrittDaten(), WizardSchritt.DATEN_BESTAETIGEN.name());
        kartenPanel.add(baueSchrittZusammenfassung(), WizardSchritt.ZUSAMMENFASSUNG.name());
        kartenPanel.add(baueSchrittSpeichern(), WizardSchritt.SPEICHERN.name());
        add(kartenPanel, BorderLayout.CENTER);

        JPanel knoepfe = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton abbrechen = new JButton("Abbrechen");
        abbrechen.setMnemonic('A');
        abbrechen.addActionListener(e -> abbrechenMitNachfrage());
        zurueckKnopf.setMnemonic('Z');
        zurueckKnopf.addActionListener(e -> {
            controller.zurueck();
            zeigeSchritt();
        });
        weiterKnopf.setMnemonic('W');
        weiterKnopf.addActionListener(e -> weiter());
        speichernKnopf.setMnemonic('S');
        speichernKnopf.addActionListener(e -> speichere());
        knoepfe.add(abbrechen);
        knoepfe.add(zurueckKnopf);
        knoepfe.add(weiterKnopf);
        knoepfe.add(speichernKnopf);
        add(knoepfe, BorderLayout.SOUTH);

        UiHilfen.escSchliesst(this, this::abbrechenMitNachfrage);
        UiHilfen.fensterSchliessenAbfangen(this, this::abbrechenMitNachfrage);
    }

    /** Schutz vor Datenverlust: Nachfrage, wenn bereits Eingaben erfasst wurden. */
    private void abbrechenMitNachfrage() {
        boolean datenErfasst = positionsListenModel.getSize() > 0
                || kundenListe.getSelectedValue() != null;
        if (datenErfasst) {
            int antwort = JOptionPane.showConfirmDialog(this,
                    "Die erfassten Eingaben gehen verloren. Assistent wirklich schließen?",
                    "Eingaben verwerfen", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (antwort != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dispose();
    }

    /** Schritt 1: Kunde auswählen (F-09). */
    private JPanel baueSchrittKunde() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel suche = new JPanel(new FlowLayout(FlowLayout.LEFT));
        suche.add(new JLabel("Suche:"));
        suche.add(kundenSuche);
        kundenSuche.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                ladeKunden(kundenSuche.getText().trim());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                ladeKunden(kundenSuche.getText().trim());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                ladeKunden(kundenSuche.getText().trim());
            }
        });
        panel.add(suche, BorderLayout.NORTH);

        kundenListe.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        kundenListe.addListSelectionListener(e -> {
            Kunde kunde = kundenListe.getSelectedValue();
            controller.getModel().setKundenNr(kunde == null ? null : kunde.getKundennummer());
        });
        panel.add(new JScrollPane(kundenListe), BorderLayout.CENTER);
        return panel;
    }

    /** Schritt 2: mindestens eine Produktposition mit Menge erfassen (F-09). */
    private JPanel baueSchrittPositionen() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel eingabe = new JPanel(new FlowLayout(FlowLayout.LEFT));
        eingabe.add(new JLabel("Produkt:"));
        eingabe.add(produktWahl);
        eingabe.add(new JLabel("Menge:"));
        eingabe.add(mengeWahl);
        JButton hinzufuegen = new JButton("Hinzufügen");
        hinzufuegen.setMnemonic('H');
        hinzufuegen.addActionListener(e -> {
            Produkt produkt = (Produkt) produktWahl.getSelectedItem();
            if (produkt == null) {
                return;
            }
            int menge = (Integer) mengeWahl.getValue();
            controller.getModel().fuegePositionHinzu(
                    new PositionsEingabe(produkt.getProduktnummer(), menge));
            positionsListenModel.addElement(menge + " x " + produkt.getBezeichnung()
                    + " (" + produkt.getProduktnummer() + ")");
        });
        eingabe.add(hinzufuegen);
        JButton entfernen = new JButton("Entfernen");
        entfernen.setMnemonic('E');
        entfernen.addActionListener(e -> {
            int index = positionsListe.getSelectedIndex();
            if (index >= 0) {
                controller.getModel().entfernePosition(index);
                positionsListenModel.remove(index);
            }
        });
        eingabe.add(entfernen);
        panel.add(eingabe, BorderLayout.NORTH);
        panel.add(new JScrollPane(positionsListe), BorderLayout.CENTER);
        return panel;
    }

    /** Schritt 3: Rechnungsdatum und Zahlungsziel bestätigen (F-09). */
    private JPanel baueSchrittDaten() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Rechnungsdatum (TT.MM.JJJJ): *"));
        rechnungsdatumFeld.setText(DATUM.format(LocalDate.now()));
        rechnungsdatumFeld.setToolTipText("Pflichtfeld — Format: TT.MM.JJJJ");
        panel.add(rechnungsdatumFeld);
        panel.add(new JLabel("Zahlungsziel (leer = 14 Tage):"));
        zahlungszielFeld.setToolTipText("Optional — Format: TT.MM.JJJJ, leer = 14 Tage nach Rechnungsdatum");
        panel.add(zahlungszielFeld);
        panel.add(UiHilfen.pflichtfeldLegende());
        return panel;
    }

    /** Schritt 4: Zusammenfassung prüfen (F-12). */
    private JPanel baueSchrittZusammenfassung() {
        JPanel panel = new JPanel(new BorderLayout());
        zusammenfassung.setEditable(false);
        panel.add(new JScrollPane(zusammenfassung), BorderLayout.CENTER);
        return panel;
    }

    /** Schritt 5: speichern (F-13). */
    private JPanel baueSchrittSpeichern() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Alle Angaben sind erfasst. Klicken Sie auf „Speichern“, "
                + "um die Rechnung zu erstellen."));
        return panel;
    }

    private void weiter() {
        if (controller.getModel().getAktuellerSchritt() == WizardSchritt.DATEN_BESTAETIGEN
                && !uebernehmeDaten()) {
            return;
        }
        if (!controller.weiter()) {
            MeldungsAnzeige.zeige(this, controller.getLetzteMeldung(), null);
            return;
        }
        zeigeSchritt();
    }

    /** Übernimmt die Datumsfelder in das Modell; bei Formatfehlern Meldung (F-10, Q-09). */
    private boolean uebernehmeDaten() {
        try {
            controller.getModel().setRechnungsdatum(
                    LocalDate.parse(rechnungsdatumFeld.getText().trim(), DATUM));
        } catch (DateTimeParseException e) {
            MeldungsAnzeige.zeige(this, Meldung.fehler("Rechnungsdatum",
                    "Das 'Rechnungsdatum' ist ungültig. Format: TT.MM.JJJJ"), null);
            return false;
        }
        String zahlungsziel = zahlungszielFeld.getText().trim();
        if (zahlungsziel.isEmpty()) {
            controller.getModel().setZahlungsziel(null);
        } else {
            try {
                controller.getModel().setZahlungsziel(LocalDate.parse(zahlungsziel, DATUM));
            } catch (DateTimeParseException e) {
                MeldungsAnzeige.zeige(this, Meldung.fehler("Zahlungsziel",
                        "Das 'Zahlungsziel' ist ungültig. Format: TT.MM.JJJJ"), null);
                return false;
            }
        }
        return true;
    }

    private void speichere() {
        Meldung meldung = controller.speichern();
        MeldungsAnzeige.zeige(this, meldung, null);
        if (meldung.typ() == MeldungsTyp.ERFOLG) {
            dispose();
        }
    }

    private void zeigeSchritt() {
        WizardSchritt schritt = controller.getModel().getAktuellerSchritt();
        if (schritt == WizardSchritt.ZUSAMMENFASSUNG) {
            zusammenfassung.setText(controller.erzeugeZusammenfassung());
        }
        karten.show(kartenPanel, schritt.name());
        schrittAnzeige.setText("Schritt " + (schritt.ordinal() + 1) + " von 5: " + schrittName(schritt));
        for (int i = 0; i < schrittMarkierungen.length; i++) {
            boolean aktuell = i == schritt.ordinal();
            schrittMarkierungen[i].setFont(schrittMarkierungen[i].getFont()
                    .deriveFont(aktuell ? Font.BOLD : Font.PLAIN));
            schrittMarkierungen[i].setForeground(UIManager.getColor(
                    aktuell ? "Label.foreground" : "Label.disabledForeground"));
        }
        zurueckKnopf.setEnabled(schritt.ordinal() > 0);
        weiterKnopf.setEnabled(schritt != WizardSchritt.SPEICHERN);
        speichernKnopf.setEnabled(schritt == WizardSchritt.SPEICHERN);
        // Enter führt immer die sinnvollste Aktion des Schritts aus
        getRootPane().setDefaultButton(
                schritt == WizardSchritt.SPEICHERN ? speichernKnopf : weiterKnopf);
    }

    private static String schrittName(WizardSchritt schritt) {
        return switch (schritt) {
            case KUNDE_WAEHLEN -> "Kunde auswählen";
            case POSITIONEN_ERFASSEN -> "Positionen erfassen";
            case DATEN_BESTAETIGEN -> "Rechnungsdatum und Zahlungsziel";
            case ZUSAMMENFASSUNG -> "Zusammenfassung prüfen";
            case SPEICHERN -> "Speichern";
        };
    }

    private void ladeKunden(String suchbegriff) {
        kundenListenModel.clear();
        for (Kunde kunde : kundenService.suche(suchbegriff)) {
            kundenListenModel.addElement(kunde);
        }
    }

    private void ladeProdukte() {
        produktWahl.setModel(new DefaultComboBoxModel<>(
                produktService.suche("").toArray(new Produkt[0])));
    }
}
