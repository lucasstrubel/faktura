package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.Belegtyp;
import de.lucasstrubel.faktura.dokumente.Dokument;
import de.lucasstrubel.faktura.dokumente.DokumentCsvExport;
import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.dokumente.DokumentStatus;
import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.ProduktService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Modulansicht Dokumente (D-F-06 bis F-08, F-14, F-15): Dokumentliste mit
 * Statusfilter, Belegaktionen (PDF-Export, optional Druck und E-Mail),
 * geführte Rechnungserstellung (Wizard) und Stornierung mit
 * Bestätigungsdialog.
 */
public class DokumentListenPanel extends JPanel implements ModulPanel {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DokumentService dokumentService;
    private final KundenService kundenService;
    private final ProduktService produktService;
    private final DokumentCsvExport datenExport;
    private final DokumentListenController controller;

    /** Statusfilter; {@code null} steht für "Alle" (F-06). */
    private final JComboBox<DokumentStatus> statusFilter = new JComboBox<>(
            statusFilterWerte());
    private final DokumentTabellenModel tabellenModel = new DokumentTabellenModel();
    private final JTable tabelle = new JTable(tabellenModel);

    private final JButton folgebelegKnopf = new JButton("Folgebeleg erzeugen");
    private final JButton versendenKnopf = new JButton("Versenden");
    private final JButton stornierenKnopf = new JButton("Stornieren");
    private final JButton pdfKnopf = new JButton("PDF exportieren…");
    private final JButton druckenKnopf = new JButton("Drucken");
    private final JButton mailKnopf = new JButton("Per E-Mail senden");
    private final JLabel trefferAnzeige = UiHilfen.trefferLabel();

    public DokumentListenPanel(DokumentService dokumentService,
                               KundenService kundenService,
                               ProduktService produktService,
                               DokumentCsvExport datenExport,
                               EreignisBus ereignisBus) {
        this.dokumentService = dokumentService;
        this.kundenService = kundenService;
        this.produktService = produktService;
        this.datenExport = datenExport;
        this.controller = new DokumentListenController(dokumentService);
        baueOberflaeche();
        aktualisiere();
        ereignisBus.abonniere(DatenBereich.DOKUMENTE, this::aktualisiere);
        // Kundenname und -nummer werden in der Belegliste angezeigt
        ereignisBus.abonniere(DatenBereich.KUNDEN, this::aktualisiere);
    }

    private void baueOberflaeche() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel kopf = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        kopf.add(new JLabel("Statusfilter:"));
        kopf.add(statusFilter);
        statusFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> liste, Object wert,
                    int index, boolean selektiert, boolean fokus) {
                return super.getListCellRendererComponent(liste,
                        wert == null ? "Alle" : wert, index, selektiert, fokus);
            }
        });
        statusFilter.addActionListener(e -> aktualisiere());

        JButton neueRechnung = new JButton("Neue Rechnung (Assistent)…");
        neueRechnung.setMnemonic('R');
        neueRechnung.setToolTipText("Geführte Rechnungserstellung in fünf Schritten (Alt+R)");
        neueRechnung.addActionListener(e -> oeffneWizard());
        JButton neuerBeleg = new JButton("Neuer Beleg…");
        neuerBeleg.setMnemonic('B');
        neuerBeleg.setToolTipText("Angebot, Auftragsbestätigung oder Lieferschein erstellen (Alt+B)");
        neuerBeleg.addActionListener(e -> oeffneBelegDialog());
        JButton datenExportKnopf = new JButton("CSV exportieren…");
        datenExportKnopf.setMnemonic('C');
        datenExportKnopf.setToolTipText("Alle Belegdaten als CSV-Datei exportieren");
        datenExportKnopf.addActionListener(e -> exportiereDaten());
        kopf.add(neueRechnung);
        kopf.add(neuerBeleg);
        kopf.add(datenExportKnopf);
        add(kopf, BorderLayout.NORTH);

        tabelle.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelle.getSelectionModel().addListSelectionListener(e -> aktualisiereAktionen());
        TabellenFormat.konfiguriere(tabelle);
        tabelle.getColumnModel().getColumn(4).setCellRenderer(TabellenFormat.waehrungsRenderer());
        tabelle.getColumnModel().getColumn(5).setCellRenderer(TabellenFormat.statusRenderer());
        JPanel listenSeite = new JPanel(new BorderLayout(0, 4));
        listenSeite.add(new JScrollPane(tabelle), BorderLayout.CENTER);
        listenSeite.add(trefferAnzeige, BorderLayout.SOUTH);
        add(listenSeite, BorderLayout.CENTER);

        JPanel aktionen = new JPanel(new FlowLayout(FlowLayout.LEFT));
        folgebelegKnopf.setMnemonic('F');
        versendenKnopf.setMnemonic('V');
        stornierenKnopf.setMnemonic('O');
        pdfKnopf.setMnemonic('P');
        druckenKnopf.setMnemonic('D');
        mailKnopf.setMnemonic('E');
        folgebelegKnopf.addActionListener(e -> erzeugeFolgebeleg());
        versendenKnopf.addActionListener(e -> versende());
        stornierenKnopf.addActionListener(e -> storniere());
        pdfKnopf.addActionListener(e -> exportierePdf());
        druckenKnopf.addActionListener(e -> drucke());
        mailKnopf.addActionListener(e -> sendePerMail());
        aktionen.add(folgebelegKnopf);
        aktionen.add(versendenKnopf);
        aktionen.add(stornierenKnopf);
        aktionen.add(pdfKnopf);
        aktionen.add(druckenKnopf);
        aktionen.add(mailKnopf);
        add(aktionen, BorderLayout.SOUTH);
        aktualisiereAktionen();
    }

    private Dokument auswahl() {
        int zeile = tabelle.getSelectedRow();
        return zeile < 0 ? null
                : tabellenModel.gibZeile(tabelle.convertRowIndexToModel(zeile));
    }

    /** Aktiviert/deaktiviert die Belegaktionen gemäß Status (F-08, F-14). */
    private void aktualisiereAktionen() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            for (JButton knopf : List.of(folgebelegKnopf, versendenKnopf, stornierenKnopf,
                    pdfKnopf, druckenKnopf, mailKnopf)) {
                UiHilfen.schalteAktion(knopf, false, "Bitte zuerst einen Beleg in der Liste auswählen.");
            }
            return;
        }
        BelegAktionen verfuegbar = controller.aktionenFuer(dokument);
        DokumentStatus status = dokument.getStatus();
        UiHilfen.schalteAktion(folgebelegKnopf, dokument.belegtyp() != Belegtyp.RECHNUNG,
                "Für Rechnungen wird kein Folgebeleg erzeugt.");
        UiHilfen.schalteAktion(versendenKnopf, verfuegbar.aenderbar(),
                "Im Status " + status + " sind keine inhaltlichen Änderungen mehr möglich (GR-02).");
        UiHilfen.schalteAktion(stornierenKnopf, verfuegbar.stornierbar(),
                "Stornieren ist nur für Rechnungen im Status OFFEN möglich (aktuell: " + status + ").");
        String keinPdf = "Für diesen Beleg ist kein PDF-Export möglich.";
        UiHilfen.schalteAktion(pdfKnopf, verfuegbar.pdfExport(), keinPdf);
        UiHilfen.schalteAktion(druckenKnopf, verfuegbar.pdfExport(), keinPdf);
        UiHilfen.schalteAktion(mailKnopf, verfuegbar.pdfExport(), keinPdf);
    }

    private void oeffneWizard() {
        RechnungsWizardController wizardController =
                new RechnungsWizardController(dokumentService, kundenService, produktService);
        Window fenster = SwingUtilities.getWindowAncestor(this);
        RechnungsWizardDialog dialog = new RechnungsWizardDialog(fenster, wizardController,
                kundenService, produktService);
        dialog.setVisible(true);
    }

    private void oeffneBelegDialog() {
        Window fenster = SwingUtilities.getWindowAncestor(this);
        BelegDialog dialog = new BelegDialog(fenster, dokumentService, kundenService, produktService);
        dialog.setVisible(true);
    }

    private void erzeugeFolgebeleg() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        MeldungsAnzeige.mitFehlerbehandlung(this, null, () -> {
            Dokument folgebeleg = dokumentService.erzeugeFolgebeleg(dokument.getBelegnummer());
            MeldungsAnzeige.zeige(this, Meldung.erfolg(folgebeleg.belegtyp().anzeigename() + " "
                    + folgebeleg.getBelegnummer() + " wurde aus " + dokument.getBelegnummer()
                    + " erzeugt."), null);
        });
    }

    private void versende() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        int antwort = JOptionPane.showConfirmDialog(this,
                "Beleg " + dokument.getBelegnummer() + " als versendet markieren?\n"
                        + "Danach sind keine inhaltlichen Änderungen mehr möglich (GR-02).",
                "Versenden", JOptionPane.YES_NO_OPTION);
        if (antwort != JOptionPane.YES_OPTION) {
            return;
        }
        MeldungsAnzeige.mitFehlerbehandlung(this, null, () -> {
            dokumentService.versende(dokument.getBelegnummer());
            MeldungsAnzeige.zeige(this, Meldung.erfolg("Der Beleg " + dokument.getBelegnummer()
                    + " ist jetzt im Status VERSENDET."), null);
        });
    }

    /** Stornierung mit Bestätigungsdialog: Rechnungsnummer und Bruttosumme (F-15). */
    private void storniere() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        int antwort = JOptionPane.showConfirmDialog(this,
                "Rechnung " + dokument.getBelegnummer() + " über "
                        + dokument.getSummeBrutto().toPlainString() + " EUR (brutto) wirklich stornieren?",
                "Rechnung stornieren", JOptionPane.YES_NO_OPTION);
        Meldung meldung = controller.storniere(dokument.getBelegnummer(),
                antwort == JOptionPane.YES_OPTION);
        MeldungsAnzeige.zeige(this, meldung, null);
    }

    private void exportierePdf() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        JFileChooser auswahlDialog = new JFileChooser();
        auswahlDialog.setSelectedFile(new File(dokument.getBelegnummer() + ".pdf"));
        if (auswahlDialog.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path ziel = auswahlDialog.getSelectedFile().toPath();
            MeldungsAnzeige.mitFehlerbehandlung(this, null, () -> {
                dokumentService.exportierePdf(dokument.getBelegnummer(), ziel);
                MeldungsAnzeige.zeige(this, Meldung.erfolg("Das PDF wurde exportiert nach " + ziel), null);
            });
        }
    }

    /** Vollständiger Datenexport aller Belege als CSV (Q-08, IF-04). */
    private void exportiereDaten() {
        JFileChooser auswahlDialog = new JFileChooser();
        auswahlDialog.setSelectedFile(new File("dokumente.csv"));
        if (auswahlDialog.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path ziel = auswahlDialog.getSelectedFile().toPath();
            MeldungsAnzeige.mitFehlerbehandlung(this, null, () -> {
                datenExport.exportiereCsv(ziel);
                MeldungsAnzeige.zeige(this, Meldung.erfolg(
                        "Die Belegdaten wurden exportiert nach " + ziel), null);
            });
        }
    }

    /** Optionaler Druck über das Betriebssystem (IF-02). */
    private void drucke() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        try {
            Path temp = Files.createTempFile(dokument.getBelegnummer() + "-", ".pdf");
            dokumentService.exportierePdf(dokument.getBelegnummer(), temp);
            Desktop.getDesktop().print(temp.toFile());
        } catch (Exception e) {
            MeldungsAnzeige.zeige(this, Meldung.fehler(null,
                    "Drucken nicht möglich: " + e.getMessage()), null);
        }
    }

    /** Optionaler Versand über den Standard-E-Mail-Client (IF-03). */
    private void sendePerMail() {
        Dokument dokument = auswahl();
        if (dokument == null) {
            return;
        }
        try {
            Path temp = Files.createTempFile(dokument.getBelegnummer() + "-", ".pdf");
            dokumentService.exportierePdf(dokument.getBelegnummer(), temp);
            String betreff = URLEncoder.encode(dokument.belegtyp().anzeigename() + " "
                    + dokument.getBelegnummer(), StandardCharsets.UTF_8).replace("+", "%20");
            String text = URLEncoder.encode("Bitte das exportierte PDF anhängen:\n" + temp,
                    StandardCharsets.UTF_8).replace("+", "%20");
            Desktop.getDesktop().mail(new URI("mailto:?subject=" + betreff + "&body=" + text));
        } catch (Exception e) {
            MeldungsAnzeige.zeige(this, Meldung.fehler(null,
                    "E-Mail-Client konnte nicht geöffnet werden: " + e.getMessage()), null);
        }
    }

    @Override
    public boolean hatUngespeicherteAenderungen() {
        return false;
    }

    @Override
    public void aktualisiere() {
        DokumentStatus status = (DokumentStatus) statusFilter.getSelectedItem();
        List<Dokument> liste = controller.gefiltert(status);
        tabellenModel.setze(liste);
        int gesamt = status == null ? liste.size() : controller.gefiltert(null).size();
        if (gesamt == 0) {
            trefferAnzeige.setText("Noch keine Belege vorhanden");
        } else if (liste.isEmpty()) {
            trefferAnzeige.setText("Keine Belege im Status " + status);
        } else {
            trefferAnzeige.setText(liste.size() + " von " + gesamt + " Belegen");
        }
        aktualisiereAktionen();
    }

    /** "Alle" (= {@code null}) gefolgt von allen Belegstatus. */
    private static DokumentStatus[] statusFilterWerte() {
        DokumentStatus[] status = DokumentStatus.values();
        DokumentStatus[] werte = new DokumentStatus[status.length + 1];
        System.arraycopy(status, 0, werte, 1, status.length);
        return werte;
    }

    private static final class DokumentTabellenModel extends AbstractTableModel {

        private static final String[] SPALTEN =
                {"Belegnummer", "Typ", "Datum", "Kunde", "Bruttosumme", "Status"};

        private List<Dokument> dokumente = new ArrayList<>();

        void setze(List<Dokument> neueDokumente) {
            this.dokumente = new ArrayList<>(neueDokumente);
            fireTableDataChanged();
        }

        Dokument gibZeile(int zeile) {
            return dokumente.get(zeile);
        }

        @Override
        public int getRowCount() {
            return dokumente.size();
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
            return switch (spalte) {
                case 4 -> java.math.BigDecimal.class;
                case 5 -> DokumentStatus.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int zeile, int spalte) {
            Dokument dokument = dokumente.get(zeile);
            return switch (spalte) {
                case 0 -> dokument.getBelegnummer();
                case 1 -> dokument.belegtyp().anzeigename();
                case 2 -> dokument.getDatum() == null ? "" : DATUM.format(dokument.getDatum());
                case 3 -> dokument.getKundeName() + " (" + dokument.getKundenReferenz() + ")";
                case 4 -> dokument.getSummeBrutto();
                default -> dokument.getStatus();
            };
        }
    }
}
