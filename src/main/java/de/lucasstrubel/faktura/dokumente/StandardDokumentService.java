package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.firma.Firmenprofil;
import de.lucasstrubel.faktura.firma.FirmenprofilService;
import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.DatenGeaendertEreignis;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Standardimplementierung des {@link DokumentService} (Komponente A, Kapitel 7):
 * orchestriert {@link BelegnummernGenerator}, {@link KundenService},
 * {@link ProduktService}, {@link FirmenprofilService},
 * {@link DokumentRepository} und {@link PdfExporter}.
 */
@Service
public class StandardDokumentService implements DokumentService {

    /** Standard-Zahlungsziel in Kalendertagen ab Rechnungsdatum (GR-06, F-14). */
    public static final int STANDARD_ZAHLUNGSZIEL_TAGE = 14;

    /** Standard-Gültigkeit eines Angebots in Kalendertagen ab Erstelldatum (F-02). */
    public static final int STANDARD_GUELTIGKEIT_TAGE = 30;

    /** Protokollierter Benutzer einer Stornierung (BA-14); Einzelplatzbetrieb. */
    public static final String SYSTEM_BENUTZER = "Anwender";

    private final DokumentRepository repository;
    private final BelegnummernGenerator nummernGenerator;
    private final KundenService kundenService;
    private final ProduktService produktService;
    private final FirmenprofilService firmenprofilService;
    private final PdfExporter pdfExporter;
    private final ApplicationEventPublisher ereignisse;

    public StandardDokumentService(DokumentRepository repository,
                                   BelegnummernGenerator nummernGenerator,
                                   KundenService kundenService,
                                   ProduktService produktService,
                                   FirmenprofilService firmenprofilService,
                                   PdfExporter pdfExporter,
                                   ApplicationEventPublisher ereignisse) {
        this.repository = repository;
        this.nummernGenerator = nummernGenerator;
        this.kundenService = kundenService;
        this.produktService = produktService;
        this.firmenprofilService = firmenprofilService;
        this.pdfExporter = pdfExporter;
        this.ereignisse = ereignisse;
    }

    @Override
    @Transactional
    public Angebot erstelleAngebot(String kundenNr, List<Positionsangabe> positionen, LocalDate gueltigBis) {
        LocalDate datum = LocalDate.now();
        if (gueltigBis != null && gueltigBis.isBefore(datum)) {
            throw new ValidierungsException("Gültig bis",
                    "'Gültig bis' darf nicht vor dem Angebotsdatum liegen (A-F-32).");
        }
        Angebot angebot = new Angebot();
        return erstelleBeleg(angebot, kundenNr, positionen, datum, () ->
                angebot.setGueltigBis(gueltigBis != null
                        ? gueltigBis
                        : datum.plusDays(STANDARD_GUELTIGKEIT_TAGE)));
    }

    @Override
    @Transactional
    public Auftragsbestaetigung erstelleAuftragsbestaetigung(String kundenNr, List<Positionsangabe> positionen) {
        return erstelleBeleg(new Auftragsbestaetigung(), kundenNr, positionen,
                LocalDate.now(), () -> { });
    }

    @Override
    @Transactional
    public Lieferschein erstelleLieferschein(String kundenNr, List<Positionsangabe> positionen, LocalDate lieferdatum) {
        LocalDate datum = LocalDate.now();
        Lieferschein lieferschein = new Lieferschein();
        return erstelleBeleg(lieferschein, kundenNr, positionen, datum, () ->
                lieferschein.setLieferdatum(lieferdatum != null ? lieferdatum : datum));
    }

    @Override
    @Transactional
    public Rechnung erstelleRechnung(String kundenNr, List<Positionsangabe> positionen,
                                     LocalDate rechnungsdatum, LocalDate leistungsdatum,
                                     LocalDate zahlungsziel) {
        if (rechnungsdatum == null) {
            throw new ValidierungsException("Rechnungsdatum",
                    "Das Pflichtfeld 'Rechnungsdatum' fehlt (F-18).");
        }
        if (zahlungsziel != null && zahlungsziel.isBefore(rechnungsdatum)) {
            throw new ValidierungsException("Zahlungsziel",
                    "Das 'Zahlungsziel' darf nicht vor dem Rechnungsdatum liegen (A-F-32).");
        }
        Rechnung rechnung = new Rechnung();
        return erstelleBeleg(rechnung, kundenNr, positionen, rechnungsdatum, () -> {
            rechnung.setLeistungsdatum(leistungsdatum != null ? leistungsdatum : rechnungsdatum);
            rechnung.setZahlungsziel(zahlungsziel != null
                    ? zahlungsziel
                    : rechnungsdatum.plusDays(STANDARD_ZAHLUNGSZIEL_TAGE));
            rechnung.setzeStatus(DokumentStatus.OFFEN);
        });
    }

    /**
     * Überschreibt die Default-Methode der Schnittstelle nur, um sie
     * transaktional zu machen: Ein Default-Methoden-Aufruf liefe am
     * Transaktions-Proxy vorbei — die Nummernvergabe liefe dann in einer
     * eigenen Transaktion, und ein gescheitertes Speichern hinterließe eine
     * Lücke (GR-01, NK-01).
     */
    @Override
    @Transactional
    public Rechnung erstelleRechnung(String kundenNr, List<Positionsangabe> positionen,
                                     LocalDate rechnungsdatum, LocalDate zahlungsziel) {
        return erstelleRechnung(kundenNr, positionen, rechnungsdatum, null, zahlungsziel);
    }

    /**
     * Gemeinsamer Ablauf aller vier Belegarten: erst fachlich prüfen, dann die
     * Belegnummer ziehen, Kopfdaten und Positionen setzen, speichern und die
     * Ansichten benachrichtigen. Die belegtypeigenen Felder (Gültigkeit,
     * Lieferdatum, Zahlungsziel, Status) trägt der Aufrufer über
     * {@code spezifisch} nach.
     *
     * <p>Die Reihenfolge ist bewusst: Kunde, Positionen und Firmenprofil
     * werden geprüft, <em>bevor</em> die Nummer gezogen wird — ein
     * Eingabefehler soll den Nummernkreis gar nicht erst berühren. Scheitert
     * danach das Speichern, rollt die Transaktion die Nummernvergabe zurück
     * (GR-01).
     */
    private <T extends Dokument> T erstelleBeleg(T beleg, String kundenNr,
                                                 List<Positionsangabe> positionen,
                                                 LocalDate datum, Runnable spezifisch) {
        Kunde kunde = pruefeKunde(kundenNr);
        List<Dokumentposition> dokumentpositionen = bauePositionen(positionen);
        Firmenprofil aussteller = firmenprofilService.fuerBeleg(beleg instanceof Rechnung);

        vergebeNummer(beleg, datum);
        beleg.setDatum(datum);
        beleg.setzeKunde(kunde.getKundennummer(), kunde.getName(), kunde.anschrift());
        beleg.setzeAussteller(aussteller);
        beleg.setzePositionen(dokumentpositionen);
        spezifisch.run();
        repository.speichere(beleg);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.DOKUMENTE));
        return beleg;
    }

    @Override
    @Transactional
    public Dokument erzeugeFolgebeleg(String belegnummer) {
        Dokument vorgaenger = pruefeBeleg(belegnummer);
        LocalDate datum = LocalDate.now();

        Dokument folgebeleg = switch (vorgaenger.belegtyp()) {
            case ANGEBOT -> new Auftragsbestaetigung();
            case AUFTRAGSBESTAETIGUNG -> {
                Lieferschein lieferschein = new Lieferschein();
                lieferschein.setLieferdatum(datum);
                yield lieferschein;
            }
            case LIEFERSCHEIN -> {
                // Leistungsdatum ist der Tag der Lieferung, nicht der Tag der
                // Rechnungsstellung (§ 14 Abs. 4 Nr. 6 UStG, A-F-31)
                LocalDate lieferdatum = ((Lieferschein) vorgaenger).getLieferdatum();
                Rechnung rechnung = new Rechnung();
                rechnung.setLeistungsdatum(lieferdatum != null ? lieferdatum : datum);
                rechnung.setZahlungsziel(datum.plusDays(STANDARD_ZAHLUNGSZIEL_TAGE));
                yield rechnung;
            }
            case RECHNUNG -> throw new ValidierungsException("Beleg",
                    "Für eine Rechnung kann kein Folgebeleg erzeugt werden.");
        };
        Firmenprofil aussteller = firmenprofilService.fuerBeleg(folgebeleg instanceof Rechnung);

        vergebeNummer(folgebeleg, datum);
        folgebeleg.setDatum(datum);
        // Übernahme von Kunde, Positionen und Mengen aus dem Vorgänger (GR-05, F-22)
        folgebeleg.setzeKunde(vorgaenger.getKundenReferenz(),
                vorgaenger.getKundeName(), vorgaenger.getKundeAnschrift());
        folgebeleg.setzeAussteller(aussteller);
        folgebeleg.setzePositionen(new ArrayList<>(vorgaenger.getPositionen()));
        folgebeleg.setVorgaengerNr(vorgaenger.getBelegnummer());
        if (folgebeleg instanceof Rechnung rechnung) {
            rechnung.setzeStatus(DokumentStatus.OFFEN);
        }
        repository.speichere(folgebeleg);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.DOKUMENTE));
        return folgebeleg;
    }

    @Override
    @Transactional
    public void versende(String belegnummer) {
        Dokument dokument = pruefeBeleg(belegnummer);
        dokument.versende();
        repository.speichere(dokument);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.DOKUMENTE));
    }

    /**
     * Storniert eine Rechnung (F-19, F-20, A-F-29). Eine noch nicht
     * versendete Rechnung hat den Empfänger nie erreicht und wird nur
     * gekennzeichnet. Eine versendete Rechnung dagegen liegt beim Kunden;
     * sie wird durch eine neue Stornorechnung mit negativen Mengen
     * ausgeglichen, die eine eigene Nummer aus dem lückenlosen Rechnungskreis
     * erhält (F-24). Kennzeichnung und Stornorechnung entstehen in einer
     * Transaktion.
     */
    @Override
    @Transactional
    public Rechnung storniere(String rechnungsnummer) {
        Dokument dokument = pruefeBeleg(rechnungsnummer);
        if (!(dokument instanceof Rechnung rechnung)) {
            throw new ValidierungsException("Beleg",
                    "Nur Rechnungen können storniert werden (F-19).");
        }
        boolean warVersendet = rechnung.getStatus() == DokumentStatus.VERSENDET;
        LocalDate heute = LocalDate.now();
        rechnung.storniere(heute, SYSTEM_BENUTZER);
        repository.speichere(rechnung);

        Rechnung ergebnis = rechnung;
        if (warVersendet) {
            ergebnis = erstelleStornorechnung(rechnung, heute);
        }
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.DOKUMENTE));
        return ergebnis;
    }

    /**
     * Stornorechnung zu einer versendeten Rechnung (A-F-29): gleiche Kunden-,
     * Aussteller- und Leistungsdaten, alle Mengen negiert, ohne Zahlungsziel.
     */
    private Rechnung erstelleStornorechnung(Rechnung original, LocalDate datum) {
        Rechnung storno = new Rechnung();
        vergebeNummer(storno, datum);
        storno.setDatum(datum);
        storno.setzeKunde(original.getKundenReferenz(), original.getKundeName(),
                original.getKundeAnschrift());
        storno.setzeAussteller(original.getAussteller() != null
                ? original.getAussteller()
                : firmenprofilService.fuerBeleg(true));
        storno.setzePositionen(original.getPositionen().stream()
                .map(Dokumentposition::negiert)
                .toList());
        storno.setVorgaengerNr(original.getBelegnummer());
        storno.setStornoZu(original.getBelegnummer());
        storno.setLeistungsdatum(original.getLeistungsdatum());
        storno.setzeStatus(DokumentStatus.OFFEN);
        repository.speichere(storno);
        return storno;
    }

    @Override
    @Transactional
    public void markiereBezahlt(String rechnungsnummer, LocalDate bezahltAm) {
        Dokument dokument = pruefeBeleg(rechnungsnummer);
        if (!(dokument instanceof Rechnung rechnung)) {
            throw new ValidierungsException("Beleg",
                    "Nur Rechnungen können als bezahlt markiert werden (A-F-28).");
        }
        if (bezahltAm == null) {
            throw new ValidierungsException("Zahlungsdatum",
                    "Das Pflichtfeld 'Zahlungsdatum' fehlt (A-F-28).");
        }
        if (rechnung.getDatum() != null && bezahltAm.isBefore(rechnung.getDatum())) {
            throw new ValidierungsException("Zahlungsdatum",
                    "Das 'Zahlungsdatum' darf nicht vor dem Rechnungsdatum liegen (A-F-28).");
        }
        rechnung.markiereBezahlt(bezahltAm);
        repository.speichere(rechnung);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.DOKUMENTE));
    }

    @Override
    public List<Dokument> alleDokumente() {
        return repository.alle();
    }

    @Override
    public List<Rechnung> offeneRechnungen() {
        return repository.alle().stream()
                .filter(d -> d instanceof Rechnung && d.getStatus() == DokumentStatus.OFFEN)
                .map(d -> (Rechnung) d)
                .toList();
    }

    @Override
    public Summen berechneSummen(List<Positionsangabe> positionen) {
        Rechnung probe = new Rechnung();
        probe.setzePositionen(bauePositionen(positionen));
        return new Summen(probe.getSummeNetto(), probe.getSummeSteuer(), probe.getSummeBrutto());
    }

    @Override
    public void exportierePdf(String belegnummer, Path zielDatei) {
        pdfExporter.exportiere(pruefeBeleg(belegnummer), zielDatei);
    }

    /**
     * Zieht die nächste Belegnummer. Liefert der Nummernkreis eine bereits
     * vergebene Nummer, wird abgebrochen, statt einen Bestandsbeleg — womöglich
     * eine versendete Rechnung — stillschweigend zu überschreiben (GR-01, GR-02).
     */
    private void vergebeNummer(Dokument beleg, LocalDate datum) {
        String nummer = nummernGenerator.naechsteNummer(beleg.belegtyp(), datum.getYear());
        if (repository.findeNachNummer(nummer) != null) {
            throw new IllegalStateException("Die Belegnummer " + nummer
                    + " ist bereits vergeben; der Nummernkreis ist inkonsistent.");
        }
        beleg.setBelegnummer(nummer);
    }

    private Kunde pruefeKunde(String kundenNr) {
        if (kundenNr == null || kundenNr.isBlank()) {
            throw new ValidierungsException("Kunde",
                    "Das Pflichtfeld 'Kunde' fehlt (F-18).");
        }
        Kunde kunde = kundenService.findeKunde(kundenNr);
        if (kunde == null) {
            throw new ValidierungsException("Kunde",
                    "Der Kunde " + kundenNr + " existiert nicht.");
        }
        return kunde;
    }

    private Dokument pruefeBeleg(String belegnummer) {
        Dokument dokument = repository.findeNachNummer(belegnummer);
        if (dokument == null) {
            throw new ValidierungsException("Beleg",
                    "Der Beleg " + belegnummer + " existiert nicht.");
        }
        return dokument;
    }

    /** Baut die Positionen mit Produkt-Snapshot; mindestens eine Position erforderlich (F-18). */
    private List<Dokumentposition> bauePositionen(List<Positionsangabe> positionen) {
        if (positionen == null || positionen.isEmpty()) {
            throw new ValidierungsException("Position",
                    "Mindestens eine 'Position' ist erforderlich (F-18).");
        }
        List<Dokumentposition> ergebnis = new ArrayList<>();
        for (Positionsangabe angabe : positionen) {
            if (angabe.menge() <= 0) {
                throw new ValidierungsException("Menge",
                        "Die 'Menge' muss größer als 0 sein.");
            }
            Produkt produkt = produktService.findeProdukt(angabe.produktnummer());
            if (produkt == null) {
                throw new ValidierungsException("Produkt",
                        "Das Produkt " + angabe.produktnummer() + " existiert nicht.");
            }
            // Snapshot von Bezeichnung, Einzelpreis und Steuersatz (GR-03, F-23)
            ergebnis.add(new Dokumentposition(produkt.getProduktnummer(), produkt.getBezeichnung(),
                    angabe.menge(), produkt.getEinzelpreisNetto(), produkt.getSteuersatz()));
        }
        return ergebnis;
    }
}
