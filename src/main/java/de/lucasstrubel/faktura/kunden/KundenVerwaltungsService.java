package de.lucasstrubel.faktura.kunden;

import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.gemeinsam.LoeschAbgelehntException;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import java.util.List;

/**
 * Fachlogik der Kundenverwaltung (Pflichtenheft Gruppe C):
 * Validierung (F-03, F-04), Nummernvergabe (F-02), Löschsperre GR-04
 * (F-08–F-10) sowie lesender Zugriff für Gruppe A (F-14).
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

    /** Pflichtfeld- und Formatprüfung (F-03, F-04); benennt das betroffene Feld (Q-09). */
    private void validiere(Kunde kunde) {
        pruefePflichtfeld(kunde.getName(), "Name");
        pruefePflichtfeld(kunde.getStrasse(), "Straße");
        pruefePflichtfeld(kunde.getPlz(), "PLZ");
        pruefePflichtfeld(kunde.getOrt(), "Ort");
        String eMail = kunde.getEMail();
        if (eMail != null && !eMail.isBlank() && !eMail.matches(".+@.+")) {
            throw new ValidierungsException("E-Mail",
                    "Das Feld 'E-Mail' hat ein ungültiges Format: " + eMail);
        }
    }

    private void pruefePflichtfeld(String wert, String feldname) {
        if (wert == null || wert.isBlank()) {
            throw new ValidierungsException(feldname,
                    "Das Pflichtfeld '" + feldname + "' fehlt.");
        }
    }
}
