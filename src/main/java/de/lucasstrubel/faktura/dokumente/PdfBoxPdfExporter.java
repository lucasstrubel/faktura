package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.firma.Firmenprofil;
import de.lucasstrubel.faktura.firma.FirmenprofilService;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Standardisierter PDF-Export der Belege mit Apache PDFBox
 * (A-F-04, F-07, F-10, F-15), angelehnt an den deutschen Geschäftsbrief:
 * Absender- und Empfängerblock, Belegkopf mit Datum und Referenzen,
 * Positionstabelle mit festen Spalten und rechtsbündigen Beträgen sowie
 * Summenblock.
 *
 * <p>Rechnungen enthalten die Pflichtangaben gemäß § 14 UStG (F-13):
 * Name und Anschrift von Aussteller und Kunde, Steuernummer bzw. USt-IdNr.
 * des Ausstellers, Belegnummer, Rechnungs- und Leistungsdatum, Positionen mit
 * Mengen und Einzelbeträgen sowie Entgelt und Steuerbetrag je Steuersatz
 * (A-F-25).
 *
 * <p>Die Schrift (Liberation Sans, SIL Open Font License) wird eingebettet:
 * Die PDF-Standardschriften kennen nur den Zeichenvorrat WinAnsi, und ein
 * Kunde namens "Łukasz" ließ den Export sonst scheitern. Zeichen, die auch
 * die eingebettete Schrift nicht darstellen kann (etwa Emoji), werden durch
 * "?" ersetzt statt den Export abzubrechen. Lange Texte werden nach ihrer
 * gemessenen Breite umbrochen, nicht abgeschnitten.
 */
@Component
public class PdfBoxPdfExporter implements PdfExporter {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Seitenränder und Zeilenraster (A4 hoch, Angaben in PDF-Punkten). */
    private static final float RAND = 50;
    private static final float ZEILENHOEHE = 14;
    private static final float SEITENBREITE = PDRectangle.A4.getWidth();
    private static final float RECHTS = SEITENBREITE - RAND;
    private static final float SATZBREITE = RECHTS - RAND;

    /** Spaltenraster der Positionstabelle: Textspalten linksbündig ... */
    private static final float SPALTE_POS = RAND;
    private static final float SPALTE_PRODUKT = 80;

    private static final String SCHRIFT_NORMAL = "/schrift/LiberationSans-Regular.ttf";
    private static final String SCHRIFT_FETT = "/schrift/LiberationSans-Bold.ttf";

    /** Aussteller-Stammdaten für Belege ohne Snapshot (Altbestand vor v3.0). */
    private final FirmenprofilService firmenprofilService;

    private final byte[] schriftNormal = ladeSchrift(SCHRIFT_NORMAL);
    private final byte[] schriftFett = ladeSchrift(SCHRIFT_FETT);

    /** Ohne Service (Tests): Belege müssen einen Aussteller-Snapshot tragen. */
    public PdfBoxPdfExporter() {
        this(null);
    }

    @Autowired
    public PdfBoxPdfExporter(FirmenprofilService firmenprofilService) {
        this.firmenprofilService = firmenprofilService;
    }

    /**
     * Aussteller des Belegs: der beim Erstellen gespeicherte Snapshot
     * (A-F-27); nur Belege aus der Zeit vor dem Snapshot fallen auf das
     * aktuelle Firmenprofil zurück.
     */
    private Firmenprofil aussteller(Dokument dokument) {
        if (dokument.getAussteller() != null) {
            return dokument.getAussteller();
        }
        if (firmenprofilService != null) {
            return firmenprofilService.lade().orElseThrow(PdfBoxPdfExporter::keinProfil);
        }
        throw keinProfil();
    }

    private static ValidierungsException keinProfil() {
        return new ValidierungsException("Firmenprofil",
                "Für den PDF-Export fehlt das Firmenprofil — bitte unter 'Einstellungen' hinterlegen.");
    }

    @Override
    public void exportiere(Dokument dokument, Path zielDatei) {
        Firmenprofil firma = aussteller(dokument);
        try (PDDocument pdf = new PDDocument()) {
            Schreiber schreiber = new Schreiber(pdf,
                    PDType0Font.load(pdf, new ByteArrayInputStream(schriftNormal)),
                    PDType0Font.load(pdf, new ByteArrayInputStream(schriftFett)));

            schreibeBriefkopf(schreiber, dokument, firma);
            schreibeBelegkopf(schreiber, dokument);
            schreibePositionstabelle(schreiber, dokument);
            schreibeSummenblock(schreiber, dokument);
            schreibeSchlusstext(schreiber, dokument, firma);

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
    private void schreibeBriefkopf(Schreiber schreiber, Dokument dokument, Firmenprofil firma)
            throws IOException {
        schreiber.rechtsbuendig(schreiber.fett, 11, firma.name(), RECHTS);
        schreiber.rechtsbuendig(schreiber.normal, 9, firma.strasse(), RECHTS);
        schreiber.rechtsbuendig(schreiber.normal, 9, firma.plzOrt(), RECHTS);
        if (istGesetzt(firma.steuernummer())) {
            schreiber.rechtsbuendig(schreiber.normal, 9, "Steuernummer " + firma.steuernummer(), RECHTS);
        }
        if (istGesetzt(firma.ustIdNr())) {
            schreiber.rechtsbuendig(schreiber.normal, 9, "USt-IdNr. " + firma.ustIdNr(), RECHTS);
        }
        schreiber.leer();

        schreiber.absatz(schreiber.normal, 7, firma.name() + " · " + firma.strasse()
                + " · " + firma.plzOrt());
        schreiber.linie();
        schreiber.absatz(schreiber.normal, 10, dokument.getKundeName()
                + "  (Kundennr. " + dokument.getKundenReferenz() + ")");
        for (String zeile : anschriftZeilen(dokument.getKundeAnschrift())) {
            schreiber.absatz(schreiber.normal, 10, zeile);
        }
        schreiber.leer();
        schreiber.leer();
    }

    /**
     * Die Anschrift liegt als {@code "Straße, PLZ Ort"} vor (C-F-06); getrennt
     * wird an der <em>letzten</em> Trennstelle, damit eine Straßenangabe mit
     * Komma ("Hauptstr. 5, Hinterhaus") vollständig in der ersten Zeile bleibt.
     */
    static List<String> anschriftZeilen(String anschrift) {
        if (anschrift == null || anschrift.isBlank()) {
            return List.of();
        }
        int trenner = anschrift.lastIndexOf(", ");
        if (trenner < 0) {
            return List.of(anschrift);
        }
        return List.of(anschrift.substring(0, trenner), anschrift.substring(trenner + 2));
    }

    /** Belegtitel, Stornokennzeichen und Datums-/Referenzangaben. */
    private void schreibeBelegkopf(Schreiber schreiber, Dokument dokument) throws IOException {
        Rechnung rechnung = dokument instanceof Rechnung r ? r : null;
        String titel = rechnung != null && rechnung.istStornorechnung()
                ? "Stornorechnung" : dokument.belegtyp().anzeigename();
        schreiber.absatz(schreiber.fett, 16, titel + " " + dokument.getBelegnummer());
        if (dokument.getStatus() == DokumentStatus.STORNIERT) {
            schreiber.absatz(schreiber.fett, 12, "*** STORNIERT ***");
        }
        schreiber.leer();
        schreiber.absatz(schreiber.normal, 10, "Datum: " + format(dokument.getDatum()));
        if (dokument instanceof Angebot angebot) {
            schreiber.absatz(schreiber.normal, 10, "Gültig bis: " + format(angebot.getGueltigBis()));
        }
        if (dokument instanceof Lieferschein lieferschein) {
            schreiber.absatz(schreiber.normal, 10, "Lieferdatum: " + format(lieferschein.getLieferdatum()));
        }
        if (rechnung != null) {
            schreiber.absatz(schreiber.normal, 10, "Leistungsdatum: " + format(rechnung.getLeistungsdatum()));
            if (rechnung.getZahlungsziel() != null) {
                schreiber.absatz(schreiber.normal, 10, "Zahlbar bis: " + format(rechnung.getZahlungsziel()));
            }
        }
        if (rechnung != null && rechnung.istStornorechnung()) {
            schreiber.absatz(schreiber.normal, 10, "Storniert Rechnung: " + rechnung.getStornoZu());
        } else if (dokument.getVorgaengerNr() != null) {
            schreiber.absatz(schreiber.normal, 10, "Referenzbeleg: " + dokument.getVorgaengerNr());
        }
        schreiber.leer();
    }

    /**
     * Positionstabelle mit gemessenen Zahlenspalten; reicht der Platz für die
     * Leistungsbeschreibung nicht, werden die Positionen gestapelt. Der Kopf
     * wird je Folgeseite wiederholt, eine Position nicht über einen
     * Seitenwechsel zerrissen (F-03).
     */
    private void schreibePositionstabelle(Schreiber schreiber, Dokument dokument) throws IOException {
        String[] titel = {"Menge", "Einzelpreis", "USt", "Summe"};
        float[] breiten = new float[4];
        for (int i = 0; i < 4; i++) {
            breiten[i] = schreiber.breite(schreiber.fett, 10, titel[i]);
        }
        for (Dokumentposition position : dokument.getPositionen()) {
            String[] werte = zahlen(position);
            for (int i = 0; i < 4; i++) {
                breiten[i] = Math.max(breiten[i], schreiber.breite(schreiber.normal, 10, werte[i]));
            }
        }
        float[] kanten = new float[4];
        kanten[3] = RECHTS;
        for (int i = 2; i >= 0; i--) {
            kanten[i] = kanten[i + 1] - breiten[i + 1] - 8;
        }
        float textBreite = kanten[0] - breiten[0] - 8 - SPALTE_PRODUKT;
        boolean gestapelt = textBreite < 140;
        schreiber.folgekopf = () -> schreibeTabellenkopf(schreiber, titel, kanten, gestapelt);
        schreibeTabellenkopf(schreiber, titel, kanten, gestapelt);
        int pos = 1;
        for (Dokumentposition position : dokument.getPositionen()) {
            String beschreibung = beschreibung(position);
            String[] werte = zahlen(position);
            if (gestapelt) {
                List<String> absaetze = new ArrayList<>();
                absaetze.add(beschreibung);
                for (int i = 0; i < 4; i++) {
                    absaetze.add(titel[i] + ": " + werte[i]);
                }
                int zeilen = 1;
                for (String absatz : absaetze) {
                    zeilen += schreiber.umbreche(schreiber.normal, 10, absatz, SATZBREITE).size();
                }
                schreiber.halteZusammen(zeilen);
                schreiber.absatz(schreiber.fett, 10, "Position " + pos++);
                for (String absatz : absaetze) {
                    schreiber.absatz(schreiber.normal, 10, absatz);
                }
                schreiber.leer();
                continue;
            }
            List<String> bezeichnung = schreiber.umbreche(schreiber.normal, 10, beschreibung, textBreite);
            schreiber.halteZusammen(Math.max(1, bezeichnung.size()));
            schreiber.beginneZeile();
            schreiber.text(schreiber.normal, 10, String.valueOf(pos++), SPALTE_POS);
            schreiber.text(schreiber.normal, 10,
                    bezeichnung.isEmpty() ? "" : bezeichnung.get(0), SPALTE_PRODUKT);
            for (int i = 0; i < 4; i++) {
                schreiber.textRechts(schreiber.normal, 10, werte[i], kanten[i]);
            }
            schreiber.beendeZeile();
            for (String folgezeile : bezeichnung.subList(Math.min(1, bezeichnung.size()), bezeichnung.size())) {
                schreiber.beginneZeile();
                schreiber.text(schreiber.normal, 10, folgezeile, SPALTE_PRODUKT);
                schreiber.beendeZeile();
            }
        }
        schreiber.folgekopf = null;
        schreiber.linie();
    }

    /**
     * Produktnummer, Bezeichnung und Einheit, soweit vorhanden. Eine unbekannte
     * Einheit (Altbestand vor 3.1) entfällt, damit ein erneut gedrucktes PDF
     * eines ausgestellten Belegs keinen neuen Hinweistext trägt (GR-02).
     */
    private static String beschreibung(Dokumentposition position) {
        StringJoiner teile = new StringJoiner(" · ");
        for (String teil : new String[]{position.getProduktReferenz(), position.getBezeichnung(),
                position.getEinheit()}) {
            if (teil != null && !teil.isBlank()) {
                teile.add(teil.strip());
            }
        }
        return teile.toString();
    }

    private static String[] zahlen(Dokumentposition position) {
        return new String[]{String.valueOf(position.getMenge()), betrag(position.getEinzelpreisNetto()),
                prozent(position.getSteuersatz()) + " %", betrag(position.getPositionssummeNetto())};
    }

    private void schreibeTabellenkopf(Schreiber schreiber, String[] titel, float[] kanten,
                                      boolean gestapelt) throws IOException {
        schreiber.beginneZeile();
        schreiber.text(schreiber.fett, 10, gestapelt ? "Positionen" : "Pos", SPALTE_POS);
        if (!gestapelt) {
            schreiber.text(schreiber.fett, 10, "Leistung / Einheit", SPALTE_PRODUKT);
            for (int i = 0; i < 4; i++) {
                schreiber.textRechts(schreiber.fett, 10, titel[i], kanten[i]);
            }
        }
        schreiber.beendeZeile();
        schreiber.linie();
    }

    /**
     * Summen rechtsbündig unter der Tabelle (F-03): Entgelt, Steuerbetrag je
     * Steuersatz (A-F-25, § 14 Abs. 4 Nr. 7/8 UStG), Bruttosumme hervorgehoben.
     */
    private void schreibeSummenblock(Schreiber schreiber, Dokument dokument) throws IOException {
        summenzeile(schreiber, schreiber.normal, 10, "Summe netto:", dokument.getSummeNetto());
        if (dokument.getBetragssnapshot().berechnungsart() == Berechnungsart.UNGEKLAERT) {
            summenzeile(schreiber, schreiber.normal, 10, "Umsatzsteuer gesamt:", dokument.getSummeSteuer());
            schreiber.absatz(schreiber.normal, 10,
                    "Historischer Beleg: Aufschlüsselung nach Steuersätzen nicht verfügbar.");
        }
        for (Steuerzeile zeile : dokument.steueraufschluesselung()) {
            summenzeile(schreiber, schreiber.normal, 10, "Umsatzsteuer " + prozent(zeile.steuersatz())
                    + " % auf " + betrag(zeile.netto()) + " EUR:", zeile.steuer());
        }
        summenzeile(schreiber, schreiber.fett, 11, "Summe brutto:", dokument.getSummeBrutto());
        schreiber.leer();
    }

    private void summenzeile(Schreiber schreiber, PDFont font, float groesse,
                             String beschriftung, BigDecimal wert) throws IOException {
        String betragstext = betrag(wert) + " EUR";
        float wertBreite = schreiber.breite(font, groesse, betragstext);
        float labelBreite = SATZBREITE - wertBreite - 8;
        if (labelBreite < 140) {
            schreiber.absatz(font, groesse, beschriftung);
            schreiber.rechtsbuendig(font, groesse, betragstext, RECHTS);
        } else {
            List<String> zeilen = schreiber.umbreche(font, groesse, beschriftung, labelBreite);
            for (int i = 0; i < zeilen.size(); i++) {
                schreiber.beginneZeile();
                schreiber.textRechts(font, groesse, zeilen.get(i), RECHTS - wertBreite - 8);
                if (i == zeilen.size() - 1) { schreiber.textRechts(font, groesse, betragstext, RECHTS); }
                schreiber.beendeZeile();
            }
        }
    }

    /** Belegtyp-spezifischer Hinweistext am Ende des Dokuments. */
    private void schreibeSchlusstext(Schreiber schreiber, Dokument dokument, Firmenprofil firma)
            throws IOException {
        if (dokument instanceof Rechnung rechnung && rechnung.istStornorechnung()) {
            schreiber.absatz(schreiber.normal, 10, "Mit dieser Stornorechnung wird die Rechnung "
                    + rechnung.getStornoZu() + " vollständig storniert.");
        } else if (dokument instanceof Rechnung rechnung && rechnung.getZahlungsziel() != null
                && dokument.getStatus() != DokumentStatus.STORNIERT) {
            schreiber.absatz(schreiber.normal, 10, "Bitte überweisen Sie den Rechnungsbetrag bis zum "
                    + format(rechnung.getZahlungsziel()) + ".");
            if (istGesetzt(firma.iban())) {
                String bank = istGesetzt(firma.bank()) ? firma.bank() + " · " : "";
                String bic = istGesetzt(firma.bic()) ? " · BIC " + firma.bic() : "";
                schreiber.absatz(schreiber.normal, 10, "Bankverbindung: " + bank
                        + "IBAN " + firma.iban() + bic);
            }
        }
        if (dokument instanceof Angebot angebot && angebot.getGueltigBis() != null) {
            schreiber.absatz(schreiber.normal, 10, "Dieses Angebot ist gültig bis zum "
                    + format(angebot.getGueltigBis()) + ".");
        }
    }

    private static boolean istGesetzt(String wert) {
        return wert != null && !wert.isBlank();
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

    private static byte[] ladeSchrift(String pfad) {
        try (InputStream strom = PdfBoxPdfExporter.class.getResourceAsStream(pfad)) {
            if (strom == null) {
                throw new IllegalStateException("Schrift fehlt im Klassenpfad: " + pfad);
            }
            return strom.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Schrift konnte nicht geladen werden: " + pfad, e);
        }
    }

    /**
     * Zeilenweiser Schreiber mit automatischem Seitenumbruch. Absätze
     * entstehen über {@link #absatz} (mit Umbruch nach Breite);
     * Tabellenzeilen mit mehreren Spalten über {@link #beginneZeile},
     * {@link #text}/{@link #textRechts} und {@link #beendeZeile}.
     */
    private static final class Schreiber {

        private final PDDocument pdf;
        private final PDFont normal;
        private final PDFont fett;
        /** Ob eine Schrift ein Zeichen darstellen kann; je Schrift und Zeichen einmal geprüft. */
        private final Map<PDFont, Map<Integer, Boolean>> darstellbar = new HashMap<>();
        private PDPageContentStream inhalt;
        private float y;
        /** Tabellenkopf, den {@link #neueSeite()} auf Folgeseiten wiederholt; sonst {@code null}. */
        private Kopf folgekopf;

        @FunctionalInterface
        private interface Kopf {
            void schreibe() throws IOException;
        }

        Schreiber(PDDocument pdf, PDFont normal, PDFont fett) throws IOException {
            this.pdf = pdf;
            this.normal = normal;
            this.fett = fett;
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
            if (folgekopf != null) {
                folgekopf.schreibe();
            }
        }

        /**
         * Beginnt eine neue Seite, wenn die folgenden Zeilen nicht mehr auf die
         * aktuelle passen — aber nur, wenn sie auf einer leeren Seite Platz hätten.
         */
        void halteZusammen(int zeilen) throws IOException {
            boolean passtAufLeereSeite = PDRectangle.A4.getHeight() - RAND
                    - (zeilen - 1) * ZEILENHOEHE >= RAND + ZEILENHOEHE;
            if (!passtNoch(zeilen) && passtAufLeereSeite) {
                neueSeite();
            }
        }

        boolean passtNoch(int zeilen) {
            return y - (zeilen - 1) * ZEILENHOEHE >= RAND + ZEILENHOEHE;
        }

        /** Beginnt eine Tabellenzeile; bricht bei Bedarf auf eine neue Seite um. */
        void beginneZeile() throws IOException {
            if (!passtNoch(1)) {
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
            inhalt.showText(bereinige(font, text));
            inhalt.endText();
        }

        /** Rechtsbündiger Text: {@code xRechts} ist die rechte Kante der Spalte. */
        void textRechts(PDFont font, float groesse, String text, float xRechts) throws IOException {
            String sicher = bereinige(font, text);
            text(font, groesse, sicher, xRechts - breite(font, groesse, sicher));
        }

        /** Absatz am linken Rand, nach Breite auf die Satzspiegelbreite umbrochen. */
        void absatz(PDFont font, float groesse, String text) throws IOException {
            for (String zeile : umbreche(font, groesse, text, SATZBREITE)) {
                beginneZeile();
                text(font, groesse, zeile, RAND);
                beendeZeile();
            }
        }

        /** Einzelne rechtsbündige Zeile (z. B. Absenderblock). */
        void rechtsbuendig(PDFont font, float groesse, String text, float xRechts) throws IOException {
            for (String zeile : umbreche(font, groesse, text, xRechts - RAND)) {
                beginneZeile();
                textRechts(font, groesse, zeile, xRechts);
                beendeZeile();
            }
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

        /**
         * Bricht den Text an Wortgrenzen so um, dass jede Zeile höchstens
         * {@code maxBreite} breit ist; ein einzelnes überlanges Wort wird
         * zeichenweise geteilt. Kein Zeichen geht verloren (§ 14 Abs. 4 Nr. 5
         * UStG verlangt die vollständige Bezeichnung der Leistung).
         */
        List<String> umbreche(PDFont font, float groesse, String text, float maxBreite)
                throws IOException {
            String sicher = bereinige(font, text);
            List<String> zeilen = new ArrayList<>();
            if (sicher.isBlank()) {
                return zeilen;
            }
            StringBuilder zeile = new StringBuilder();
            for (String wort : sicher.split(" ")) {
                String kandidat = zeile.isEmpty() ? wort : zeile + " " + wort;
                if (breite(font, groesse, kandidat) <= maxBreite) {
                    zeile.setLength(0);
                    zeile.append(kandidat);
                    continue;
                }
                if (!zeile.isEmpty()) {
                    zeilen.add(zeile.toString());
                    zeile.setLength(0);
                }
                String rest = wort;
                while (breite(font, groesse, rest) > maxBreite && rest.length() > 1) {
                    int teil = rest.length() - 1;
                    while (teil > 1 && breite(font, groesse, rest.substring(0, teil)) > maxBreite) {
                        teil--;
                    }
                    zeilen.add(rest.substring(0, teil));
                    rest = rest.substring(teil);
                }
                zeile.append(rest);
            }
            if (!zeile.isEmpty()) {
                zeilen.add(zeile.toString());
            }
            return zeilen;
        }

        private static float breite(PDFont font, float groesse, String text) throws IOException {
            return font.getStringWidth(text) / 1000 * groesse;
        }

        /**
         * Ersetzt Steuerzeichen (Tabulator, Zeilenumbruch) durch Leerzeichen
         * und Zeichen ohne Glyphe in der Schrift durch "?".
         */
        private String bereinige(PDFont font, String text) {
            if (text == null) {
                return "";
            }
            StringBuilder ergebnis = new StringBuilder(text.length());
            text.codePoints().forEach(zeichen -> {
                if (Character.isISOControl(zeichen)) {
                    ergebnis.append(' ');
                } else if (kannDarstellen(font, zeichen)) {
                    ergebnis.appendCodePoint(zeichen);
                } else {
                    ergebnis.append('?');
                }
            });
            return ergebnis.toString();
        }

        private boolean kannDarstellen(PDFont font, int zeichen) {
            return darstellbar.computeIfAbsent(font, f -> new HashMap<>())
                    .computeIfAbsent(zeichen, z -> {
                        try {
                            font.encode(new String(Character.toChars(z)));
                            return true;
                        } catch (IllegalArgumentException | IOException e) {
                            return false;
                        }
                    });
        }
    }
}
