package de.lucasstrubel.faktura.produkte;

import de.lucasstrubel.faktura.gemeinsam.JdbcNummernkreis;

import org.springframework.stereotype.Component;

/**
 * Transaktionssichere Produktnummern {@code P-NNNNNN} (B, Kapitel 4): Der
 * Zähler liegt in der Tabelle {@code nummernkreis} und wird in derselben
 * Transaktion fortgeschrieben, in der das Produkt gespeichert wird. Ein
 * fehlgeschlagenes Speichern verbraucht damit keine Nummer.
 *
 * @see EinfacherProduktnummernGenerator speicherbasierte Variante für Tests
 *      und den JSON-Betrieb
 */
@Component
public class JdbcProduktnummernGenerator implements ProduktnummernGenerator {

    /** Nummernpräfix und zugleich Bereichsschlüssel des Nummernkreises. */
    private static final String BEREICH = "P";

    private final JdbcNummernkreis nummernkreis;
    private final ProduktRepository repository;

    public JdbcProduktnummernGenerator(JdbcNummernkreis nummernkreis, ProduktRepository repository) {
        this.nummernkreis = nummernkreis;
        this.repository = repository;
    }

    @Override
    public String naechsteNummer() {
        int wert = nummernkreis.naechsterWert(BEREICH, JdbcNummernkreis.OHNE_JAHR,
                () -> EinfacherProduktnummernGenerator.hoechsteImBestand(repository) + 1);
        return String.format("%s-%06d", BEREICH, wert);
    }
}
