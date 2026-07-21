package de.lucasstrubel.faktura.dokumente;

import java.util.HashMap;
import java.util.Map;

/**
 * Belegnummern im Format {@code <PRÄFIX>-<JAHR>-NNNNNN}. Je Belegtyp und
 * Jahr wird ein eigener fortlaufender Zähler auf Basis der höchsten bisher
 * vergebenen Nummer geführt; Rechnungsnummern sind damit lückenlos, da
 * Belege nie gelöscht werden (GR-01, F-12).
 *
 * <p>Der Zähler liegt ausschließlich im Speicher und wird erhöht, bevor der
 * Beleg gespeichert ist — ein fehlgeschlagenes Speichern verbraucht also eine
 * Nummer. Im Datenbankbetrieb übernimmt deshalb der
 * {@link JdbcBelegnummernGenerator}; diese Variante bleibt für Modultests und
 * den JSON-Betrieb erhalten, wo es keine Transaktion gibt.
 *
 * <p>Nicht threadsicher: alle Aufrufe erfolgen auf dem FX-Application-Thread
 * (Einzelplatzbetrieb, vgl. {@code EreignisBus}).
 */
public class EinfacherBelegnummernGenerator implements BelegnummernGenerator {

    private final Map<String, Integer> zaehler = new HashMap<>();

    public EinfacherBelegnummernGenerator() {
    }

    /** Initialisiert die Zähler aus den höchsten bereits vergebenen Nummern im Bestand. */
    public static EinfacherBelegnummernGenerator ausRepository(DokumentRepository repository) {
        EinfacherBelegnummernGenerator generator = new EinfacherBelegnummernGenerator();
        for (Dokument dokument : repository.alle()) {
            Belegnummernformat.zerlege(dokument.getBelegnummer()).ifPresent(teile ->
                    generator.zaehler.merge(teile.kreis(), teile.laufendeNummer() + 1, Math::max));
        }
        return generator;
    }

    /** Setzt den Zähler explizit, z. B. {@code setzeZaehler(RECHNUNG, 2026, 7)} → {@code R-2026-000007}. */
    public void setzeZaehler(Belegtyp typ, int jahr, int wert) {
        zaehler.put(typ.praefix() + "-" + jahr, wert);
    }

    @Override
    public String naechsteNummer(Belegtyp typ, int jahr) {
        String schluessel = typ.praefix() + "-" + jahr;
        int naechste = zaehler.getOrDefault(schluessel, 1);
        zaehler.put(schluessel, naechste + 1);
        return Belegnummernformat.formatiere(typ, jahr, naechste);
    }
}
