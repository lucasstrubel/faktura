package de.lucasstrubel.faktura.gui;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Gemeinsamer Schutz für ungespeicherte Änderungen beim Beenden (Q-05):
 * Ansichten melden eine Prüfung an, {@code FxAnwendung} fragt sie beim
 * Schließen des Hauptfensters ab. Jede Prüfung darf eine Rückfrage zeigen;
 * {@code false} bricht das Beenden ab.
 */
@Component
public class SchliessSchutz {
    private final List<BooleanSupplier> pruefungen = new ArrayList<>();

    public void registriere(BooleanSupplier pruefung) { pruefungen.add(pruefung); }

    public boolean darfSchliessen() {
        return pruefungen.stream().allMatch(BooleanSupplier::getAsBoolean);
    }
}
