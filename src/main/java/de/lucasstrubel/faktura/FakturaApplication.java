package de.lucasstrubel.faktura;

import de.lucasstrubel.faktura.dokumente.DokumentCsvExport;
import de.lucasstrubel.faktura.dokumente.DokumentService;
import de.lucasstrubel.faktura.gemeinsam.EreignisBus;
import de.lucasstrubel.faktura.gui.DokumentListenPanel;
import de.lucasstrubel.faktura.gui.HauptFenster;
import de.lucasstrubel.faktura.gui.KundenPanel;
import de.lucasstrubel.faktura.gui.ProduktPanel;
import de.lucasstrubel.faktura.gui.StammdatenController;
import de.lucasstrubel.faktura.kunden.KundenCsvExport;
import de.lucasstrubel.faktura.kunden.KundenVerwaltungsService;
import de.lucasstrubel.faktura.produkte.ProduktCsvExport;
import de.lucasstrubel.faktura.produkte.ProduktVerwaltungsService;

import com.formdev.flatlaf.FlatLightLaf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Einstiegspunkt der Desktop-Fakturierungsanwendung: Der Spring-IoC-Container
 * verdrahtet die vier Komponenten (A: Dokumentenzyklus, B: Produkte, C: Kunden,
 * D: Oberfläche); anschließend wird die Swing-Oberfläche auf dem
 * Event-Dispatch-Thread aus den Beans aufgebaut. Alle Daten liegen
 * ausschließlich lokal im konfigurierten Datenverzeichnis (Q-06, IF-01).
 */
@SpringBootApplication
public class FakturaApplication {

    private static final Logger LOG = LoggerFactory.getLogger(FakturaApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext kontext = new SpringApplicationBuilder(FakturaApplication.class)
                .headless(false)
                .run(args);
        LOG.info("Faktura startet; Datenverzeichnis: {}",
                kontext.getBean(FakturaEigenschaften.class).datenVerzeichnis().toAbsolutePath());
        SwingUtilities.invokeLater(() -> starteOberflaeche(kontext));
    }

    /** Baut die Programmoberfläche (Komponente D) aus den Beans des Containers auf. */
    private static void starteOberflaeche(ConfigurableApplicationContext kontext) {
        try {
            FlatLightLaf.setup();
            UIManager.put("Table.alternateRowColor", new java.awt.Color(245, 246, 248));
            UIManager.put("Table.showHorizontalLines", false);
        } catch (Exception e) {
            LOG.warn("FlatLaf konnte nicht initialisiert werden, Standard-Look-and-Feel wird verwendet", e);
        }

        KundenVerwaltungsService kundenService = kontext.getBean(KundenVerwaltungsService.class);
        ProduktVerwaltungsService produktService = kontext.getBean(ProduktVerwaltungsService.class);
        StammdatenController stammdatenController = kontext.getBean(StammdatenController.class);
        EreignisBus ereignisBus = kontext.getBean(EreignisBus.class);

        HauptFenster fenster = new HauptFenster(
                new KundenPanel(kundenService, stammdatenController,
                        kontext.getBean(KundenCsvExport.class), ereignisBus),
                new ProduktPanel(produktService, stammdatenController,
                        kontext.getBean(ProduktCsvExport.class), ereignisBus),
                new DokumentListenPanel(kontext.getBean(DokumentService.class), kundenService,
                        produktService, kontext.getBean(DokumentCsvExport.class), ereignisBus));
        fenster.setVisible(true);
        LOG.info("Faktura ist bedienbereit (Q-04)");
    }
}
