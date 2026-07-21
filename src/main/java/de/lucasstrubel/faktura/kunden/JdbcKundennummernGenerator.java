package de.lucasstrubel.faktura.kunden;

import de.lucasstrubel.faktura.gemeinsam.JdbcNummernkreis;

import org.springframework.stereotype.Component;

/**
 * Transaktionssichere Kundennummern {@code K-NNNNNN} (C, Kapitel 4): Der
 * Zähler liegt in der Tabelle {@code nummernkreis} und wird in derselben
 * Transaktion fortgeschrieben, in der der Kunde gespeichert wird. Ein
 * fehlgeschlagenes Speichern verbraucht damit keine Nummer.
 *
 * @see EinfacherKundennummernGenerator speicherbasierte Variante für Tests und
 *      den JSON-Betrieb
 */
@Component
public class JdbcKundennummernGenerator implements KundennummernGenerator {

    /** Nummernpräfix und zugleich Bereichsschlüssel des Nummernkreises. */
    private static final String BEREICH = "K";

    private final JdbcNummernkreis nummernkreis;
    private final KundenRepository repository;

    public JdbcKundennummernGenerator(JdbcNummernkreis nummernkreis, KundenRepository repository) {
        this.nummernkreis = nummernkreis;
        this.repository = repository;
    }

    @Override
    public String naechsteNummer() {
        int wert = nummernkreis.naechsterWert(BEREICH, JdbcNummernkreis.OHNE_JAHR,
                () -> EinfacherKundennummernGenerator.hoechsteImBestand(repository) + 1);
        return String.format("%s-%06d", BEREICH, wert);
    }
}
