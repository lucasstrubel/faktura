package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenVerwaltungsService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modale Formular-Maske zum Anlegen und Bearbeiten eines Kunden (D-F-04,
 * F-05): alle Pflicht- und optionalen Felder mit Kennzeichnung, Validierungs-
 * rückmeldung mit Feldmarkierung und Schutz vor unbeabsichtigtem Verwerfen.
 */
public class KundenFormularDialog extends JDialog {

    private final KundenVerwaltungsService service;
    /** Kundennummer des bearbeiteten Kunden; {@code null} = Neuanlage. */
    private final String vorhandeneNummer;

    private final JTextField nameFeld = new JTextField(20);
    private final JTextField strasseFeld = new JTextField(20);
    private final JTextField plzFeld = new JTextField(8);
    private final JTextField ortFeld = new JTextField(20);
    private final JTextField eMailFeld = new JTextField(20);
    private final JTextField telefonFeld = new JTextField(20);
    private final JTextField ustIdNrFeld = new JTextField(20);
    private final Map<String, JComponent> felder = new LinkedHashMap<>();

    private boolean ungespeichert;

    public KundenFormularDialog(Window besitzer, KundenVerwaltungsService service,
                                Kunde vorhandener) {
        super(besitzer, vorhandener == null ? "Neuen Kunden anlegen"
                        : "Kunde " + vorhandener.getKundennummer() + " bearbeiten",
                ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.vorhandeneNummer = vorhandener == null ? null : vorhandener.getKundennummer();
        felder.put("Name", nameFeld);
        felder.put("Straße", strasseFeld);
        felder.put("PLZ", plzFeld);
        felder.put("Ort", ortFeld);
        felder.put("E-Mail", eMailFeld);
        baueOberflaeche();
        if (vorhandener != null) {
            fuelleFelder(vorhandener);
        }
        beobachteAenderungen();
        pack();
        setLocationRelativeTo(besitzer);
    }

    private void baueOberflaeche() {
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formular = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        plzFeld.setToolTipText("Pflichtfeld — Postleitzahl, z. B. 68163");
        eMailFeld.setToolTipText("Optional — Format: name@domain.de");

        int zeile = 0;
        zeile = formularZeile(formular, c, zeile, "Name: *", nameFeld);
        zeile = formularZeile(formular, c, zeile, "Straße: *", strasseFeld);
        zeile = formularZeile(formular, c, zeile, "PLZ: *", plzFeld);
        zeile = formularZeile(formular, c, zeile, "Ort: *", ortFeld);
        zeile = formularZeile(formular, c, zeile, "E-Mail:", eMailFeld);
        zeile = formularZeile(formular, c, zeile, "Telefon:", telefonFeld);
        zeile = formularZeile(formular, c, zeile, "USt-IdNr.:", ustIdNrFeld);

        c.gridx = 0;
        c.gridy = zeile;
        c.gridwidth = 2;
        formular.add(UiHilfen.pflichtfeldLegende(), c);
        add(formular, BorderLayout.CENTER);

        JPanel knoepfe = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton abbrechen = new JButton("Abbrechen");
        abbrechen.setMnemonic('A');
        abbrechen.addActionListener(e -> abbrechenMitNachfrage());
        JButton speichern = new JButton("Speichern");
        speichern.setMnemonic('S');
        speichern.addActionListener(e -> speichere());
        knoepfe.add(abbrechen);
        knoepfe.add(speichern);
        add(knoepfe, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(speichern);
        UiHilfen.escSchliesst(this, this::abbrechenMitNachfrage);
        UiHilfen.fensterSchliessenAbfangen(this, this::abbrechenMitNachfrage);
    }

    private int formularZeile(JPanel formular, GridBagConstraints c, int zeile,
                              String beschriftung, JComponent feld) {
        c.gridx = 0;
        c.gridy = zeile;
        c.gridwidth = 1;
        c.weightx = 0;
        formular.add(new JLabel(beschriftung), c);
        c.gridx = 1;
        c.weightx = 1;
        formular.add(feld, c);
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
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                ungespeichert = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                ungespeichert = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                ungespeichert = true;
            }
        };
        for (JTextField feld : List.of(nameFeld, strasseFeld, plzFeld, ortFeld,
                eMailFeld, telefonFeld, ustIdNrFeld)) {
            feld.getDocument().addDocumentListener(listener);
        }
    }

    private void speichere() {
        MeldungsAnzeige.mitFehlerbehandlung(this, felder, () -> {
            Kunde kunde = vorhandeneNummer == null
                    ? new Kunde()
                    : service.findeKunde(vorhandeneNummer);
            kunde.setName(nameFeld.getText().trim());
            kunde.setStrasse(strasseFeld.getText().trim());
            kunde.setPlz(plzFeld.getText().trim());
            kunde.setOrt(ortFeld.getText().trim());
            kunde.setEMail(leerZuNull(eMailFeld.getText()));
            kunde.setTelefon(leerZuNull(telefonFeld.getText()));
            kunde.setUstIdNr(leerZuNull(ustIdNrFeld.getText()));

            Kunde gespeichert = vorhandeneNummer == null
                    ? service.legeAn(kunde)
                    : service.aendere(kunde);
            ungespeichert = false;
            MeldungsAnzeige.zeige(this, Meldung.erfolg("Der Kunde wurde gespeichert. Kundennummer: "
                    + gespeichert.getKundennummer()), felder);
            dispose();
        });
    }

    /** Schutz vor Datenverlust: Nachfrage, wenn bereits Eingaben geändert wurden. */
    private void abbrechenMitNachfrage() {
        if (ungespeichert) {
            int antwort = JOptionPane.showConfirmDialog(this,
                    "Die Eingaben gehen verloren. Maske wirklich schließen?",
                    "Eingaben verwerfen", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (antwort != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dispose();
    }

    private static String leerZuNull(String text) {
        String wert = text.trim();
        return wert.isEmpty() ? null : wert;
    }
}
