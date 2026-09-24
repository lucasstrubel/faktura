package de.lucasstrubel.faktura.gui;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ansichtsbezogene Tastenkürzel (D-F-22): Strg+N, Strg+F und F5 gelten nur,
 * solange die Ansicht angezeigt wird.
 *
 * <p>Die Ansichten werden einmal geladen und dann im Inhaltsbereich der einen
 * Hauptszene ausgetauscht. Früher trug jede Ansicht ihre Kürzel beim
 * Einhängen in die Szene ein, entfernte sie beim Aushängen aber nie — wer nach
 * den Kunden die Einstellungen öffnete, bekam mit Strg+N weiterhin den
 * Dialog „Neuer Kunde“. Jetzt werden die Kürzel beim Aushängen wieder
 * ausgetragen; entfernt wird dabei nur, was noch von dieser Ansicht stammt,
 * damit die Reihenfolge von Ein- und Aushängen keine Rolle spielt.
 */
public final class Tastenkuerzel {

    private Tastenkuerzel() {
    }

    /**
     * Bindet die Standardkürzel an die Lebensdauer von {@code anker} in der Szene.
     *
     * @param anker        ein Knoten der Ansicht, z. B. ihre Tabelle
     * @param neu          Aktion für Strg+N
     * @param suche        Aktion für Strg+F
     * @param aktualisiere Aktion für F5
     */
    public static void binde(Node anker, Runnable neu, Runnable suche, Runnable aktualisiere) {
        Map<KeyCombination, Runnable> kuerzel = new LinkedHashMap<>();
        kuerzel.put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), neu);
        kuerzel.put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), suche);
        kuerzel.put(new KeyCodeCombination(KeyCode.F5), aktualisiere);

        anker.sceneProperty().addListener((beobachtbar, alteSzene, neueSzene) -> {
            if (alteSzene != null) {
                trageAus(alteSzene, kuerzel);
            }
            if (neueSzene != null) {
                neueSzene.getAccelerators().putAll(kuerzel);
            }
        });
        if (anker.getScene() != null) {
            anker.getScene().getAccelerators().putAll(kuerzel);
        }
    }

    private static void trageAus(Scene szene, Map<KeyCombination, Runnable> kuerzel) {
        kuerzel.forEach((kombination, aktion) -> szene.getAccelerators().remove(kombination, aktion));
    }
}
