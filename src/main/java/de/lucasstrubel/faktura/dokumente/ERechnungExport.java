package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.firma.Firmenprofil;
import de.lucasstrubel.faktura.firma.FirmenprofilService;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import org.mustangproject.BankDetails;
import org.mustangproject.Invoice;
import org.mustangproject.Item;
import org.mustangproject.Product;
import org.mustangproject.TradeParty;
import org.mustangproject.ZUGFeRD.Profiles;
import org.mustangproject.ZUGFeRD.ZUGFeRD2PullProvider;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * E-Rechnung nach EN 16931: erzeugt aus einer {@link Rechnung} das
 * strukturierte CII-XML (ZUGFeRD-Profil EN 16931, Grundlage der
 * E-Rechnungspflicht im B2B-Bereich seit 01.01.2025) mit der
 * Mustang-Bibliothek (A-F-30). Verwendet ausschließlich die im Beleg
 * gespeicherten Snapshots von Kunde und Aussteller (C-F-06, A-F-27); nur
 * Belege ohne Aussteller-Snapshot (Altbestand) greifen auf das aktuelle
 * Firmenprofil zurück.
 *
 * <p>Eine Stornorechnung wird als korrigierte Rechnung (Dokumentart 384)
 * mit Verweis auf die stornierte Rechnung ausgegeben (A-F-29); eine
 * stornierte Rechnung selbst wird nicht mehr als E-Rechnung ausgestellt.
 */
@Component
public class ERechnungExport {

    /** Einheit "Stück" nach UN/ECE Recommendation 20. */
    private static final String EINHEIT_STUECK = "C62";

    private final FirmenprofilService firmenprofilService;

    public ERechnungExport(FirmenprofilService firmenprofilService) {
        this.firmenprofilService = firmenprofilService;
    }

    /** Schreibt das EN-16931-XML der Rechnung in die Zieldatei. */
    public void exportiereXml(Rechnung rechnung, Path zielDatei) {
        if (rechnung.getStatus() == DokumentStatus.STORNIERT) {
            throw new ValidierungsException("Beleg", "Die Rechnung " + rechnung.getBelegnummer()
                    + " ist storniert und wird nicht mehr als E-Rechnung ausgestellt.");
        }
        ZUGFeRD2PullProvider provider = new ZUGFeRD2PullProvider();
        provider.setProfile(Profiles.getByName("EN16931"));
        provider.generateXML(baueInvoice(rechnung));
        try {
            Files.write(zielDatei, provider.getXML());
        } catch (IOException e) {
            throw new UncheckedIOException("E-Rechnung-Export fehlgeschlagen: " + zielDatei, e);
        }
    }

    private Invoice baueInvoice(Rechnung rechnung) {
        Firmenprofil firma = aussteller(rechnung);
        if (!firma.hatSteuerkennung()) {
            // EN 16931 BR-CO-26: ohne Steuerkennung des Verkäufers ungültig
            throw new ValidierungsException("Steuernummer",
                    "Für die E-Rechnung ist im Firmenprofil eine Steuernummer oder USt-IdNr. "
                            + "erforderlich (A-F-26).");
        }
        TradeParty verkaeufer = new TradeParty(firma.name(), firma.strasse(),
                firma.plz(), firma.ort(), "DE");
        if (firma.ustIdNr() != null && !firma.ustIdNr().isBlank()) {
            verkaeufer.addVATID(firma.ustIdNr());
        }
        if (firma.steuernummer() != null && !firma.steuernummer().isBlank()) {
            verkaeufer.addTaxID(firma.steuernummer());
        }
        if (firma.iban() != null && !firma.iban().isBlank()) {
            BankDetails bank = new BankDetails(firma.iban().replace(" ", ""));
            if (firma.bic() != null && !firma.bic().isBlank()) {
                bank.setBIC(firma.bic());
            }
            verkaeufer.addBankDetails(bank);
        }

        Invoice invoice = new Invoice()
                .setNumber(rechnung.getBelegnummer())
                .setIssueDate(datum(rechnung.getDatum()))
                .setDueDate(datum(rechnung.getZahlungsziel()))
                .setDeliveryDate(datum(rechnung.getLeistungsdatum() != null
                        ? rechnung.getLeistungsdatum() : rechnung.getDatum()))
                .setSender(verkaeufer)
                .setRecipient(kaeufer(rechnung));
        if (rechnung.istStornorechnung()) {
            invoice.setCorrection(rechnung.getStornoZu());
        }

        for (Dokumentposition position : rechnung.getPositionen()) {
            BigDecimal steuersatzProzent = position.getSteuersatz() == null
                    ? BigDecimal.ZERO
                    : position.getSteuersatz().multiply(new BigDecimal("100"));
            Product produkt = new Product(position.getBezeichnung(), "",
                    EINHEIT_STUECK, steuersatzProzent);
            invoice.addItem(new Item(produkt,
                    position.getEinzelpreisNetto() == null
                            ? BigDecimal.ZERO : position.getEinzelpreisNetto(),
                    BigDecimal.valueOf(position.getMenge())));
        }
        return invoice;
    }

    private Firmenprofil aussteller(Rechnung rechnung) {
        if (rechnung.getAussteller() != null) {
            return rechnung.getAussteller();
        }
        return firmenprofilService.lade().orElseThrow(() -> new ValidierungsException(
                "Firmenprofil", "Für die E-Rechnung fehlt das Firmenprofil — bitte unter "
                        + "'Einstellungen' hinterlegen."));
    }

    /**
     * Der Beleg speichert die Kundenanschrift als Snapshot
     * {@code "Straße, PLZ Ort"} (C-F-06); für die E-Rechnung wird sie in
     * ihre Bestandteile zerlegt — an der letzten Trennstelle, damit ein Komma
     * in der Straßenangabe ("Hauptstr. 5, Hinterhaus") nicht die PLZ verschiebt.
     */
    private static TradeParty kaeufer(Rechnung rechnung) {
        String anschrift = rechnung.getKundeAnschrift() == null ? "" : rechnung.getKundeAnschrift();
        String strasse = anschrift;
        String plz = "";
        String ort = "";
        int trenner = anschrift.lastIndexOf(", ");
        if (trenner >= 0) {
            strasse = anschrift.substring(0, trenner);
            String plzOrt = anschrift.substring(trenner + 2).strip();
            int leerzeichen = plzOrt.indexOf(' ');
            if (leerzeichen > 0) {
                plz = plzOrt.substring(0, leerzeichen);
                ort = plzOrt.substring(leerzeichen + 1);
            } else {
                ort = plzOrt;
            }
        }
        TradeParty kaeufer = new TradeParty(rechnung.getKundeName(), strasse, plz, ort, "DE");
        kaeufer.setID(rechnung.getKundenReferenz());
        return kaeufer;
    }

    private static Date datum(LocalDate wert) {
        return wert == null ? null
                : Date.from(wert.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
