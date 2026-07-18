package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.dokumente.DokumentStatus;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Gemeinsame Tabellendarstellung der Modulansichten (Q-05): Währungsbeträge
 * rechtsbündig im deutschen Format, Belegstatus farblich hervorgehoben,
 * einheitliche Zeilenhöhe und Sortierung per Spaltenkopf.
 */
public final class TabellenFormat {

    private TabellenFormat() {
    }

    /** Formatiert einen Betrag im deutschen Währungsformat, z. B. "1.234,50 €". */
    public static String formatiereBetrag(BigDecimal betrag) {
        return NumberFormat.getCurrencyInstance(Locale.GERMANY).format(betrag);
    }

    /** Einheitliche Grundeinstellungen: Zeilenhöhe und Sortierung per Spaltenkopf. */
    public static void konfiguriere(JTable tabelle) {
        tabelle.setRowHeight(26);
        tabelle.setAutoCreateRowSorter(true);
        tabelle.setFillsViewportHeight(true);
    }

    /** Rechtsbündiger Renderer für {@link BigDecimal}-Beträge im Währungsformat. */
    public static DefaultTableCellRenderer waehrungsRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object wert) {
                setText(wert instanceof BigDecimal betrag ? formatiereBetrag(betrag) : "");
            }
        };
        renderer.setHorizontalAlignment(JLabel.RIGHT);
        return renderer;
    }

    /** Renderer für {@link DokumentStatus}: dezente Statusfarbe, Selektion bleibt lesbar. */
    public static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tabelle, Object wert,
                    boolean selektiert, boolean fokus, int zeile, int spalte) {
                super.getTableCellRendererComponent(tabelle, wert, selektiert, fokus, zeile, spalte);
                if (wert instanceof DokumentStatus status) {
                    setText(status.name());
                    setForeground(selektiert ? tabelle.getSelectionForeground() : farbeFuer(status));
                } else {
                    setForeground(selektiert ? tabelle.getSelectionForeground()
                            : tabelle.getForeground());
                }
                return this;
            }
        };
    }

    private static Color farbeFuer(DokumentStatus status) {
        return switch (status) {
            // dunkleres Grau: Kontrast >= 4,5:1 auch auf der alternierenden Zeilenfarbe
            case ENTWURF -> new Color(90, 90, 90);
            case OFFEN -> new Color(0, 90, 180);
            case VERSENDET -> new Color(0, 130, 60);
            case STORNIERT -> new Color(180, 40, 40);
        };
    }
}
