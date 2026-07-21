package de.lucasstrubel.faktura.dokumente;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aufbau der Belegnummern {@code <PRÄFIX>-<JAHR>-NNNNNN} (Komponente A,
 * Kapitel 4). Beide Nummerngeneratoren — der speicherbasierte
 * {@link EinfacherBelegnummernGenerator} und der transaktionssichere
 * {@link JdbcBelegnummernGenerator} — greifen hierauf zurück, damit das Format
 * nur an einer Stelle festgelegt ist.
 */
final class Belegnummernformat {

    private static final Pattern FORMAT = Pattern.compile("(AN|AB|LS|R)-(\\d{4})-(\\d{6})");

    private Belegnummernformat() {
    }

    /** Zerlegte Belegnummer: Belegtyp, Jahr und laufende Nummer. */
    record Bestandteile(Belegtyp typ, int jahr, int laufendeNummer) {

        /** Schlüssel des zugehörigen Nummernkreises, z. B. {@code R-2026}. */
        String kreis() {
            return typ.praefix() + "-" + jahr;
        }
    }

    /** Setzt die Belegnummer zusammen, z. B. {@code R-2026-000124}. */
    static String formatiere(Belegtyp typ, int jahr, int laufendeNummer) {
        return String.format("%s-%04d-%06d", typ.praefix(), jahr, laufendeNummer);
    }

    /**
     * Zerlegt eine Belegnummer; leer, wenn sie dem Format nicht entspricht
     * (Altdaten, Fremdformate).
     */
    static Optional<Bestandteile> zerlege(String belegnummer) {
        if (belegnummer == null) {
            return Optional.empty();
        }
        Matcher treffer = FORMAT.matcher(belegnummer);
        if (!treffer.matches()) {
            return Optional.empty();
        }
        Belegtyp typ = null;
        for (Belegtyp kandidat : Belegtyp.values()) {
            if (kandidat.praefix().equals(treffer.group(1))) {
                typ = kandidat;
                break;
            }
        }
        if (typ == null) {
            return Optional.empty();
        }
        return Optional.of(new Bestandteile(typ, Integer.parseInt(treffer.group(2)),
                Integer.parseInt(treffer.group(3))));
    }

    /**
     * Liefert die laufende Nummer, falls die Belegnummer dem Format entspricht
     * und zu Belegtyp und Jahr gehört; sonst {@link OptionalInt#empty()}.
     */
    static OptionalInt laufendeNummer(String belegnummer, Belegtyp typ, int jahr) {
        return zerlege(belegnummer)
                .filter(teile -> teile.typ() == typ && teile.jahr() == jahr)
                .map(teile -> OptionalInt.of(teile.laufendeNummer()))
                .orElseGet(OptionalInt::empty);
    }

    /** Höchste im Bestand vergebene laufende Nummer je Belegtyp und Jahr; 0, wenn keine. */
    static int hoechsteImBestand(DokumentRepository repository, Belegtyp typ, int jahr) {
        int hoechste = 0;
        for (Dokument dokument : repository.alle()) {
            OptionalInt nummer = laufendeNummer(dokument.getBelegnummer(), typ, jahr);
            if (nummer.isPresent() && nummer.getAsInt() > hoechste) {
                hoechste = nummer.getAsInt();
            }
        }
        return hoechste;
    }
}
