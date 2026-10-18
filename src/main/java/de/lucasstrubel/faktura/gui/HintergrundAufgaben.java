package de.lucasstrubel.faktura.gui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Führt länger laufende Vorgänge außerhalb des FX-Application-Threads aus
 * (D-F-20, Q-05): PDF- und E-Rechnungs-Export, CSV-Ausgabe, Datensicherung,
 * Druck und Mailversand. Zuvor liefen diese Vorgänge direkt im
 * Ereignis-Handler — die Oberfläche fror für ihre gesamte Dauer ohne jede
 * Rückmeldung ein.
 *
 * <p>Während ein Vorgang läuft, wird das auslösende Bedienelement gesperrt und
 * der Mauszeiger auf „beschäftigt“ gesetzt; {@link #laeuft()} und
 * {@link #beschreibung()} sind für die Statuszeile der Hauptansicht gedacht.
 * Die Vorgänge laufen nacheinander auf einem Faden; gezählt wird, wie viele
 * noch ausstehen, damit die Statuszeile erst nach dem letzten wieder „Bereit“
 * zeigt. Das auslösende Element erhält danach seinen vorherigen Zustand
 * zurück — war es schon gesperrt, bleibt es gesperrt.
 * Fehler werden über dieselbe Zuordnung wie im synchronen Fall gemeldet
 * ({@link FxMeldung#zuMeldung}), Erfolgsmeldungen laufen über den
 * Rückruf {@code beiErfolg} — beides wieder auf dem FX-Application-Thread.
 */
@Component
public class HintergrundAufgaben implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HintergrundAufgaben.class);

    /** Wartezeit beim Herunterfahren, bevor laufende Aufgaben abgebrochen werden. */
    private static final int ABSCHALT_FRIST_SEKUNDEN = 5;

    private final ExecutorService ausfuehrer = Executors.newSingleThreadExecutor(auftrag -> {
        Thread faden = new Thread(auftrag, "faktura-hintergrund");
        // Daemon: ein hängender Vorgang darf das Beenden nicht blockieren
        faden.setDaemon(true);
        return faden;
    });

    private final ReadOnlyBooleanWrapper laeuft = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper beschreibung = new ReadOnlyStringWrapper("");

    /** Anzahl gestarteter, noch nicht beendeter Vorgänge; nur auf dem FX-Thread geändert. */
    private int ausstehend;

    /** Szenen mit Wartezeiger; nach dem letzten Vorgang zurückgesetzt, nur auf dem FX-Thread geändert. */
    private final Set<Scene> szenenMitWartezeiger = new HashSet<>();

    /**
     * Führt {@code arbeit} im Hintergrund aus und meldet das Ergebnis auf dem
     * FX-Application-Thread.
     *
     * @param titel     Kurzbeschreibung für die Statuszeile, z. B. „PDF wird exportiert…“
     * @param ausloeser Bedienelement, das währenddessen gesperrt wird; darf {@code null} sein
     * @param arbeit    der eigentliche Vorgang; läuft <em>nicht</em> auf dem FX-Thread
     *                  und darf deshalb keine Bedienelemente anfassen
     * @param beiErfolg Rückruf nach fehlerfreiem Durchlauf, auf dem FX-Thread
     */
    public void starte(String titel, Node ausloeser, Runnable arbeit, Runnable beiErfolg) {
        starte(titel, ausloeser, () -> {
            arbeit.run();
            return null;
        }, ergebnis -> {
            if (beiErfolg != null) {
                beiErfolg.run();
            }
        });
    }

    /**
     * Wie {@link #starte(String, Node, Runnable, Runnable)}, reicht aber das
     * Ergebnis von {@code arbeit} an {@code beiErfolg} weiter — etwa einen im
     * Hintergrund gelesenen Datenbestand, den die Ansicht dann anzeigt.
     */
    public <T> void starte(String titel, Node ausloeser, Supplier<T> arbeit, Consumer<T> beiErfolg) {
        Task<T> aufgabe = new Task<>() {
            @Override
            protected T call() {
                return arbeit.get();
            }
        };
        boolean warGesperrt = ausloeser != null && ausloeser.isDisable();
        aufgabe.setOnSucceeded(ereignis -> {
            beende(ausloeser, warGesperrt);
            if (beiErfolg != null) {
                beiErfolg.accept(aufgabe.getValue());
            }
        });
        aufgabe.setOnFailed(ereignis -> {
            beende(ausloeser, warGesperrt);
            LOG.warn("Hintergrundvorgang fehlgeschlagen: {}", titel);
            FxMeldung.zeige(FxMeldung.zuMeldung(aufgabe.getException()), null);
        });

        beginne(titel, ausloeser);
        ausfuehrer.execute(aufgabe);
    }

    /** Meldet, ob gerade ein Hintergrundvorgang läuft (für die Statuszeile). */
    public ReadOnlyBooleanProperty laeuft() {
        return laeuft.getReadOnlyProperty();
    }

    /** Beschreibung des laufenden Vorgangs; leer, wenn keiner läuft. */
    public ReadOnlyStringProperty beschreibung() {
        return beschreibung.getReadOnlyProperty();
    }

    private void beginne(String titel, Node ausloeser) {
        ausstehend++;
        laeuft.set(true);
        beschreibung.set(titel);
        if (ausloeser != null) {
            ausloeser.setDisable(true);
            Scene szene = ausloeser.getScene();
            if (szene != null) {
                szene.setCursor(Cursor.WAIT);
                szenenMitWartezeiger.add(szene);
            }
        }
    }

    private void beende(Node ausloeser, boolean warGesperrt) {
        ausstehend = Math.max(0, ausstehend - 1);
        if (ausloeser != null) {
            ausloeser.setDisable(warGesperrt);
        }
        if (ausstehend == 0) {
            laeuft.set(false);
            beschreibung.set("");
            // Auch wenn der letzte Vorgang keinen Auslöser hatte (etwa die
            // Aktualisierung der Übersicht nach einem Export)
            szenenMitWartezeiger.forEach(szene -> szene.setCursor(Cursor.DEFAULT));
            szenenMitWartezeiger.clear();
        }
    }

    /**
     * Beendet den Ausführer beim Herunterfahren des Containers; noch laufende
     * Vorgänge bekommen eine kurze Frist.
     */
    @Override
    public void close() {
        ausfuehrer.shutdown();
        try {
            if (!ausfuehrer.awaitTermination(ABSCHALT_FRIST_SEKUNDEN, TimeUnit.SECONDS)) {
                LOG.warn("Hintergrundvorgang lief beim Beenden noch; wird abgebrochen");
                ausfuehrer.shutdownNow();
            }
        } catch (InterruptedException e) {
            ausfuehrer.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** Führt {@code aktion} auf dem FX-Application-Thread aus, egal von wo aufgerufen. */
    static void aufFxThread(Runnable aktion) {
        if (Platform.isFxApplicationThread()) {
            aktion.run();
        } else {
            Platform.runLater(aktion);
        }
    }
}
