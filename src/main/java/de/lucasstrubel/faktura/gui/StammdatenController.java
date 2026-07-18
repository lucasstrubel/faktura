package de.lucasstrubel.faktura.gui;

import de.lucasstrubel.faktura.kunden.Kunde;
import de.lucasstrubel.faktura.kunden.KundenService;
import de.lucasstrubel.faktura.produkte.Produkt;
import de.lucasstrubel.faktura.produkte.ProduktService;

import java.util.List;

/**
 * Controller der Stammdaten-Ansichten (D-F-03): delegiert Suchanfragen an
 * die Dienste der Komponenten B und C; die GUI rechnet und filtert selbst nicht.
 * GUI-frei und damit im Modultest ohne Oberfläche prüfbar (TC-14, TC-15).
 */
public class StammdatenController {

    private final KundenService kundenService;
    private final ProduktService produktService;

    public StammdatenController(KundenService kundenService, ProduktService produktService) {
        this.kundenService = kundenService;
        this.produktService = produktService;
    }

    /** Volltextsuche über Name oder Kundennummer (C-F-12). */
    public List<Kunde> sucheKunden(String suchbegriff) {
        return kundenService.suche(suchbegriff);
    }

    /** Volltextsuche über Bezeichnung oder Produktnummer (B-F-12). */
    public List<Produkt> sucheProdukte(String suchbegriff) {
        return produktService.suche(suchbegriff);
    }

    /**
     * Listeninhalt der Kunden-Modulansicht (D-F-03): ein leerer oder fehlender
     * Suchbegriff zeigt den gesamten Bestand; die Sortierung nach Name kommt
     * aus der Fachkomponente (C-F-11).
     */
    public List<Kunde> kundenListe(String suchbegriff) {
        return sucheKunden(suchbegriff == null ? "" : suchbegriff.trim());
    }

    /**
     * Listeninhalt der Produkt-Modulansicht (D-F-03): ein leerer oder fehlender
     * Suchbegriff zeigt den gesamten Bestand; die Sortierung nach Bezeichnung
     * kommt aus der Fachkomponente (B-F-11).
     */
    public List<Produkt> produkteListe(String suchbegriff) {
        return sucheProdukte(suchbegriff == null ? "" : suchbegriff.trim());
    }
}
