package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktVerwaltungsService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modale Formular-Maske zum Anlegen und Bearbeiten eines Produkts (D-F-04,
 * F-05): alle Pflicht- und optionalen Felder mit Kennzeichnung, Validierungs-
 * rückmeldung mit Feldmarkierung und Schutz vor unbeabsichtigtem Verwerfen.
 */
public class ProduktFormularDialog extends JDialog {

    private static final String[] STEUERSAETZE = {"19 %", "7 %", "0 %"};

    private final ProduktVerwaltungsService service;
    /** Produktnummer des bearbeiteten Produkts; {@code null} = Neuanlage. */
    private final String vorhandeneNummer;

    private final JTextField bezeichnungFeld = new JTextField(20);
    private final JTextField beschreibungFeld = new JTextField(20);
    private final JTextField preisFeld = new JTextField(10);
    private final JComboBox<String> steuersatzWahl = new JComboBox<>(STEUERSAETZE);
    private final JTextField einheitFeld = new JTextField(10);
    private final Map<String, JComponent> felder = new LinkedHashMap<>();

    private boolean ungespeichert;

    public ProduktFormularDialog(Window besitzer, ProduktVerwaltungsService service,
                                 Produkt vorhandenes) {
        super(besitzer, vorhandenes == null ? "Neues Produkt anlegen"
                        : "Produkt " + vorhandenes.getProduktnummer() + " bearbeiten",
                ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.vorhandeneNummer = vorhandenes == null ? null : vorhandenes.getProduktnummer();
        felder.put("Bezeichnung", bezeichnungFeld);
        felder.put("Einzelpreis", preisFeld);
        felder.put("Steuersatz", steuersatzWahl);
        baueOberflaeche();
        if (vorhandenes != null) {
            fuelleFelder(vorhandenes);
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

        preisFeld.setToolTipText("Pflichtfeld — z. B. 19,90 (Komma oder Punkt als Dezimaltrennzeichen)");

        int zeile = 0;
        zeile = formularZeile(formular, c, zeile, "Bezeichnung: *", bezeichnungFeld);
        zeile = formularZeile(formular, c, zeile, "Beschreibung:", beschreibungFeld);
        zeile = formularZeile(formular, c, zeile, "Einzelpreis (netto): *", preisFeld);
        zeile = formularZeile(formular, c, zeile, "Steuersatz: *", steuersatzWahl);
        zeile = formularZeile(formular, c, zeile, "Einheit:", einheitFeld);

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

    private void fuelleFelder(Produkt produkt) {
        bezeichnungFeld.setText(produkt.getBezeichnung());
        beschreibungFeld.setText(produkt.getBeschreibung() == null ? "" : produkt.getBeschreibung());
        preisFeld.setText(produkt.getEinzelpreisNetto().toPlainString());
        steuersatzWahl.setSelectedIndex(switch (produkt.getSteuersatz().stripTrailingZeros().toPlainString()) {
            case "0.19" -> 0;
            case "0.07" -> 1;
            default -> 2;
        });
        einheitFeld.setText(produkt.getEinheit() == null ? "" : produkt.getEinheit());
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
        for (JTextField feld : List.of(bezeichnungFeld, beschreibungFeld, preisFeld, einheitFeld)) {
            feld.getDocument().addDocumentListener(listener);
        }
        steuersatzWahl.addActionListener(e -> ungespeichert = true);
    }

    private void speichere() {
        MeldungsAnzeige.mitFehlerbehandlung(this, felder, () -> {
            Produkt produkt = vorhandeneNummer == null
                    ? new Produkt()
                    : service.findeProdukt(vorhandeneNummer);
            produkt.setBezeichnung(bezeichnungFeld.getText().trim());
            produkt.setBeschreibung(leerZuNull(beschreibungFeld.getText()));
            produkt.setEinzelpreisNetto(parsePreis(preisFeld.getText()));
            produkt.setSteuersatz(gewaehlterSteuersatz());
            produkt.setEinheit(leerZuNull(einheitFeld.getText()));

            Produkt gespeichert = vorhandeneNummer == null
                    ? service.legeAn(produkt)
                    : service.aendere(produkt);
            ungespeichert = false;
            MeldungsAnzeige.zeige(this, Meldung.erfolg("Das Produkt wurde gespeichert. Produktnummer: "
                    + gespeichert.getProduktnummer()), felder);
            dispose();
        });
    }

    /** Akzeptiert deutsches und englisches Dezimaltrennzeichen. */
    private BigDecimal parsePreis(String text) {
        String wert = text.trim().replace(',', '.');
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
        return switch (steuersatzWahl.getSelectedIndex()) {
            case 0 -> new BigDecimal("0.19");
            case 1 -> new BigDecimal("0.07");
            default -> new BigDecimal("0.00");
        };
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
