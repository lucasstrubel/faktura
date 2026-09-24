package de.lucasstrubel.faktura.kunden;

import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.DatenGeaendertEreignis;
import de.lucasstrubel.faktura.gemeinsam.LoeschAbgelehntException;
import de.lucasstrubel.faktura.gemeinsam.Validierung;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fachlogik der Kundenverwaltung (Pflichtenheft Teil C):
 * Validierung (F-03, F-04), Nummernvergabe (F-02), Löschsperre GR-04
 * (F-08–F-10) sowie lesender Zugriff für Komponente A (F-14).
 */
@Service
public class KundenVerwaltungsService implements KundenService {

    private final KundenRepository repository;
    private final KundennummernGenerator nummernGenerator;
    private final KundenReferenzPruefung referenzPruefung;
    private final ApplicationEventPublisher ereignisse;

    public KundenVerwaltungsService(KundenRepository repository,
                                    KundennummernGenerator nummernGenerator,
                                    KundenReferenzPruefung referenzPruefung,
                                    ApplicationEventPublisher ereignisse) {
        this.repository = repository;
        this.nummernGenerator = nummernGenerator;
        this.referenzPruefung = referenzPruefung;
        this.ereignisse = ereignisse;
    }

    /**
     * Legt einen neuen Kunden an und vergibt die Kundennummer (F-01, F-02).
     * Nummernvergabe und Speichern laufen in einer Transaktion — schlägt das
     * Speichern fehl, ist die Nummer nicht verbraucht.
     */
    @Transactional
    public Kunde legeAn(Kunde kunde) {
        bereinige(kunde);
        validiere(kunde);
        kunde.setKundennummer(nummernGenerator.naechsteNummer());
        if (repository.findeNachNummer(kunde.getKundennummer()) != null) {
            // Ein Nummernkreis, der eine vergebene Nummer liefert, darf nie
            // stillschweigend einen Bestandskunden überschreiben (C-F-02)
            throw new IllegalStateException("Die Kundennummer " + kunde.getKundennummer()
                    + " ist bereits vergeben; der Nummernkreis ist inkonsistent.");
        }
        Kunde gespeichert = repository.speichere(kunde);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.KUNDEN));
        return gespeichert;
    }

    /** Ändert einen bestehenden Kunden; die Pflichtfeldprüfung gilt unverändert (F-05). */
    @Transactional
    public Kunde aendere(Kunde kunde) {
        if (kunde.getKundennummer() == null) {
            throw new ValidierungsException("Kundennummer", "Der Kunde wurde noch nicht angelegt.");
        }
        if (repository.findeNachNummer(kunde.getKundennummer()) == null) {
            throw new ValidierungsException("Kundennummer",
                    "Der Kunde " + kunde.getKundennummer() + " existiert nicht.");
        }
        bereinige(kunde);
        validiere(kunde);
        Kunde gespeichert = repository.speichere(kunde);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.KUNDEN));
        return gespeichert;
    }

    /**
     * Löscht einen Kunden ohne verknüpfte Dokumente (F-08); bei verknüpften
     * Dokumenten wird der Vorgang mit Angabe der Anzahl abgelehnt (F-09, GR-04).
     */
    @Transactional
    public void loescheKunde(String kundennummer) {
        int anzahl = referenzPruefung.anzahlVerknuepfterDokumente(kundennummer);
        if (anzahl > 0) {
            throw new LoeschAbgelehntException(
                    "Der Kunde " + kundennummer + " kann nicht gelöscht werden: "
                            + anzahl + " verknüpfte Dokumente vorhanden (GR-04).");
        }
        repository.loesche(kundennummer);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.KUNDEN));
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
     * Speichert Eingaben in einheitlicher Form (C-F-19): ohne führende oder
     * folgende Leerzeichen, leere optionale Felder als {@code null}, die
     * USt-IdNr. kompakt in Großschrift — so, wie sie auch auf Belegen und in
     * der E-Rechnung erscheinen soll.
     */
    private static void bereinige(Kunde kunde) {
        kunde.setName(Validierung.bereinige(kunde.getName()));
        kunde.setStrasse(Validierung.bereinige(kunde.getStrasse()));
        kunde.setPlz(Validierung.bereinige(kunde.getPlz()));
        kunde.setOrt(Validierung.bereinige(kunde.getOrt()));
        kunde.setEMail(Validierung.bereinige(kunde.getEMail()));
        kunde.setTelefon(Validierung.bereinige(kunde.getTelefon()));
        kunde.setUstIdNr(Validierung.normalisiereUstIdNr(kunde.getUstIdNr()));
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
