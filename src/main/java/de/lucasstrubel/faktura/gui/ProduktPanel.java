package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktCsvExport;
import de.lucasstrubel.faktura.produkte.ProduktVerwaltungsService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Modulansicht Produktverwaltung (D-F-03 bis F-05): sortierte Liste in voller
 * Breite mit Suchfeld; Anlegen und Bearbeiten erfolgen über die modale
 * Formular-Maske {@link ProduktFormularDialog} (Aufruf per Knopf oder
 * Doppelklick), die Aktionen liegen unter der Tabelle.
 */
public class ProduktPanel extends JPanel implements ModulPanel {

    private final ProduktVerwaltungsService service;
    private final StammdatenController controller;
    private final ProduktCsvExport csvExport;

    private final JTextField suchfeld = new JTextField(20);
    private final ProduktTabellenModel tabellenModel = new ProduktTabellenModel();
    private final JTable tabelle = new JTable(tabellenModel);
    private final JLabel trefferAnzeige = UiHilfen.trefferLabel();

    private final JButton bearbeitenKnopf = new JButton("Bearbeiten…");
    private final JButton loeschenKnopf = new JButton("Löschen");

    /**
     * @param service     Fachkomponente der Gruppe B für Anlegen/Ändern/Löschen
     * @param controller  GUI-freier Controller für Suche und Listeninhalt (D-F-03)
     * @param csvExport   CSV-Export der Produktstammdaten (B-F-13)
     * @param ereignisBus Observer-Verteiler für Aktualisierungen nach Datenänderungen
     */
    public ProduktPanel(ProduktVerwaltungsService service, StammdatenController controller,
                        ProduktCsvExport csvExport, EreignisBus ereignisBus) {
        this.service = service;
        this.controller = controller;
        this.csvExport = csvExport;
        baueOberflaeche();
        aktualisiere();
        ereignisBus.abonniere(DatenBereich.PRODUKTE, this::aktualisiere);
    }

    private void baueOberflaeche() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel suchleiste = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        suchleiste.add(new JLabel("Suche:"));
        UiHilfen.platzhalter(suchfeld, "Bezeichnung oder Produktnummer…");
        suchfeld.setToolTipText("Suche nach Bezeichnung oder Produktnummer (Strg+F)");
        suchleiste.add(suchfeld);
        suchfeld.getDocument().addDocumentListener(neuerSuchListener());
        add(suchleiste, BorderLayout.NORTH);

        tabelle.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TabellenFormat.konfiguriere(tabelle);
        tabelle.getColumnModel().getColumn(2).setCellRenderer(TabellenFormat.waehrungsRenderer());
        tabelle.getSelectionModel().addListSelectionListener(e -> aktualisiereAktionen());
        tabelle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    bearbeite();
                }
            }
        });

        JPanel listenSeite = new JPanel(new BorderLayout(0, 4));
        listenSeite.add(new JScrollPane(tabelle), BorderLayout.CENTER);
        listenSeite.add(trefferAnzeige, BorderLayout.SOUTH);
        add(listenSeite, BorderLayout.CENTER);

        JPanel aktionen = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton neuKnopf = new JButton("Neues Produkt anlegen…");
        neuKnopf.setMnemonic('N');
        neuKnopf.setToolTipText("Öffnet die Maske für ein neues Produkt (Alt+N)");
        neuKnopf.addActionListener(e -> legeNeuAn());
        bearbeitenKnopf.setMnemonic('B');
        bearbeitenKnopf.addActionListener(e -> bearbeite());
        loeschenKnopf.setMnemonic('L');
        loeschenKnopf.addActionListener(e -> loesche());
        JButton exportKnopf = new JButton("CSV exportieren…");
        exportKnopf.setMnemonic('C');
        exportKnopf.addActionListener(e -> exportiere());
        aktionen.add(neuKnopf);
        aktionen.add(bearbeitenKnopf);
        aktionen.add(loeschenKnopf);
        aktionen.add(exportKnopf);
        add(aktionen, BorderLayout.SOUTH);

        UiHilfen.strgF(this, suchfeld);
        aktualisiereAktionen();
    }

    private Produkt auswahl() {
        int zeile = tabelle.getSelectedRow();
        return zeile < 0 ? null
                : tabellenModel.gibZeile(tabelle.convertRowIndexToModel(zeile));
    }

    private void aktualisiereAktionen() {
        boolean ausgewaehlt = auswahl() != null;
        String hinweis = "Bitte zuerst ein Produkt in der Liste auswählen.";
        UiHilfen.schalteAktion(bearbeitenKnopf, ausgewaehlt, hinweis);
        UiHilfen.schalteAktion(loeschenKnopf, ausgewaehlt, hinweis);
    }

    private void legeNeuAn() {
        new ProduktFormularDialog(SwingUtilities.getWindowAncestor(this), service, null)
                .setVisible(true);
    }

    private void bearbeite() {
        Produkt produkt = auswahl();
        if (produkt == null) {
            return;
        }
        new ProduktFormularDialog(SwingUtilities.getWindowAncestor(this), service, produkt)
                .setVisible(true);
    }

    private void loesche() {
        Produkt produkt = auswahl();
        if (produkt == null) {
            return;
        }
        int antwort = JOptionPane.showConfirmDialog(this,
                "Produkt " + produkt.getProduktnummer() + " wirklich dauerhaft löschen?",
                "Produkt löschen", JOptionPane.YES_NO_OPTION);
        if (antwort != JOptionPane.YES_OPTION) {
            return;
        }
        MeldungsAnzeige.mitFehlerbehandlung(this, null, () -> {
            service.loescheProdukt(produkt.getProduktnummer());
            MeldungsAnzeige.zeige(this, Meldung.erfolg("Das Produkt wurde gelöscht."), null);
        });
    }

    private void exportiere() {
        JFileChooser auswahlDialog = new JFileChooser();
        auswahlDialog.setSelectedFile(new File("produkte.csv"));
        if (auswahlDialog.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            MeldungsAnzeige.mitFehlerbehandlung(this, null, () -> {
                csvExport.exportiereCsv(auswahlDialog.getSelectedFile().toPath());
                MeldungsAnzeige.zeige(this, Meldung.erfolg("Die Produktstammdaten wurden exportiert nach "
                        + auswahlDialog.getSelectedFile()), null);
            });
        }
    }

    @Override
    public boolean hatUngespeicherteAenderungen() {
        // Eingaben erfolgen ausschließlich in der modalen Formular-Maske;
        // dort schützt die eigene Nachfrage vor Datenverlust (D-F-02).
        return false;
    }

    @Override
    public void aktualisiere() {
        List<Produkt> liste = controller.produkteListe(suchfeld.getText());
        tabellenModel.setze(liste);
        trefferAnzeige.setText(UiHilfen.trefferText(liste.size(),
                controller.produkteListe("").size(), "Produkte", suchfeld.getText()));
    }

    private DocumentListener neuerSuchListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aktualisiere();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aktualisiere();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aktualisiere();
            }
        };
    }

    private static final class ProduktTabellenModel extends AbstractTableModel {

        private static final String[] SPALTEN = {"Produktnummer", "Bezeichnung", "Einzelpreis (netto)", "Steuersatz", "Einheit"};

        private List<Produkt> produkte = new ArrayList<>();

        void setze(List<Produkt> neueProdukte) {
            this.produkte = new ArrayList<>(neueProdukte);
            fireTableDataChanged();
        }

        Produkt gibZeile(int zeile) {
            return produkte.get(zeile);
        }

        @Override
        public int getRowCount() {
            return produkte.size();
        }

        @Override
        public int getColumnCount() {
            return SPALTEN.length;
        }

        @Override
        public String getColumnName(int spalte) {
            return SPALTEN[spalte];
        }

        @Override
        public Class<?> getColumnClass(int spalte) {
            return spalte == 2 ? BigDecimal.class : String.class;
        }

        @Override
        public Object getValueAt(int zeile, int spalte) {
            Produkt produkt = produkte.get(zeile);
            return switch (spalte) {
                case 0 -> produkt.getProduktnummer();
                case 1 -> produkt.getBezeichnung();
                case 2 -> produkt.getEinzelpreisNetto();
                case 3 -> produkt.getSteuersatz().multiply(new BigDecimal("100"))
                        .stripTrailingZeros().toPlainString() + " %";
                default -> produkt.getEinheit() == null ? "" : produkt.getEinheit();
            };
        }
    }
}
