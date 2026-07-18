package de.lucasstrubel.faktura.gemeinsam;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Einfacher synchroner Ereignis-Verteiler (Observer-Muster): Die Services
 * melden nach jeder schreibenden Operation den geänderten {@link DatenBereich},
 * die Modulansichten abonnieren die für sie relevanten Bereiche und
 * aktualisieren sich selbst. Dadurch entfallen manuelle Refresh-Aufrufe
 * zwischen den Modulen.
 *
 * <p>Alle Aufrufe laufen auf dem Event-Dispatch-Thread der Swing-Oberfläche;
 * eine Synchronisierung ist daher nicht erforderlich (Einzelplatzbetrieb).
 */
public class EreignisBus {

    private final Map<DatenBereich, List<Runnable>> abonnenten = new EnumMap<>(DatenBereich.class);

    /** Registriert einen Beobachter für Änderungen im angegebenen Bereich. */
    public void abonniere(DatenBereich bereich, Runnable beobachter) {
        abonnenten.computeIfAbsent(bereich, b -> new ArrayList<>()).add(beobachter);
    }

    /** Meldet eine Änderung im angegebenen Bereich an alle Beobachter. */
    public void melde(DatenBereich bereich) {
        for (Runnable beobachter : abonnenten.getOrDefault(bereich, List.of())) {
            beobachter.run();
        }
    }
}
