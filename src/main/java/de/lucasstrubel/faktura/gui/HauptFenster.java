package de.lucasstrubel.faktura.gui;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hauptfenster mit Navigation zu den drei Modulen Kundenverwaltung,
 * Produktverwaltung und Dokumente (D-F-01). Das aktive Modul ist in der
 * Navigationsleiste sichtbar markiert; Alt+1/2/3 wechseln direkt. Beim
 * Modulwechsel wird bei ungespeicherten Formulareingaben nachgefragt (D-F-02).
 */
public class HauptFenster extends JFrame {

    private final CardLayout karten = new CardLayout();
    private final JPanel kartenPanel = new JPanel(karten);
    private final Map<String, ModulPanel> module = new LinkedHashMap<>();
    private final Map<String, JToggleButton> navigationsKnoepfe = new LinkedHashMap<>();

    private String aktuellesModul;

    public HauptFenster(KundenPanel kundenPanel, ProduktPanel produktPanel,
                        DokumentListenPanel dokumentePanel) {
        super("Faktura");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        module.put("Kunden", kundenPanel);
        module.put("Produkte", produktPanel);
        module.put("Dokumente", dokumentePanel);

        JToolBar navigation = new JToolBar();
        navigation.setFloatable(false);
        ButtonGroup gruppe = new ButtonGroup();
        int modulNummer = 1;
        for (Map.Entry<String, ModulPanel> eintrag : module.entrySet()) {
            JToggleButton knopf = new JToggleButton(eintrag.getKey());
            knopf.setMargin(new Insets(6, 14, 6, 14));
            knopf.setToolTipText(eintrag.getKey() + " anzeigen (Alt+" + modulNummer + ")");
            knopf.addActionListener(e -> wechsleZu(eintrag.getKey()));
            gruppe.add(knopf);
            navigation.add(knopf);
            navigationsKnoepfe.put(eintrag.getKey(), knopf);
            bindeModulTaste(eintrag.getKey(), modulNummer);
            modulNummer++;
        }
        add(navigation, BorderLayout.NORTH);

        kartenPanel.add(kundenPanel, "Kunden");
        kartenPanel.add(produktPanel, "Produkte");
        kartenPanel.add(dokumentePanel, "Dokumente");
        add(kartenPanel, BorderLayout.CENTER);

        aktuellesModul = "Kunden";
        navigationsKnoepfe.get(aktuellesModul).setSelected(true);
        karten.show(kartenPanel, aktuellesModul);

        setSize(1100, 650);
        setLocationRelativeTo(null);
    }

    /** Alt+1/2/3 wechselt direkt zum jeweiligen Modul. */
    private void bindeModulTaste(String modulName, int nummer) {
        KeyStroke taste = KeyStroke.getKeyStroke(KeyEvent.VK_0 + nummer, InputEvent.ALT_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(taste, "modul" + nummer);
        getRootPane().getActionMap().put("modul" + nummer, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wechsleZu(modulName);
            }
        });
    }

    /** Modulwechsel mit Nachfrage bei ungespeicherten Eingaben (D-F-02). */
    private void wechsleZu(String modulName) {
        if (modulName.equals(aktuellesModul)) {
            navigationsKnoepfe.get(aktuellesModul).setSelected(true);
            return;
        }
        ModulPanel aktuell = module.get(aktuellesModul);
        if (aktuell.hatUngespeicherteAenderungen()) {
            int antwort = JOptionPane.showConfirmDialog(this,
                    "Das Formular enthält ungespeicherte Eingaben. Modul trotzdem wechseln?",
                    "Ungespeicherte Eingaben", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (antwort != JOptionPane.YES_OPTION) {
                // Wechsel abgebrochen: Markierung zurück auf das aktuelle Modul
                navigationsKnoepfe.get(aktuellesModul).setSelected(true);
                return;
            }
        }
        aktuellesModul = modulName;
        navigationsKnoepfe.get(modulName).setSelected(true);
        module.get(modulName).aktualisiere();
        karten.show(kartenPanel, modulName);
    }
}
