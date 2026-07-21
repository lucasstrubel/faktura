package de.lucasstrubel.faktura.gemeinsam;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Einfacher synchroner Ereignis-Verteiler (Observer-Muster): Die Services
 * veröffentlichen nach jeder schreibenden Operation ein
 * {@link DatenGeaendertEreignis} über den Spring-{@code ApplicationEventPublisher};
 * dieser Bus empfängt es ({@link #empfange}) und benachrichtigt die
 * abonnierten Modulansichten. Dadurch entfallen manuelle Refresh-Aufrufe
 * zwischen den Modulen, und die Ansichten müssen keine Spring-Beans sein.
 *
 * <p>Die Zustellung erfolgt erst <em>nach</em> dem Commit der auslösenden
 * Transaktion: Rollt eine Transaktion zurück — etwa weil das Speichern
 * scheitert — darf die Oberfläche keinen Datensatz anzeigen, den es nicht
 * gibt. {@code fallbackExecution} hält Ereignisse aus Aufrufen ohne laufende
 * Transaktion weiterhin am Leben.
 *
 * <p>Alle Aufrufe laufen auf dem JavaFX-Application-Thread; eine
 * Synchronisierung ist daher nicht erforderlich (Einzelplatzbetrieb).
 */
@Component
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

    /** Brücke vom Spring-Ereignissystem zu den abonnierten Ansichten. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void empfange(DatenGeaendertEreignis ereignis) {
        melde(ereignis.bereich());
    }
}
