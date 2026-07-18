package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenCsvExport;
import de.lucasstrubel.faktura.kunden.KundenVerwaltungsService;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Modulansicht Kundenverwaltung (D-F-03 bis F-05): sortierte Liste in voller
 * Breite mit Suchfeld; Anlegen und Bearbeiten erfolgen über die modale
 * Formular-Maske {@link KundenFormularDialog} (Aufruf per Knopf oder
 * Doppelklick), die Aktionen liegen unter der Tabelle.
 */
public class KundenPanel extends JPanel implements ModulPanel {

    private final KundenVerwaltungsService service;
    private final StammdatenController controller;
    private final KundenCsvExport csvExport;

    private final JTextField suchfeld = new JTextField(20);
    private final KundenTabellenModel tabellenModel = new KundenTabellenModel();
    private final JTable tabelle = new JTable(tabellenModel);
    private final JLabel trefferAnzeige = UiHilfen.trefferLabel();

    private final JButton bearbeitenKnopf = new JButton("Bearbeiten…");
    private final JButton loeschenKnopf = new JButton("Löschen");

    /**
     * @param service     Fachkomponente der Komponente C für Anlegen/Ändern/Löschen
     * @param controller  GUI-freier Controller für Suche und Listeninhalt (D-F-03)
     * @param csvExport   CSV-Export der Kundenstammdaten (C-F-13)
     * @param ereignisBus Observer-Verteiler für Aktualisierungen nach Datenänderungen
     */
    public KundenPanel(KundenVerwaltungsService service, StammdatenController controller,
                       KundenCsvExport csvExport, EreignisBus ereignisBus) {
        this.service = service;
        this.controller = controller;
        this.csvExport = csvExport;
        baueOberflaeche();
        aktualisiere();
        ereignisBus.abonniere(DatenBereich.KUNDEN, this::aktualisiere);
    }

    private void baueOberflaeche() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel suchleiste = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        suchleiste.add(new JLabel("Suche:"));
        UiHilfen.platzhalter(suchfeld, "Name oder Kundennummer…");
        suchfeld.setToolTipText("Suche nach Name oder Kundennummer (Strg+F)");
        suchleiste.add(suchfeld);
        suchfeld.getDocument().addDocumentListener(neuerSuchListener());
        add(suchleiste, BorderLayout.NORTH);

        tabelle.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TabellenFormat.konfiguriere(tabelle);
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
        JButton neuKnopf = new JButton("Neuen Kunden anlegen…");
        neuKnopf.setMnemonic('N');
        neuKnopf.setToolTipText("Öffnet die Maske für einen neuen Kunden (Alt+N)");
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

    private Kunde auswahl() {
        int zeile = tabelle.getSelectedRow();
        return zeile < 0 ? null
                : tabellenModel.gibZeile(tabelle.convertRowIndexToModel(zeile));
    }

    private void aktualisiereAktionen() {
        boolean ausgewaehlt = auswahl() != null;
        String hinweis = "Bitte zuerst einen Kunden in der Liste auswählen.";
        UiHilfen.schalteAktion(bearbeitenKnopf, ausgewaehlt, hinweis);
        UiHilfen.schalteAktion(loeschenKnopf, ausgewaehlt, hinweis);
    }

    private void legeNeuAn() {
        new KundenFormularDialog(SwingUtilities.getWindowAncestor(this), service, null)
                .setVisible(true);
    }

    private void bearbeite() {
        Kunde kunde = auswahl();
        if (kunde == null) {
            return;
        }
        new KundenFormularDialog(SwingUtilities.getWindowAncestor(this), service, kunde)
                .setVisible(true);
    }

    private void loesche() {
        Kunde kunde = auswahl();
        if (kunde == null) {
            return;
        }
        int antwort = JOptionPane.showConfirmDialog(this,
                "Kunde " + kunde.getKundennummer() + " wirklich dauerhaft löschen?",
                "Kunde löschen", JOptionPane.YES_NO_OPTION);
        if (antwort != JOptionPane.YES_OPTION) {
            return;
        }
        MeldungsAnzeige.mitFehlerbehandlung(this, null, () -> {
            service.loescheKunde(kunde.getKundennummer());
            MeldungsAnzeige.zeige(this, Meldung.erfolg("Der Kunde wurde gelöscht."), null);
        });
    }

    private void exportiere() {
        JFileChooser auswahlDialog = new JFileChooser();
        auswahlDialog.setSelectedFile(new File("kunden.csv"));
        if (auswahlDialog.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            MeldungsAnzeige.mitFehlerbehandlung(this, null, () -> {
                csvExport.exportiereCsv(auswahlDialog.getSelectedFile().toPath());
                MeldungsAnzeige.zeige(this, Meldung.erfolg("Die Kundenstammdaten wurden exportiert nach "
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
        List<Kunde> liste = controller.kundenListe(suchfeld.getText());
        tabellenModel.setze(liste);
        trefferAnzeige.setText(UiHilfen.trefferText(liste.size(),
                controller.kundenListe("").size(), "Kunden", suchfeld.getText()));
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

    private static final class KundenTabellenModel extends AbstractTableModel {

        private static final String[] SPALTEN = {"Kundennummer", "Name", "Straße", "PLZ", "Ort"};

        private List<Kunde> kunden = new ArrayList<>();

        void setze(List<Kunde> neueKunden) {
            this.kunden = new ArrayList<>(neueKunden);
            fireTableDataChanged();
        }

        Kunde gibZeile(int zeile) {
            return kunden.get(zeile);
        }

        @Override
        public int getRowCount() {
            return kunden.size();
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
        public Object getValueAt(int zeile, int spalte) {
            Kunde kunde = kunden.get(zeile);
            return switch (spalte) {
                case 0 -> kunde.getKundennummer();
                case 1 -> kunde.getName();
                case 2 -> kunde.getStrasse();
                case 3 -> kunde.getPlz();
                default -> kunde.getOrt();
            };
        }
    }
}
