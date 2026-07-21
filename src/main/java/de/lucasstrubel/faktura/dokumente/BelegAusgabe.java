package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.gemeinsam.AusgabeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Ausgabe eines Belegs an die Außenwelt: PDF in eine gewählte Datei (F-04,
 * F-07, F-10, F-15), Druck über das Betriebssystem (IF-02) und Versand über
 * das Standard-Mailprogramm (IF-03).
 *
 * <p>Zuvor lag dieser Code in der Dokumentansicht der Oberfläche. Dort
 * vermischte er Ansichtsverdrahtung mit Datei- und Betriebssystemzugriff, fing
 * Ausnahmen pauschal ab — und legte die erzeugten Zwischen-PDFs dauerhaft im
 * Temp-Verzeichnis des Systems ab: Rechnungen mit Namen und Anschrift der
 * Kunden blieben dort nach dem Beenden liegen. Diese Klasse hält die
 * Zwischendateien stattdessen in einem eigenen Sitzungsverzeichnis, das beim
 * Herunterfahren restlos gelöscht wird (Q-06, DSGVO).
 *
 * <p>Frei von JavaFX und damit ohne laufende Oberfläche testbar.
 */
@Component
public class BelegAusgabe implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BelegAusgabe.class);

    private final DokumentService dokumentService;

    /** Träge angelegt: Wer nie druckt oder mailt, bekommt kein Temp-Verzeichnis. */
    private Path sitzungsverzeichnis;

    public BelegAusgabe(DokumentService dokumentService) {
        this.dokumentService = dokumentService;
    }

    /** Schreibt das Beleg-PDF in die gewählte Zieldatei. */
    public void exportierePdf(String belegnummer, Path zielDatei) {
        dokumentService.exportierePdf(belegnummer, zielDatei);
        LOG.info("PDF des Belegs {} exportiert nach {}", belegnummer, zielDatei);
    }

    /** Druckt den Beleg über das Standardprogramm des Betriebssystems (IF-02). */
    public void drucke(String belegnummer) {
        pruefeUnterstuetzt(Desktop.Action.PRINT,
                "Auf diesem System ist kein Druckdienst verfügbar.");
        Path pdf = zwischendatei(belegnummer);
        try {
            Desktop.getDesktop().print(pdf.toFile());
            LOG.info("Beleg {} an den Druckdienst übergeben", belegnummer);
        } catch (IOException e) {
            throw new AusgabeException(
                    "Der Beleg konnte nicht gedruckt werden: " + e.getMessage(), e);
        }
    }

    /**
     * Öffnet das Standard-Mailprogramm mit vorbereitetem Betreff und einem
     * Hinweis auf das erzeugte PDF (IF-03). Das PDF automatisch anzuhängen ist
     * über {@code mailto:} nicht möglich — der Pfad steht deshalb im Text.
     */
    public void sendePerMail(String belegnummer) {
        pruefeUnterstuetzt(Desktop.Action.MAIL,
                "Auf diesem System ist kein Standard-Mailprogramm eingerichtet.");
        Dokument dokument = pruefeBeleg(belegnummer);
        Path pdf = zwischendatei(belegnummer);
        String betreff = kodiere(dokument.belegtyp().anzeigename() + " " + belegnummer);
        String text = kodiere("Bitte das exportierte PDF anhängen:\n" + pdf);
        try {
            Desktop.getDesktop().mail(new URI("mailto:?subject=" + betreff + "&body=" + text));
            LOG.info("Mailprogramm für Beleg {} geöffnet", belegnummer);
        } catch (IOException | URISyntaxException e) {
            throw new AusgabeException(
                    "Das Mailprogramm konnte nicht geöffnet werden: " + e.getMessage(), e);
        }
    }

    /** Erzeugt das PDF im Sitzungsverzeichnis und liefert seinen Pfad. */
    private Path zwischendatei(String belegnummer) {
        try {
            Path ziel = sitzungsverzeichnis().resolve(belegnummer + ".pdf");
            dokumentService.exportierePdf(belegnummer, ziel);
            return ziel;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Das Zwischen-PDF konnte nicht erzeugt werden: " + belegnummer, e);
        }
    }

    private Path sitzungsverzeichnis() throws IOException {
        if (sitzungsverzeichnis == null) {
            sitzungsverzeichnis = Files.createTempDirectory("faktura-");
            LOG.debug("Sitzungsverzeichnis für Zwischendateien angelegt: {}", sitzungsverzeichnis);
        }
        return sitzungsverzeichnis;
    }

    private Dokument pruefeBeleg(String belegnummer) {
        return dokumentService.alleDokumente().stream()
                .filter(dokument -> dokument.getBelegnummer().equals(belegnummer))
                .findFirst()
                .orElseThrow(() -> new AusgabeException(
                        "Der Beleg " + belegnummer + " existiert nicht."));
    }

    private static void pruefeUnterstuetzt(Desktop.Action aktion, String meldung) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(aktion)) {
            throw new AusgabeException(meldung);
        }
    }

    /** {@code mailto:} erwartet Prozent-Kodierung; {@code +} gilt dort nicht als Leerzeichen. */
    private static String kodiere(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Löscht das Sitzungsverzeichnis samt aller Zwischen-PDFs. Wird vom
     * Spring-Container beim Herunterfahren aufgerufen.
     */
    @Override
    public void close() {
        if (sitzungsverzeichnis == null || !Files.exists(sitzungsverzeichnis)) {
            return;
        }
        try (Stream<Path> inhalt = Files.walk(sitzungsverzeichnis)) {
            // Tiefste Einträge zuerst, damit die Verzeichnisse leer sind
            for (Path eintrag : inhalt.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(eintrag);
            }
            LOG.debug("Sitzungsverzeichnis gelöscht: {}", sitzungsverzeichnis);
        } catch (IOException e) {
            // Kein Grund, das Beenden scheitern zu lassen — aber es muss auffallen
            LOG.warn("Sitzungsverzeichnis {} konnte nicht vollständig gelöscht werden",
                    sitzungsverzeichnis, e);
        }
    }
}
