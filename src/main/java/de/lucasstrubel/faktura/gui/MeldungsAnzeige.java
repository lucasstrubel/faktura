package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.gemeinsam.LoeschAbgelehntException;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Einheitliche Darstellung von Fehler- und Erfolgsmeldungen (D-F-16, F-17):
 * Das betroffene Eingabefeld wird optisch markiert UND die Meldung benennt
 * das Feld namentlich (Q-09).
 */
public final class MeldungsAnzeige {

    private static final Border FEHLER_RAND = BorderFactory.createLineBorder(Color.RED, 2);

    private MeldungsAnzeige() {
    }

    /**
     * Zeigt die Meldung als Dialog an und markiert bei Validierungsfehlern
     * das betroffene Feld rot; alle übrigen Felder werden zurückgesetzt.
     */
    public static void zeige(Component parent, Meldung meldung, Map<String, JComponent> felder) {
        if (felder != null) {
            felder.forEach((name, feld) -> feld.setBorder(UIManager.getBorder("TextField.border")));
            if (meldung != null && meldung.feldname() != null) {
                JComponent feld = felder.get(meldung.feldname());
                if (feld != null) {
                    feld.setBorder(FEHLER_RAND);
                    feld.requestFocusInWindow();
                }
            }
        }
        if (meldung == null) {
            return;
        }
        if (meldung.typ() == MeldungsTyp.FEHLER) {
            JOptionPane.showMessageDialog(parent, meldung.text(),
                    meldung.feldname() != null ? "Eingabe unvollständig: " + meldung.feldname() : "Fehler",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent, meldung.text(), "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Zentrale Fehlerbehandlung der Modulansichten: führt die Aktion aus und
     * zeigt fachliche Fehler einheitlich an — Validierungsfehler mit
     * Feldmarkierung (Q-09), abgelehnte Löschvorgänge (GR-04), unzulässige
     * Statuswechsel (GR-02) und Persistenzfehler beim Speichern (IF-01).
     */
    public static void mitFehlerbehandlung(Component parent, Map<String, JComponent> felder,
                                           Runnable aktion) {
        try {
            aktion.run();
        } catch (ValidierungsException e) {
            zeige(parent, Meldung.fehler(e.getFeldname(), e.getMessage()), felder);
        } catch (LoeschAbgelehntException | IllegalStateException e) {
            zeige(parent, Meldung.fehler(null, e.getMessage()), felder);
        } catch (UncheckedIOException e) {
            zeige(parent, Meldung.fehler(null,
                    "Die Daten konnten nicht gespeichert werden: " + e.getMessage()), felder);
        } catch (RuntimeException e) {
            // Letztes Netz: kein stilles Scheitern auf dem Event-Dispatch-Thread
            zeige(parent, Meldung.fehler(null, "Unerwarteter Fehler: " + e), felder);
        }
    }
}
