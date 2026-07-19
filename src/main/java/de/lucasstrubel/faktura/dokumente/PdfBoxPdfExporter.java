package de.lucasstrubel.faktura.dokumente;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Standardisierter PDF-Export der Belege mit Apache PDFBox
 * (A-F-04, F-07, F-10, F-15), angelehnt an den deutschen Geschäftsbrief:
 * Absender- und Empfängerblock, Belegkopf mit Datum und Referenzen,
 * Positionstabelle mit festen Spalten und rechtsbündigen Beträgen sowie
 * Summenblock.
 *
 * <p>Rechnungen enthalten die Pflichtangaben gemäß § 14 UStG (F-13):
 * Name und Anschrift von Aussteller und Kunde, Belegnummer, Rechnungs- und
 * Leistungsdatum, Positionen mit Mengen und Einzelbeträgen, Steuersatz und
 * Steuerbetrag sowie Netto-/Bruttosummen.
 */
@Component
public class PdfBoxPdfExporter implements PdfExporter {

    /** Aussteller-Stammdaten (§ 14 UStG); bei Produktivbetrieb anzupassen. */
    private static final String AUSSTELLER_NAME = "Faktura Software";
    private static final String AUSSTELLER_STRASSE = "Musterstraße 1";
    private static final String AUSSTELLER_ORT = "68163 Mannheim";
    private static final String AUSSTELLER_UST_ID = "USt-IdNr. DE000000000";

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Seitenränder und Zeilenraster (A4 hoch, Angaben in PDF-Punkten). */
    private static final float RAND = 50;
    private static final float ZEILENHOEHE = 14;
    private static final float SEITENBREITE = PDRectangle.A4.getWidth();
    private static final float RECHTS = SEITENBREITE - RAND;

    /** Spaltenraster der Positionstabelle: Textspalten linksbündig ... */
    private static final float SPALTE_POS = RAND;
    private static final float SPALTE_PRODUKT = 80;
    private static final float SPALTE_BEZEICHNUNG = 155;
    /** ... Zahlenspalten rechtsbündig an ihrer rechten Kante. */
    private static final float SPALTE_MENGE = 385;
    private static final float SPALTE_EINZELPREIS = 460;
    private static final float SPALTE_UST = 497;
    private static final float SPALTE_SUMME = RECHTS;

    private final PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont fett = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    @Override
    public void exportiere(Dokument dokument, Path zielDatei) {
        try (PDDocument pdf = new PDDocument()) {
            Schreiber schreiber = new Schreiber(pdf);

            schreibeBriefkopf(schreiber, dokument);
            schreibeBelegkopf(schreiber, dokument);
            schreibePositionstabelle(schreiber, dokument);
            schreibeSummenblock(schreiber, dokument);
            schreibeSchlusstext(schreiber, dokument);

            schreiber.schliesse();
            Path zielVerzeichnis = zielDatei.getParent();
            if (zielVerzeichnis != null) {
                Files.createDirectories(zielVerzeichnis);
            }
            pdf.save(zielDatei.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("PDF-Export fehlgeschlagen: " + zielDatei, e);
        }
    }

    /** Absenderblock rechts oben, Rücksendezeile und Empfängerblock links. */
    private void schreibeBriefkopf(Schreiber schreiber, Dokument dokument) throws IOException {
        schreiber.rechtsbuendig(fett, 11, AUSSTELLER_NAME, RECHTS);
        schreiber.rechtsbuendig(normal, 9, AUSSTELLER_STRASSE, RECHTS);
        schreiber.rechtsbuendig(normal, 9, AUSSTELLER_ORT, RECHTS);
        schreiber.rechtsbuendig(normal, 9, AUSSTELLER_UST_ID, RECHTS);
        schreiber.leer();

        schreiber.zeile(normal, 7, AUSSTELLER_NAME + " · " + AUSSTELLER_STRASSE
                + " · " + AUSSTELLER_ORT);
        schreiber.linie();
        schreiber.zeile(normal, 10, dokument.getKundeName()
                + "  (Kundennr. " + dokument.getKundenReferenz() + ")");
        // anschrift() liefert "Straße, PLZ Ort" einzeilig; für den
        // Empfängerblock wird sie an der ersten Trennstelle umbrochen
        String anschrift = dokument.getKundeAnschrift();
        int trenner = anschrift == null ? -1 : anschrift.indexOf(", ");
        if (trenner >= 0) {
            schreiber.zeile(normal, 10, anschrift.substring(0, trenner));
            schreiber.zeile(normal, 10, anschrift.substring(trenner + 2));
        } else if (anschrift != null) {
            schreiber.zeile(normal, 10, anschrift);
        }
        schreiber.leer();
        schreiber.leer();
    }

    /** Belegtitel, Stornokennzeichen und Datums-/Referenzangaben. */
    private void schreibeBelegkopf(Schreiber schreiber, Dokument dokument) throws IOException {
        schreiber.zeile(fett, 16, dokument.belegtyp().anzeigename() + " " + dokument.getBelegnummer());
        if (dokument.getStatus() == DokumentStatus.STORNIERT) {
            schreiber.zeile(fett, 12, "*** STORNIERT ***");
        }
        schreiber.leer();
        schreiber.zeile(normal, 10, "Datum: " + format(dokument.getDatum()));
        if (dokument instanceof Angebot angebot) {
            schreiber.zeile(normal, 10, "Gültig bis: " + format(angebot.getGueltigBis()));
        }
        if (dokument instanceof Lieferschein lieferschein) {
            schreiber.zeile(normal, 10, "Lieferdatum: " + format(lieferschein.getLieferdatum()));
        }
        if (dokument instanceof Rechnung rechnung) {
            schreiber.zeile(normal, 10, "Leistungsdatum: " + format(rechnung.getLeistungsdatum()));
            schreiber.zeile(normal, 10, "Zahlbar bis: " + format(rechnung.getZahlungsziel()));
        }
        if (dokument.getVorgaengerNr() != null) {
            schreiber.zeile(normal, 10, "Referenzbeleg: " + dokument.getVorgaengerNr());
        }
        schreiber.leer();
    }

    /** Positionstabelle im festen Spaltenraster; Kopf wird je Seite wiederholt. */
    private void schreibePositionstabelle(Schreiber schreiber, Dokument dokument) throws IOException {
        schreibeTabellenkopf(schreiber);
        int pos = 1;
        for (Dokumentposition position : dokument.getPositionen()) {
            if (!schreiber.passtNochZeile()) {
                schreiber.neueSeite();
                schreibeTabellenkopf(schreiber);
            }
            schreiber.beginneZeile();
            schreiber.text(normal, 10, String.valueOf(pos++), SPALTE_POS);
            schreiber.text(normal, 10, position.getProduktReferenz(), SPALTE_PRODUKT);
            schreiber.text(normal, 10, kuerze(position.getBezeichnung(), 40), SPALTE_BEZEICHNUNG);
            schreiber.textRechts(normal, 10, String.valueOf(position.getMenge()), SPALTE_MENGE);
            schreiber.textRechts(normal, 10, betrag(position.getEinzelpreisNetto()), SPALTE_EINZELPREIS);
            schreiber.textRechts(normal, 10, prozent(position.getSteuersatz()) + " %", SPALTE_UST);
            schreiber.textRechts(normal, 10, betrag(position.getPositionssummeNetto()), SPALTE_SUMME);
            schreiber.beendeZeile();
        }
        schreiber.linie();
    }

    private void schreibeTabellenkopf(Schreiber schreiber) throws IOException {
        schreiber.beginneZeile();
        schreiber.text(fett, 10, "Pos", SPALTE_POS);
        schreiber.text(fett, 10, "Produkt", SPALTE_PRODUKT);
        schreiber.text(fett, 10, "Bezeichnung", SPALTE_BEZEICHNUNG);
        schreiber.textRechts(fett, 10, "Menge", SPALTE_MENGE);
        schreiber.textRechts(fett, 10, "Einzelpreis", SPALTE_EINZELPREIS);
        schreiber.textRechts(fett, 10, "USt", SPALTE_UST);
        schreiber.textRechts(fett, 10, "Summe", SPALTE_SUMME);
        schreiber.beendeZeile();
        schreiber.linie();
    }

    /** Summen rechtsbündig unter der Tabelle; Bruttosumme hervorgehoben (F-03). */
    private void schreibeSummenblock(Schreiber schreiber, Dokument dokument) throws IOException {
        summenzeile(schreiber, normal, 10, "Summe netto:", dokument.getSummeNetto());
        summenzeile(schreiber, normal, 10, "Umsatzsteuer:", dokument.getSummeSteuer());
        summenzeile(schreiber, fett, 11, "Summe brutto:", dokument.getSummeBrutto());
        schreiber.leer();
    }

    private void summenzeile(Schreiber schreiber, PDFont font, float groesse,
                             String beschriftung, BigDecimal wert) throws IOException {
        schreiber.beginneZeile();
        schreiber.textRechts(font, groesse, beschriftung, SPALTE_EINZELPREIS);
        schreiber.textRechts(font, groesse, betrag(wert) + " EUR", SPALTE_SUMME);
        schreiber.beendeZeile();
    }

    /** Belegtyp-spezifischer Hinweistext am Ende des Dokuments. */
    private void schreibeSchlusstext(Schreiber schreiber, Dokument dokument) throws IOException {
        if (dokument instanceof Rechnung rechnung && rechnung.getZahlungsziel() != null
                && dokument.getStatus() != DokumentStatus.STORNIERT) {
            schreiber.zeile(normal, 10, "Bitte überweisen Sie den Rechnungsbetrag bis zum "
                    + format(rechnung.getZahlungsziel()) + ".");
        }
        if (dokument instanceof Angebot angebot && angebot.getGueltigBis() != null) {
            schreiber.zeile(normal, 10, "Dieses Angebot ist gültig bis zum "
                    + format(angebot.getGueltigBis()) + ".");
        }
    }

    private static String format(LocalDate datum) {
        return datum == null ? "—" : DATUM.format(datum);
    }

    /** Deutsches Betragsformat mit Tausenderpunkt, z. B. "1.234,56". */
    private static String betrag(BigDecimal wert) {
        if (wert == null) {
            return "—";
        }
        NumberFormat format = NumberFormat.getNumberInstance(Locale.GERMANY);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(wert);
    }

    private static String prozent(BigDecimal steuersatz) {
        if (steuersatz == null) {
            return "—";
        }
        return steuersatz.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString();
    }

    private static String kuerze(String text, int maxLaenge) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLaenge ? text : text.substring(0, maxLaenge - 1) + "…";
    }

    /**
     * Zeilenweiser Schreiber mit automatischem Seitenumbruch. Einfache
     * Zeilen entstehen über {@link #zeile}; Tabellenzeilen mit mehreren
     * Spalten über {@link #beginneZeile}, {@link #text}/{@link #textRechts}
     * und {@link #beendeZeile}.
     */
    private static final class Schreiber {

        private final PDDocument pdf;
        private PDPageContentStream inhalt;
        private float y;

        Schreiber(PDDocument pdf) throws IOException {
            this.pdf = pdf;
            neueSeite();
        }

        void neueSeite() throws IOException {
            if (inhalt != null) {
                inhalt.close();
            }
            PDPage seite = new PDPage(PDRectangle.A4);
            pdf.addPage(seite);
            inhalt = new PDPageContentStream(pdf, seite);
            y = PDRectangle.A4.getHeight() - RAND;
        }

        boolean passtNochZeile() {
            return y >= RAND + ZEILENHOEHE;
        }

        /** Beginnt eine Tabellenzeile; bricht bei Bedarf auf eine neue Seite um. */
        void beginneZeile() throws IOException {
            if (!passtNochZeile()) {
                neueSeite();
            }
        }

        void beendeZeile() {
            y -= ZEILENHOEHE;
        }

        /** Linksbündiger Text an Spaltenposition {@code x} der aktuellen Zeile. */
        void text(PDFont font, float groesse, String text, float x) throws IOException {
            inhalt.beginText();
            inhalt.setFont(font, groesse);
            inhalt.newLineAtOffset(x, y);
            inhalt.showText(text == null ? "" : text);
            inhalt.endText();
        }

        /** Rechtsbündiger Text: {@code xRechts} ist die rechte Kante der Spalte. */
        void textRechts(PDFont font, float groesse, String text, float xRechts) throws IOException {
            String sicher = text == null ? "" : text;
            float breite = font.getStringWidth(sicher) / 1000 * groesse;
            text(font, groesse, sicher, xRechts - breite);
        }

        /** Einzelne linksbündige Zeile am linken Seitenrand. */
        void zeile(PDFont font, float groesse, String text) throws IOException {
            beginneZeile();
            text(font, groesse, text, RAND);
            beendeZeile();
        }

        /** Einzelne rechtsbündige Zeile (z. B. Absenderblock). */
        void rechtsbuendig(PDFont font, float groesse, String text, float xRechts) throws IOException {
            beginneZeile();
            textRechts(font, groesse, text, xRechts);
            beendeZeile();
        }

        /** Horizontale Trennlinie über die volle Satzspiegelbreite. */
        void linie() throws IOException {
            beginneZeile();
            inhalt.moveTo(RAND, y + ZEILENHOEHE - 4);
            inhalt.lineTo(RECHTS, y + ZEILENHOEHE - 4);
            inhalt.setLineWidth(0.5f);
            inhalt.stroke();
            y -= ZEILENHOEHE / 2;
        }

        void leer() {
            y -= ZEILENHOEHE / 2;
        }

        void schliesse() throws IOException {
            inhalt.close();
        }
    }
}
