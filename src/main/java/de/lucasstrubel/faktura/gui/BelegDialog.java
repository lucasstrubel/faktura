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
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
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
public class BelegDialog extends JDialog {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DokumentService dokumentService;

    private final JComboBox<Belegtyp> typWahl = new JComboBox<>(
            new Belegtyp[]{Belegtyp.ANGEBOT, Belegtyp.AUFTRAGSBESTAETIGUNG, Belegtyp.LIEFERSCHEIN});
    private final JComboBox<Kunde> kundenWahl = new JComboBox<>();
    private final JComboBox<Produkt> produktWahl = new JComboBox<>();
    private final JSpinner mengeWahl = new JSpinner(new SpinnerNumberModel(1, 1, 99999, 1));
    private final DefaultListModel<String> positionsListenModel = new DefaultListModel<>();
    private final JList<String> positionsListe = new JList<>(positionsListenModel);
    private final JLabel datumBeschriftung = new JLabel("Gültig bis (leer = +30 Tage):");
    private final JTextField datumFeld = new JTextField(10);

    private final List<Positionsangabe> positionen = new ArrayList<>();

    public BelegDialog(Window besitzer, DokumentService dokumentService,
                       KundenService kundenService, ProduktService produktService) {
        super(besitzer, "Neuen Beleg erstellen", ModalityType.APPLICATION_MODAL);
        this.dokumentService = dokumentService;

        kundenWahl.setModel(new DefaultComboBoxModel<>(
                kundenService.suche("").toArray(new Kunde[0])));
        produktWahl.setModel(new DefaultComboBoxModel<>(
                produktService.suche("").toArray(new Produkt[0])));

        baueOberflaeche();
        pack();
        setLocationRelativeTo(besitzer);
    }

    private void baueOberflaeche() {
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel kopf = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        kopf.add(new JLabel("Belegtyp: *"), c);
        c.gridx = 1;
        typWahl.addActionListener(e -> aktualisiereDatumsfeld());
        kopf.add(typWahl, c);

        c.gridx = 0;
        c.gridy = 1;
        kopf.add(new JLabel("Kunde: *"), c);
        c.gridx = 1;
        kopf.add(kundenWahl, c);

        c.gridx = 0;
        c.gridy = 2;
        kopf.add(datumBeschriftung, c);
        c.gridx = 1;
        datumFeld.setToolTipText("Optional — Format: TT.MM.JJJJ");
        kopf.add(datumFeld, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        kopf.add(UiHilfen.pflichtfeldLegende(), c);
        add(kopf, BorderLayout.NORTH);

        JPanel mitte = new JPanel(new BorderLayout(5, 5));
        JPanel eingabe = new JPanel(new FlowLayout(FlowLayout.LEFT));
        eingabe.add(new JLabel("Produkt:"));
        eingabe.add(produktWahl);
        eingabe.add(new JLabel("Menge:"));
        eingabe.add(mengeWahl);
        JButton hinzufuegen = new JButton("Hinzufügen");
        hinzufuegen.setMnemonic('H');
        hinzufuegen.addActionListener(e -> fuegePositionHinzu());
        eingabe.add(hinzufuegen);
        JButton entfernen = new JButton("Entfernen");
        entfernen.setMnemonic('N');
        entfernen.addActionListener(e -> entfernePosition());
        eingabe.add(entfernen);
        mitte.add(eingabe, BorderLayout.NORTH);
        mitte.add(new JScrollPane(positionsListe), BorderLayout.CENTER);
        add(mitte, BorderLayout.CENTER);

        JPanel knoepfe = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton abbrechen = new JButton("Abbrechen");
        abbrechen.setMnemonic('A');
        abbrechen.addActionListener(e -> abbrechenMitNachfrage());
        JButton erstellen = new JButton("Erstellen");
        erstellen.setMnemonic('E');
        erstellen.addActionListener(e -> erstelle());
        knoepfe.add(abbrechen);
        knoepfe.add(erstellen);
        add(knoepfe, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(erstellen);
        UiHilfen.escSchliesst(this, this::abbrechenMitNachfrage);
        UiHilfen.fensterSchliessenAbfangen(this, this::abbrechenMitNachfrage);
    }

    /** Schutz vor Datenverlust: Nachfrage, wenn bereits Positionen erfasst wurden. */
    private void abbrechenMitNachfrage() {
        if (!positionen.isEmpty()) {
            int antwort = JOptionPane.showConfirmDialog(this,
                    "Die erfassten Positionen gehen verloren. Dialog wirklich schließen?",
                    "Eingaben verwerfen", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (antwort != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dispose();
    }

    private void aktualisiereDatumsfeld() {
        Belegtyp typ = (Belegtyp) typWahl.getSelectedItem();
        datumBeschriftung.setText(switch (typ) {
            case ANGEBOT -> "Gültig bis (leer = +30 Tage):";
            case LIEFERSCHEIN -> "Lieferdatum (leer = heute):";
            default -> "Datum (entfällt):";
        });
        datumFeld.setEnabled(typ == Belegtyp.ANGEBOT || typ == Belegtyp.LIEFERSCHEIN);
    }

    private void fuegePositionHinzu() {
        Produkt produkt = (Produkt) produktWahl.getSelectedItem();
        if (produkt == null) {
            return;
        }
        int menge = (Integer) mengeWahl.getValue();
        positionen.add(new Positionsangabe(produkt.getProduktnummer(), menge));
        positionsListenModel.addElement(menge + " x " + produkt.getBezeichnung()
                + " (" + produkt.getProduktnummer() + ")");
    }

    private void entfernePosition() {
        int index = positionsListe.getSelectedIndex();
        if (index >= 0) {
            positionen.remove(index);
            positionsListenModel.remove(index);
        }
    }

    private void erstelle() {
        Kunde kunde = (Kunde) kundenWahl.getSelectedItem();
        String kundenNr = kunde == null ? null : kunde.getKundennummer();
        LocalDate datum;
        try {
            String text = datumFeld.getText().trim();
            datum = text.isEmpty() ? null : LocalDate.parse(text, DATUM);
        } catch (DateTimeParseException e) {
            MeldungsAnzeige.zeige(this, Meldung.fehler("Datum",
                    "Das Datum ist ungültig. Format: TT.MM.JJJJ"), null);
            return;
        }
        try {
            Belegtyp typ = (Belegtyp) typWahl.getSelectedItem();
            Dokument beleg = switch (typ) {
                case ANGEBOT -> dokumentService.erstelleAngebot(kundenNr, positionen, datum);
                case AUFTRAGSBESTAETIGUNG -> dokumentService.erstelleAuftragsbestaetigung(kundenNr, positionen);
                case LIEFERSCHEIN -> dokumentService.erstelleLieferschein(kundenNr, positionen, datum);
                default -> throw new IllegalStateException("Unerwarteter Belegtyp: " + typ);
            };
            MeldungsAnzeige.zeige(this, Meldung.erfolg(beleg.belegtyp().anzeigename() + " "
                    + beleg.getBelegnummer() + " wurde erstellt."), null);
            dispose();
        } catch (ValidierungsException e) {
            MeldungsAnzeige.zeige(this, Meldung.fehler(e.getFeldname(), e.getMessage()), null);
        }
    }
}
