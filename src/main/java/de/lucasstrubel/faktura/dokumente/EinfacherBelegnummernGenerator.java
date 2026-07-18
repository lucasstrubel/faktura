package de.lucasstrubel.faktura.dokumente;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Belegnummern im Format {@code <PRÄFIX>-<JAHR>-NNNNNN}. Je Belegtyp und
 * Jahr wird ein eigener fortlaufender Zähler auf Basis der höchsten bisher
 * vergebenen Nummer geführt; Rechnungsnummern sind damit lückenlos, da
 * Belege nie gelöscht werden (GR-01, F-12).
 */
public class EinfacherBelegnummernGenerator implements BelegnummernGenerator {

    private static final Pattern FORMAT = Pattern.compile("(AN|AB|LS|R)-(\\d{4})-(\\d{6})");

    private final Map<String, Integer> zaehler = new HashMap<>();

    public EinfacherBelegnummernGenerator() {
    }

    /** Initialisiert die Zähler aus den höchsten bereits vergebenen Nummern im Bestand. */
    public static EinfacherBelegnummernGenerator ausRepository(DokumentRepository repository) {
        EinfacherBelegnummernGenerator generator = new EinfacherBelegnummernGenerator();
        for (Dokument dokument : repository.alle()) {
            Matcher matcher = FORMAT.matcher(dokument.getBelegnummer());
            if (matcher.matches()) {
                String schluessel = matcher.group(1) + "-" + matcher.group(2);
                int wert = Integer.parseInt(matcher.group(3)) + 1;
                generator.zaehler.merge(schluessel, wert, Math::max);
            }
        }
        return generator;
    }

    /** Setzt den Zähler explizit, z. B. {@code setzeZaehler(RECHNUNG, 2026, 7)} → {@code R-2026-000007}. */
    public void setzeZaehler(Belegtyp typ, int jahr, int wert) {
        zaehler.put(typ.praefix() + "-" + jahr, wert);
    }

    @Override
    public synchronized String naechsteNummer(Belegtyp typ, int jahr) {
        String schluessel = typ.praefix() + "-" + jahr;
        int naechste = zaehler.getOrDefault(schluessel, 1);
        zaehler.put(schluessel, naechste + 1);
        return String.format("%s-%04d-%06d", typ.praefix(), jahr, naechste);
    }
}
