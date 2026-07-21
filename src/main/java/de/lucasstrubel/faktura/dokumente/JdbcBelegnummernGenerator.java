package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.gemeinsam.JdbcNummernkreis;

import org.springframework.stereotype.Component;

/**
 * Transaktionssichere Belegnummern (GR-01, F-12): Der Zähler je Belegtyp und
 * Jahr liegt in der Tabelle {@code nummernkreis} und wird in derselben
 * Transaktion fortgeschrieben, in der der Beleg gespeichert wird. Schlägt das
 * Speichern fehl, ist die Nummer nicht verbraucht — die Rechnungsnummernfolge
 * bleibt lückenlos.
 *
 * <p>Beim ersten Zugriff auf einen noch unbekannten Kreis wird der Startwert
 * aus dem vorhandenen Belegbestand abgeleitet; damit übernehmen bestehende
 * Installationen ohne gesonderte Datenmigration.
 *
 * @see EinfacherBelegnummernGenerator speicherbasierte Variante für Tests und
 *      den JSON-Betrieb
 */
@Component
public class JdbcBelegnummernGenerator implements BelegnummernGenerator {

    private final JdbcNummernkreis nummernkreis;
    private final DokumentRepository repository;

    public JdbcBelegnummernGenerator(JdbcNummernkreis nummernkreis, DokumentRepository repository) {
        this.nummernkreis = nummernkreis;
        this.repository = repository;
    }

    @Override
    public String naechsteNummer(Belegtyp typ, int jahr) {
        int wert = nummernkreis.naechsterWert(typ.praefix(), jahr,
                () -> Belegnummernformat.hoechsteImBestand(repository, typ, jahr) + 1);
        return Belegnummernformat.formatiere(typ, jahr, wert);
    }
}
