package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.gemeinsam.DatenBereich;
import de.lucasstrubel.faktura.gemeinsam.DatenGeaendertEreignis;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
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
 * {@link ProduktService}, {@link DokumentRepository} und {@link PdfExporter}.
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
    private final PdfExporter pdfExporter;
    private final ApplicationEventPublisher ereignisse;

    public StandardDokumentService(DokumentRepository repository,
                                   BelegnummernGenerator nummernGenerator,
                                   KundenService kundenService,
                                   ProduktService produktService,
                                   PdfExporter pdfExporter) {
        this(repository, nummernGenerator, kundenService, produktService, pdfExporter,
                ereignis -> { });
    }

    @Autowired
    public StandardDokumentService(DokumentRepository repository,
                                   BelegnummernGenerator nummernGenerator,
                                   KundenService kundenService,
                                   ProduktService produktService,
                                   PdfExporter pdfExporter,
                                   ApplicationEventPublisher ereignisse) {
        this.repository = repository;
        this.nummernGenerator = nummernGenerator;
        this.kundenService = kundenService;
        this.produktService = produktService;
        this.pdfExporter = pdfExporter;
        this.ereignisse = ereignisse;
    }

    @Override
    public Angebot erstelleAngebot(String kundenNr, List<Positionsangabe> positionen, LocalDate gueltigBis) {
        Kunde kunde = pruefeKunde(kundenNr);
        List<Dokumentposition> dokumentpositionen = bauePositionen(positionen);
        LocalDate datum = LocalDate.now();

        Angebot angebot = new Angebot();
        angebot.setBelegnummer(nummernGenerator.naechsteNummer(Belegtyp.ANGEBOT, datum.getYear()));
        angebot.setDatum(datum);
        angebot.setzeKunde(kunde.getKundennummer(), kunde.getName(), kunde.anschrift());
        angebot.setGueltigBis(gueltigBis != null ? gueltigBis : datum.plusDays(STANDARD_GUELTIGKEIT_TAGE));
        angebot.setzePositionen(dokumentpositionen);
        repository.speichere(angebot);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.DOKUMENTE));
        return angebot;
    }

    @Override
    public Auftragsbestaetigung erstelleAuftragsbestaetigung(String kundenNr, List<Positionsangabe> positionen) {
        Kunde kunde = pruefeKunde(kundenNr);
        List<Dokumentposition> dokumentpositionen = bauePositionen(positionen);
        LocalDate datum = LocalDate.now();

        Auftragsbestaetigung ab = new Auftragsbestaetigung();
        ab.setBelegnummer(nummernGenerator.naechsteNummer(Belegtyp.AUFTRAGSBESTAETIGUNG, datum.getYear()));
        ab.setDatum(datum);
        ab.setzeKunde(kunde.getKundennummer(), kunde.getName(), kunde.anschrift());
        ab.setzePositionen(dokumentpositionen);
        repository.speichere(ab);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.DOKUMENTE));
        return ab;
    }

    @Override
    public Lieferschein erstelleLieferschein(String kundenNr, List<Positionsangabe> positionen, LocalDate lieferdatum) {
        Kunde kunde = pruefeKunde(kundenNr);
        List<Dokumentposition> dokumentpositionen = bauePositionen(positionen);
        LocalDate datum = LocalDate.now();

        Lieferschein lieferschein = new Lieferschein();
        lieferschein.setBelegnummer(nummernGenerator.naechsteNummer(Belegtyp.LIEFERSCHEIN, datum.getYear()));
        lieferschein.setDatum(datum);
        lieferschein.setzeKunde(kunde.getKundennummer(), kunde.getName(), kunde.anschrift());
        lieferschein.setLieferdatum(lieferdatum != null ? lieferdatum : datum);
        lieferschein.setzePositionen(dokumentpositionen);
        repository.speichere(lieferschein);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.DOKUMENTE));
        return lieferschein;
    }

    @Override
    public Rechnung erstelleRechnung(String kundenNr, List<Positionsangabe> positionen,
                                     LocalDate rechnungsdatum, LocalDate zahlungsziel) {
        Kunde kunde = pruefeKunde(kundenNr);
        List<Dokumentposition> dokumentpositionen = bauePositionen(positionen);
        if (rechnungsdatum == null) {
            throw new ValidierungsException("Rechnungsdatum",
                    "Das Pflichtfeld 'Rechnungsdatum' fehlt (F-18).");
        }

        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer(nummernGenerator.naechsteNummer(Belegtyp.RECHNUNG, rechnungsdatum.getYear()));
        rechnung.setDatum(rechnungsdatum);
        rechnung.setLeistungsdatum(rechnungsdatum);
        rechnung.setzeKunde(kunde.getKundennummer(), kunde.getName(), kunde.anschrift());
        rechnung.setZahlungsziel(zahlungsziel != null
                ? zahlungsziel
                : rechnungsdatum.plusDays(STANDARD_ZAHLUNGSZIEL_TAGE));
        rechnung.setzePositionen(dokumentpositionen);
        rechnung.setzeStatus(DokumentStatus.OFFEN);
        repository.speichere(rechnung);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.DOKUMENTE));
        return rechnung;
    }

    @Override
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
                Rechnung rechnung = new Rechnung();
                rechnung.setLeistungsdatum(datum);
                rechnung.setZahlungsziel(datum.plusDays(STANDARD_ZAHLUNGSZIEL_TAGE));
                yield rechnung;
            }
            case RECHNUNG -> throw new ValidierungsException("Beleg",
                    "Für eine Rechnung kann kein Folgebeleg erzeugt werden.");
        };

        folgebeleg.setBelegnummer(nummernGenerator.naechsteNummer(folgebeleg.belegtyp(), datum.getYear()));
        folgebeleg.setDatum(datum);
        // Übernahme von Kunde, Positionen und Mengen aus dem Vorgänger (GR-05, F-22)
        folgebeleg.setzeKunde(vorgaenger.getKundenReferenz(),
                vorgaenger.getKundeName(), vorgaenger.getKundeAnschrift());
        folgebeleg.setzePositionen(new ArrayList<>(vorgaenger.getPositionen()));
        folgebeleg.setVorgaengerNr(vorgaenger.getBelegnummer());
        if (folgebeleg instanceof Rechnung rechnung) {
            rechnung.setLeistungsdatum(datum);
            rechnung.setzeStatus(DokumentStatus.OFFEN);
        }
        repository.speichere(folgebeleg);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.DOKUMENTE));
        return folgebeleg;
    }

    @Override
    public void versende(String belegnummer) {
        Dokument dokument = pruefeBeleg(belegnummer);
        dokument.versende();
        repository.speichere(dokument);
        ereignisse.publishEvent(new DatenGeaendertEreignis(DatenBereich.DOKUMENTE));
    }

    @Override
    public void storniere(String rechnungsnummer) {
        Dokument dokument = pruefeBeleg(rechnungsnummer);
        if (!(dokument instanceof Rechnung rechnung)) {
            throw new ValidierungsException("Beleg",
                    "Nur Rechnungen können storniert werden (F-19).");
        }
        rechnung.storniere(LocalDate.now(), SYSTEM_BENUTZER);
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
