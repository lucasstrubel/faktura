package de.lucasstrubel.faktura.kunden;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fortlaufende Kundennummern im Format {@code K-NNNNNN} (Präfix, führende
 * Nullen) auf Basis der höchsten bisher vergebenen Nummer (C, Kapitel 4).
 *
 * <p>Nicht threadsicher: alle Aufrufe erfolgen auf dem Event-Dispatch-Thread
 * (Einzelplatzbetrieb, vgl. {@code EreignisBus}).
 */
public class EinfacherKundennummernGenerator implements KundennummernGenerator {

    private static final Pattern FORMAT = Pattern.compile("K-(\\d{6})");

    private int zaehler;

    /** @param zaehler Wert der nächsten zu vergebenden Nummer (TC-02: Zähler 7 → {@code K-000007}). */
    public EinfacherKundennummernGenerator(int zaehler) {
        this.zaehler = zaehler;
    }

    /** Initialisiert den Zähler aus der höchsten bereits vergebenen Nummer im Bestand. */
    public static EinfacherKundennummernGenerator ausRepository(KundenRepository repository) {
        int hoechste = repository.alleSortiertNachName().stream()
                .map(Kunde::getKundennummer)
                .mapToInt(EinfacherKundennummernGenerator::nummernWert)
                .max()
                .orElse(0);
        return new EinfacherKundennummernGenerator(hoechste + 1);
    }

    private static int nummernWert(String kundennummer) {
        if (kundennummer == null) {
            return 0;
        }
        Matcher matcher = FORMAT.matcher(kundennummer);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    @Override
    public String naechsteNummer() {
        return String.format("K-%06d", zaehler++);
    }
}
