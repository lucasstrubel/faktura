package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.gemeinsam.Csv;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static de.lucasstrubel.faktura.gemeinsam.Csv.TRENNZEICHEN;
import static de.lucasstrubel.faktura.gemeinsam.Csv.feld;
import static de.lucasstrubel.faktura.gemeinsam.Csv.zahl;

import org.springframework.stereotype.Component;

/**
 * Export aller Belege (Bewegungsdaten) als CSV (Q-08, IF-04): UTF-8,
 * Semikolon-getrennt, mit Kopfzeile. Es wird eine Zeile je Dokumentposition
 * (denormalisiert) geschrieben, sodass der vollständige Datenbestand inklusive
 * Kopf- und Positionsdaten enthalten ist. Belege ohne Positionen erscheinen mit
 * leeren Positionsfeldern, damit kein Beleg verloren geht.
 */
@Component
public class DokumentCsvExport {

    private final DokumentRepository repository;

    public DokumentCsvExport(DokumentRepository repository) {
        this.repository = repository;
    }

    public void exportiereCsv(Path zielDatei) {
        List<String> zeilen = new ArrayList<>();
        zeilen.add(String.join(TRENNZEICHEN,
                "belegnummer", "belegtyp", "datum", "status", "vorgaengerNr",
                "kundenNr", "kundeName", "kundeAnschrift",
                "summeNetto", "summeSteuer", "summeBrutto",
                "gueltigBis", "lieferdatum", "leistungsdatum", "zahlungsziel",
                "storniertAm", "storniertVon", "bezahltAm", "stornoZu",
                "produktnummer", "bezeichnung", "menge",
                "einzelpreisNetto", "steuersatz", "positionssummeNetto", "positionssummeBrutto",
                "ausstellerName", "ausstellerStrasse", "ausstellerPlz", "ausstellerOrt",
                "ausstellerUstIdNr", "ausstellerSteuernummer", "ausstellerTelefon", "ausstellerEMail",
                "ausstellerIban", "ausstellerBic", "ausstellerBank", "einheit", "einheitCode", "berechnungsart"));
        for (Dokument dokument : repository.alle()) {
            if (dokument.getPositionen().isEmpty()) {
                zeilen.add(belegFelder(dokument) + TRENNZEICHEN + leerePositionsFelder() + TRENNZEICHEN + ergaenzung(dokument, null));
            } else {
                for (Dokumentposition position : dokument.getPositionen()) {
                    zeilen.add(belegFelder(dokument) + TRENNZEICHEN + positionsFelder(position) + TRENNZEICHEN + ergaenzung(dokument, position));
                }
            }
        }
        Csv.schreibe(zielDatei, zeilen);
    }

    public void exportiereSteuern(Path zielDatei) {
        List<String> zeilen = new ArrayList<>();
        zeilen.add("belegnummer;steuersatz;netto;steuer;berechnungsart");
        for (Dokument dokument : repository.alle()) {
            for (Steuerzeile zeile : dokument.steueraufschluesselung()) {
                zeilen.add(String.join(TRENNZEICHEN, feld(dokument.getBelegnummer()),
                        zahl(betrag(zeile.steuersatz())), zahl(betrag(zeile.netto())), zahl(betrag(zeile.steuer())),
                        dokument.getBetragssnapshot().berechnungsart().name()));
            }
        }
        Csv.schreibe(zielDatei, zeilen);
    }

    private static String ergaenzung(Dokument dokument, Dokumentposition position) {
        var firma = dokument.getAussteller();
        return String.join(TRENNZEICHEN,
                feld(firma == null ? null : firma.name()), feld(firma == null ? null : firma.strasse()),
                feld(firma == null ? null : firma.plz()), feld(firma == null ? null : firma.ort()),
                feld(firma == null ? null : firma.ustIdNr()), feld(firma == null ? null : firma.steuernummer()),
                feld(firma == null ? null : firma.telefon()), feld(firma == null ? null : firma.eMail()),
                feld(firma == null ? null : firma.iban()), feld(firma == null ? null : firma.bic()),
                feld(firma == null ? null : firma.bank()), feld(position == null ? null : position.getEinheit()),
                feld(position == null ? null : position.getEinheitCode()), dokument.getBetragssnapshot().berechnungsart().name());
    }

    private static String belegFelder(Dokument dokument) {
        Rechnung rechnung = dokument instanceof Rechnung r ? r : null;
        Angebot angebot = dokument instanceof Angebot a ? a : null;
        Lieferschein lieferschein = dokument instanceof Lieferschein l ? l : null;
        return String.join(TRENNZEICHEN,
                feld(dokument.getBelegnummer()),
                feld(dokument.belegtyp().name()),
                feld(datum(dokument.getDatum())),
                feld(dokument.getStatus() == null ? null : dokument.getStatus().name()),
                feld(dokument.getVorgaengerNr()),
                feld(dokument.getKundenReferenz()),
                feld(dokument.getKundeName()),
                feld(dokument.getKundeAnschrift()),
                zahl(betrag(dokument.getSummeNetto())),
                zahl(betrag(dokument.getSummeSteuer())),
                zahl(betrag(dokument.getSummeBrutto())),
                feld(angebot == null ? null : datum(angebot.getGueltigBis())),
                feld(lieferschein == null ? null : datum(lieferschein.getLieferdatum())),
                feld(rechnung == null ? null : datum(rechnung.getLeistungsdatum())),
                feld(rechnung == null ? null : datum(rechnung.getZahlungsziel())),
                feld(rechnung == null ? null : datum(rechnung.getStorniertAm())),
                feld(rechnung == null ? null : rechnung.getStorniertVon()),
                feld(rechnung == null ? null : datum(rechnung.getBezahltAm())),
                feld(rechnung == null ? null : rechnung.getStornoZu()));
    }

    private static String positionsFelder(Dokumentposition position) {
        return String.join(TRENNZEICHEN,
                feld(position.getProduktReferenz()),
                feld(position.getBezeichnung()),
                zahl(Integer.toString(position.getMenge())),
                zahl(betrag(position.getEinzelpreisNetto())),
                zahl(betrag(position.getSteuersatz())),
                zahl(betrag(position.getPositionssummeNetto())),
                zahl(betrag(position.getPositionssummeBrutto())));
    }

    private static String leerePositionsFelder() {
        return TRENNZEICHEN.repeat(6);
    }

    private static String datum(LocalDate datum) {
        return datum == null ? null : datum.toString();
    }

    private static String betrag(BigDecimal wert) {
        return wert == null ? null : wert.toPlainString();
    }
}
