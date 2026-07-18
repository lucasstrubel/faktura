package de.lucasstrubel.faktura.produkte;

import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.gemeinsam.LoeschAbgelehntException;
import de.lucasstrubel.faktura.gemeinsam.Validierung;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fachlogik der Produktverwaltung (Pflichtenheft Teil B):
 * Validierung (F-03, F-04), Nummernvergabe (F-02), Löschsperre
 * (F-08–F-10) sowie lesender Zugriff für Komponente A (F-14).
 */
public class ProduktVerwaltungsService implements ProduktService {

    /** Zulässige Steuersätze als Faktor (B-F-03). */
    private static final List<BigDecimal> ZULAESSIGE_STEUERSAETZE = List.of(
            new BigDecimal("0.00"), new BigDecimal("0.07"), new BigDecimal("0.19"));

    private final ProduktRepository repository;
    private final ProduktnummernGenerator nummernGenerator;
    private final ProduktReferenzPruefung referenzPruefung;
    private final EreignisBus ereignisBus;

    public ProduktVerwaltungsService(ProduktRepository repository,
                                     ProduktnummernGenerator nummernGenerator,
                                     ProduktReferenzPruefung referenzPruefung) {
        this(repository, nummernGenerator, referenzPruefung, new EreignisBus());
    }

    public ProduktVerwaltungsService(ProduktRepository repository,
                                     ProduktnummernGenerator nummernGenerator,
                                     ProduktReferenzPruefung referenzPruefung,
                                     EreignisBus ereignisBus) {
        this.repository = repository;
        this.nummernGenerator = nummernGenerator;
        this.referenzPruefung = referenzPruefung;
        this.ereignisBus = ereignisBus;
    }

    /** Legt ein neues Produkt an und vergibt die Produktnummer (F-01, F-02). */
    public Produkt legeAn(Produkt produkt) {
        validiere(produkt);
        produkt.setProduktnummer(nummernGenerator.naechsteNummer());
        Produkt gespeichert = repository.speichere(produkt);
        ereignisBus.melde(DatenBereich.PRODUKTE);
        return gespeichert;
    }

    /**
     * Ändert ein bestehendes Produkt (F-05). Bereits erstellte Dokumente bleiben
     * unverändert, da Komponente A Preis und Steuersatz als Snapshot ablegt (F-06).
     */
    public Produkt aendere(Produkt produkt) {
        if (produkt.getProduktnummer() == null) {
            throw new ValidierungsException("Produktnummer", "Das Produkt wurde noch nicht angelegt.");
        }
        validiere(produkt);
        Produkt gespeichert = repository.speichere(produkt);
        ereignisBus.melde(DatenBereich.PRODUKTE);
        return gespeichert;
    }

    /**
     * Löscht ein nicht referenziertes Produkt (F-08); referenzierte Produkte
     * werden mit Hinweis abgelehnt (F-09, F-10).
     */
    public void loescheProdukt(String produktnummer) {
        if (referenzPruefung.istProduktReferenziert(produktnummer)) {
            throw new LoeschAbgelehntException(
                    "Das Produkt " + produktnummer + " kann nicht gelöscht werden: "
                            + "es wird in Dokumenten verwendet.");
        }
        repository.loesche(produktnummer);
        ereignisBus.melde(DatenBereich.PRODUKTE);
    }

    public List<Produkt> alleSortiertNachBezeichnung() {
        return repository.alleSortiertNachBezeichnung();
    }

    @Override
    public List<Produkt> suche(String suchbegriff) {
        return repository.suche(suchbegriff);
    }

    @Override
    public Produkt findeProdukt(String produktnummer) {
        return repository.findeNachNummer(produktnummer);
    }

    /** Pflichtfeld- und Wertebereichsprüfung (F-03, F-04); benennt das betroffene Feld (Q-09). */
    private void validiere(Produkt produkt) {
        Validierung.pruefePflichtfeld(produkt.getBezeichnung(), "Bezeichnung");
        BigDecimal preis = produkt.getEinzelpreisNetto();
        if (preis == null) {
            throw new ValidierungsException("Einzelpreis",
                    "Das Pflichtfeld 'Einzelpreis (netto)' fehlt.");
        }
        if (preis.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidierungsException("Einzelpreis",
                    "Der 'Einzelpreis (netto)' muss größer oder gleich 0,00 sein.");
        }
        BigDecimal steuersatz = produkt.getSteuersatz();
        if (steuersatz == null) {
            throw new ValidierungsException("Steuersatz",
                    "Das Pflichtfeld 'Steuersatz' fehlt.");
        }
        boolean zulaessig = ZULAESSIGE_STEUERSAETZE.stream()
                .anyMatch(s -> s.compareTo(steuersatz) == 0);
        if (!zulaessig) {
            throw new ValidierungsException("Steuersatz",
                    "Unzulässiger 'Steuersatz' " + steuersatz + "; zulässig sind 0.00, 0.07 und 0.19.");
        }
    }
}
