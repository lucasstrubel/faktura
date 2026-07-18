package de.lucasstrubel.faktura.gui;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Gemeinsame Bedienhilfen der Oberfläche: Tastaturkürzel, Platzhaltertexte,
 * Pflichtfeld-Legende und Trefferanzeige. Unterstützt die einheitliche
 * Bedienbarkeit aller Modulansichten und Dialoge (Q-05, Q-09).
 */
public final class UiHilfen {

    private UiHilfen() {
    }

    /** Grauer Platzhaltertext im leeren Eingabefeld (FlatLaf-Eigenschaft). */
    public static void platzhalter(JTextField feld, String text) {
        feld.putClientProperty("JTextField.placeholderText", text);
    }

    /** ESC führt die Schließ-Aktion des Dialogs aus (inkl. Nachfrage-Logik). */
    public static void escSchliesst(JDialog dialog, Runnable schliessAktion) {
        bindeTaste(dialog.getRootPane(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "dialogSchliessen", schliessAktion);
    }

    /**
     * Leitet auch das Schließen über die Fensterleiste (X) auf die
     * Schließ-Aktion um, damit die Nachfrage nicht umgangen wird.
     */
    public static void fensterSchliessenAbfangen(JDialog dialog, Runnable schliessAktion) {
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                schliessAktion.run();
            }
        });
    }

    /** Strg+F setzt den Fokus in das Suchfeld der sichtbaren Modulansicht. */
    public static void strgF(JComponent panel, JTextField suchfeld) {
        bindeTaste(panel, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK),
                "sucheFokussieren", () -> {
                    suchfeld.requestFocusInWindow();
                    suchfeld.selectAll();
                });
    }

    /** Strg+S speichert das Formular der sichtbaren Modulansicht. */
    public static void strgS(JComponent panel, Runnable speichern) {
        bindeTaste(panel, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK),
                "formularSpeichern", speichern);
    }

    private static void bindeTaste(JComponent komponente, KeyStroke taste,
                                   String aktionsName, Runnable aktion) {
        komponente.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(taste, aktionsName);
        komponente.getActionMap().put(aktionsName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                aktion.run();
            }
        });
    }

    /** Schaltet einen Aktionsknopf und erklärt im Tooltip, warum er gesperrt ist. */
    public static void schalteAktion(JButton knopf, boolean aktiv, String grundWennGesperrt) {
        knopf.setEnabled(aktiv);
        knopf.setToolTipText(aktiv ? null : grundWennGesperrt);
    }

    /** Kleines graues Hinweis-Label „* Pflichtfeld" unter Formularen (Q-09). */
    public static JLabel pflichtfeldLegende() {
        JLabel legende = new JLabel("* Pflichtfeld");
        legende.setForeground(dezentesGrau());
        legende.putClientProperty("FlatLaf.styleClass", "small");
        return legende;
    }

    /** Graues Label für Trefferanzahl bzw. Leerzustand unter einer Tabelle. */
    public static JLabel trefferLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(dezentesGrau());
        label.setBorder(BorderFactory.createEmptyBorder(0, 4, 2, 4));
        return label;
    }

    /**
     * Text der Trefferanzeige: Anzahl der gezeigten von allen Einträgen,
     * verständlicher Leerzustand bei erfolgloser Suche oder leerem Bestand.
     *
     * @param gezeigt     Anzahl der aktuell angezeigten Einträge
     * @param gesamt      Gesamtbestand ohne Filter
     * @param einheit     Mehrzahlbegriff, z. B. "Kunden", "Produkte", "Belege"
     * @param suchbegriff aktueller Such-/Filtertext (leer = kein Filter)
     */
    public static String trefferText(int gezeigt, int gesamt, String einheit, String suchbegriff) {
        if (gesamt == 0) {
            return "Noch keine " + einheit + " vorhanden";
        }
        if (gezeigt == 0) {
            return "Keine Treffer für „" + suchbegriff.trim() + "“";
        }
        return gezeigt + " von " + gesamt + " " + einheit;
    }

    private static Color dezentesGrau() {
        Color farbe = UIManager.getColor("Label.disabledForeground");
        return farbe == null ? new Color(110, 110, 110) : farbe;
    }
}
