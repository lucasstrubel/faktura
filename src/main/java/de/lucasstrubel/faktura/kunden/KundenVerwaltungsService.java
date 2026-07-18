package de.lucasstrubel.faktura.kunden;

import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.gemeinsam.LoeschAbgelehntException;
import de.lucasstrubel.faktura.gemeinsam.Validierung;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import java.util.List;

/**
 * Fachlogik der Kundenverwaltung (Pflichtenheft Teil C):
 * Validierung (F-03, F-04), Nummernvergabe (F-02), Löschsperre GR-04
 * (F-08–F-10) sowie lesender Zugriff für Komponente A (F-14).
 */
public class KundenVerwaltungsService implements KundenService {

    private final KundenRepository repository;
    private final KundennummernGenerator nummernGenerator;
    private final KundenReferenzPruefung referenzPruefung;
    private final EreignisBus ereignisBus;

    public KundenVerwaltungsService(KundenRepository repository,
                                    KundennummernGenerator nummernGenerator,
                                    KundenReferenzPruefung referenzPruefung) {
        this(repository, nummernGenerator, referenzPruefung, new EreignisBus());
    }

    public KundenVerwaltungsService(KundenRepository repository,
                                    KundennummernGenerator nummernGenerator,
                                    KundenReferenzPruefung referenzPruefung,
                                    EreignisBus ereignisBus) {
        this.repository = repository;
        this.nummernGenerator = nummernGenerator;
        this.referenzPruefung = referenzPruefung;
        this.ereignisBus = ereignisBus;
    }

    /** Legt einen neuen Kunden an und vergibt die Kundennummer (F-01, F-02). */
    public Kunde legeAn(Kunde kunde) {
        validiere(kunde);
        kunde.setKundennummer(nummernGenerator.naechsteNummer());
        Kunde gespeichert = repository.speichere(kunde);
        ereignisBus.melde(DatenBereich.KUNDEN);
        return gespeichert;
    }

    /** Ändert einen bestehenden Kunden; die Pflichtfeldprüfung gilt unverändert (F-05). */
    public Kunde aendere(Kunde kunde) {
        if (kunde.getKundennummer() == null) {
            throw new ValidierungsException("Kundennummer", "Der Kunde wurde noch nicht angelegt.");
        }
        validiere(kunde);
        Kunde gespeichert = repository.speichere(kunde);
        ereignisBus.melde(DatenBereich.KUNDEN);
        return gespeichert;
    }

    /**
     * Löscht einen Kunden ohne verknüpfte Dokumente (F-08); bei verknüpften
     * Dokumenten wird der Vorgang mit Angabe der Anzahl abgelehnt (F-09, GR-04).
     */
    public void loescheKunde(String kundennummer) {
        int anzahl = referenzPruefung.anzahlVerknuepfterDokumente(kundennummer);
        if (anzahl > 0) {
            throw new LoeschAbgelehntException(
                    "Der Kunde " + kundennummer + " kann nicht gelöscht werden: "
                            + anzahl + " verknüpfte Dokumente vorhanden (GR-04).");
        }
        repository.loesche(kundennummer);
        ereignisBus.melde(DatenBereich.KUNDEN);
    }

    public List<Kunde> alleSortiertNachName() {
        return repository.alleSortiertNachName();
    }

    @Override
    public List<Kunde> suche(String suchbegriff) {
        return repository.suche(suchbegriff);
    }

    @Override
    public Kunde findeKunde(String kundennummer) {
        return repository.findeNachNummer(kundennummer);
    }

    /**
     * Pflichtfeld- und Formatprüfung (F-03, F-04 sowie F-16–F-18); benennt das
     * betroffene Feld (Q-09). Die Formatregeln sind zentral in
     * {@link Validierung} definiert.
     */
    private void validiere(Kunde kunde) {
        Validierung.pruefePflichtfeld(kunde.getName(), "Name");
        Validierung.pruefePflichtfeld(kunde.getStrasse(), "Straße");
        Validierung.pruefePflichtfeld(kunde.getPlz(), "PLZ");
        Validierung.pruefePflichtfeld(kunde.getOrt(), "Ort");
        Validierung.pruefePlz(kunde.getPlz());
        Validierung.pruefeEMail(kunde.getEMail());
        Validierung.pruefeTelefon(kunde.getTelefon());
        Validierung.pruefeUstIdNr(kunde.getUstIdNr());
    }
}
