package de.lucasstrubel.faktura;

import de.lucasstrubel.faktura.dokumente.DokumentCsvExport;
import de.lucasstrubel.faktura.dokumente.DokumentReferenzPruefung;
import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.dokumente.EinfacherBelegnummernGenerator;
import de.lucasstrubel.faktura.dokumente.JsonDokumentRepository;
import de.lucasstrubel.faktura.dokumente.PdfBoxPdfExporter;
import de.lucasstrubel.faktura.dokumente.StandardDokumentService;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.gui.DokumentListenPanel;
import de.lucasstrubel.faktura.gui.HauptFenster;
import de.lucasstrubel.faktura.gui.KundenPanel;
import de.lucasstrubel.faktura.gui.ProduktPanel;
import de.lucasstrubel.faktura.gui.StammdatenController;
import de.lucasstrubel.faktura.kunden.EinfacherKundennummernGenerator;
import de.lucasstrubel.faktura.kunden.JsonKundenRepository;
import de.lucasstrubel.faktura.kunden.KundenCsvExport;
import de.lucasstrubel.faktura.kunden.KundenVerwaltungsService;
import de.lucasstrubel.faktura.produkte.EinfacherProduktnummernGenerator;
import de.lucasstrubel.faktura.produkte.JsonProduktRepository;
import de.lucasstrubel.faktura.produkte.ProduktCsvExport;
import de.lucasstrubel.faktura.produkte.ProduktVerwaltungsService;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;

/**
 * Einstiegspunkt der Desktop-Fakturierungsanwendung: verdrahtet die vier
 * Komponenten (A: Dokumentenzyklus, B: Produkte, C: Kunden, D: Oberfläche)
 * und startet die GUI. Alle Daten liegen ausschließlich lokal im
 * Verzeichnis {@code daten/} (Q-06, IF-01).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path datenVerzeichnis = Path.of("daten");

        // Persistenz (IF-01)
        JsonKundenRepository kundenRepository =
                new JsonKundenRepository(datenVerzeichnis.resolve("kunden.json"));
        JsonProduktRepository produktRepository =
                new JsonProduktRepository(datenVerzeichnis.resolve("produkte.json"));
        JsonDokumentRepository dokumentRepository =
                new JsonDokumentRepository(datenVerzeichnis.resolve("dokumente.json"));

        // Gruppe A stellt die Referenzprüfungen für die Löschsperren bereit
        DokumentReferenzPruefung referenzPruefung = new DokumentReferenzPruefung(dokumentRepository);

        // Observer-Verteiler: Services melden Datenänderungen, Panels abonnieren
        EreignisBus ereignisBus = new EreignisBus();

        // Gruppe C — Kundenverwaltung
        KundenVerwaltungsService kundenService = new KundenVerwaltungsService(
                kundenRepository,
                EinfacherKundennummernGenerator.ausRepository(kundenRepository),
                referenzPruefung,
                ereignisBus);

        // Gruppe B — Produktverwaltung
        ProduktVerwaltungsService produktService = new ProduktVerwaltungsService(
                produktRepository,
                EinfacherProduktnummernGenerator.ausRepository(produktRepository),
                referenzPruefung,
                ereignisBus);

        // Gruppe A — Dokumentenzyklus
        DokumentService dokumentService = new StandardDokumentService(
                dokumentRepository,
                EinfacherBelegnummernGenerator.ausRepository(dokumentRepository),
                kundenService,
                produktService,
                new PdfBoxPdfExporter(),
                ereignisBus);

        // Gruppe D — GUI-freier Controller der Stammdaten-Ansichten (D-F-03)
        StammdatenController stammdatenController =
                new StammdatenController(kundenService, produktService);

        // Gruppe D — Programmoberfläche
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.put("Table.alternateRowColor", new java.awt.Color(245, 246, 248));
                UIManager.put("Table.showHorizontalLines", false);
            } catch (Exception e) {
                // Standard-Look-and-Feel verwenden
            }
            HauptFenster fenster = new HauptFenster(
                    new KundenPanel(kundenService, stammdatenController,
                            new KundenCsvExport(kundenRepository), ereignisBus),
                    new ProduktPanel(produktService, stammdatenController,
                            new ProduktCsvExport(produktRepository), ereignisBus),
                    new DokumentListenPanel(dokumentService, kundenService, produktService,
                            new DokumentCsvExport(dokumentRepository), ereignisBus));
            fenster.setVisible(true);
        });
    }
}
