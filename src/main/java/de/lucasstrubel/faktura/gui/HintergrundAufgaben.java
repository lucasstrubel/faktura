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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        Task<Void> aufgabe = new Task<>() {
            @Override
            protected Void call() {
                arbeit.run();
                return null;
            }
        };
        aufgabe.setOnSucceeded(ereignis -> {
            beende(ausloeser);
            if (beiErfolg != null) {
                beiErfolg.run();
            }
        });
        aufgabe.setOnFailed(ereignis -> {
            beende(ausloeser);
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
        laeuft.set(true);
        beschreibung.set(titel);
        if (ausloeser != null) {
            ausloeser.setDisable(true);
            setzeMauszeiger(ausloeser, Cursor.WAIT);
        }
    }

    private void beende(Node ausloeser) {
        laeuft.set(false);
        beschreibung.set("");
        if (ausloeser != null) {
            ausloeser.setDisable(false);
            setzeMauszeiger(ausloeser, Cursor.DEFAULT);
        }
    }

    private static void setzeMauszeiger(Node bezug, Cursor zeiger) {
        Scene szene = bezug.getScene();
        if (szene != null) {
            szene.setCursor(zeiger);
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
