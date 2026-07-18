package de.lucasstrubel.faktura.produkte;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fortlaufende Produktnummern im Format {@code P-NNNNNN} (Präfix, führende
 * Nullen) auf Basis der höchsten bisher vergebenen Nummer (B, Kapitel 4).
 *
 * <p>Nicht threadsicher: alle Aufrufe erfolgen auf dem Event-Dispatch-Thread
 * (Einzelplatzbetrieb, vgl. {@code EreignisBus}).
 */
public class EinfacherProduktnummernGenerator implements ProduktnummernGenerator {

    private static final Pattern FORMAT = Pattern.compile("P-(\\d{6})");

    private int zaehler;

    /** @param zaehler Wert der nächsten zu vergebenden Nummer (TC-02: Zähler 7 → {@code P-000007}). */
    public EinfacherProduktnummernGenerator(int zaehler) {
        this.zaehler = zaehler;
    }

    /** Initialisiert den Zähler aus der höchsten bereits vergebenen Nummer im Bestand. */
    public static EinfacherProduktnummernGenerator ausRepository(ProduktRepository repository) {
        int hoechste = repository.alleSortiertNachBezeichnung().stream()
                .map(Produkt::getProduktnummer)
                .mapToInt(EinfacherProduktnummernGenerator::nummernWert)
                .max()
                .orElse(0);
        return new EinfacherProduktnummernGenerator(hoechste + 1);
    }

    private static int nummernWert(String produktnummer) {
        if (produktnummer == null) {
            return 0;
        }
        Matcher matcher = FORMAT.matcher(produktnummer);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    @Override
    public String naechsteNummer() {
        return String.format("P-%06d", zaehler++);
    }
}
