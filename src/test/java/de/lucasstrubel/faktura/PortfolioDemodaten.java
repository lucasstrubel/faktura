package de.lucasstrubel.faktura;

import de.lucasstrubel.faktura.dokumente.*;
import de.lucasstrubel.faktura.firma.*;
import de.lucasstrubel.faktura.gemeinsam.*;
import de.lucasstrubel.faktura.kunden.*;
import de.lucasstrubel.faktura.produkte.*;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

/** PORT-01: Reproduzierbarer, ausschließlich fiktiver Demobestand über produktive Dienste. */
public final class PortfolioDemodaten {
    private PortfolioDemodaten() { }

    public static void main(String[] argumente) throws Exception {
        Path basis = Path.of(argumente[0]).toAbsolutePath();
        // Ein vorhandenes Ziel wird auch dann abgelehnt, wenn es leer erscheint.
        Files.createDirectory(basis);
        Path daten = basis.resolve("daten");
        System.setProperty("faktura.daten-verzeichnis", daten.toString());
        try (var kontext = new SpringApplicationBuilder(FakturaApplication.class).headless(true)
                .run("--faktura.daten-verzeichnis=" + daten, "--spring.main.banner-mode=off")) {
            kontext.getBean(FirmenprofilService.class).speichere(new Firmenprofil(
                    "Demowerkstatt - FIKTIVE DATEN", "Musterweg 1", "68163", "Mannheim",
                    "DE123456789", "37/123/45678", null, "demo@example.invalid",
                    "DE02 1203 0000 0000 2020 51", "BYLADEM1001", "Demobank"));
            Kunde kunde = kontext.getBean(KundenVerwaltungsService.class).legeAn(
                    new Kunde("Beispielstudio - FIKTIV", "Beispielweg 2", "68163", "Mannheim"));
            var produkte = kontext.getBean(ProduktVerwaltungsService.class);
            Produkt beratung = new Produkt("Beratung", new BigDecimal("80.00"), new BigDecimal("0.19"));
            beratung.setEinheit("Stunde"); produkte.legeAn(beratung);
            Produkt handbuch = new Produkt("Dokumentation", new BigDecimal("25.00"), new BigDecimal("0.19"));
            handbuch.setEinheit("Stück"); produkte.legeAn(handbuch);
            var dienst = kontext.getBean(DokumentService.class);
            Dokument beleg = dienst.erstelleAngebot(kunde.getKundennummer(), List.of(
                    new Positionsangabe(beratung.getProduktnummer(), 2),
                    new Positionsangabe(handbuch.getProduktnummer(), 1)), null);
            for (int i = 0; i < 3; i++) { beleg = dienst.erzeugeFolgebeleg(beleg.getBelegnummer()); }
            dienst.versende(beleg.getBelegnummer());
            String nummer = beleg.getBelegnummer();
            beratung.setEinzelpreisNetto(new BigDecimal("95.00")); produkte.aendere(beratung);
            Rechnung rechnung = (Rechnung) dienst.alleDokumente().stream()
                    .filter(d -> nummer.equals(d.getBelegnummer())).findFirst().orElseThrow();
            if (rechnung.getSummeBrutto().compareTo(new BigDecimal("220.15")) != 0
                    || rechnung.getPositionen().getFirst().getEinzelpreisNetto().compareTo(new BigDecimal("80.00")) != 0) {
                throw new AssertionError("Stammdatenänderung hat den Rechnungssnapshot verändert");
            }
            beratung.setEinzelpreisNetto(new BigDecimal("80.00")); produkte.aendere(beratung);
            Rechnung bezahlt = dienst.erstelleRechnung(kunde.getKundennummer(), List.of(
                    new Positionsangabe(handbuch.getProduktnummer(), 1)), LocalDate.now(), null);
            dienst.markiereBezahlt(bezahlt.getBelegnummer(), LocalDate.now());
            dienst.exportierePdf(nummer, basis.resolve("beispielrechnung.pdf"));
            kontext.getBean(ERechnungExport.class).exportiereXml(rechnung, basis.resolve("beispielrechnung.xml"));
            kontext.getBean(GesamtExport.class).exportiere(basis.resolve("gesamtexport.zip"));
            kontext.getBean(Datensicherung.class).erstelle(daten, basis.resolve("sicherung.zip"));
            Files.writeString(basis.resolve("DEMOSTAND.txt"), "Nur fiktive Daten. PORT-01\n"
                    + "Rechnung " + nummer + ": 185,00 netto / 35,15 Steuer / 220,15 brutto\n"
                    + "Beratung: 80,00 EUR/Stunde; Dokumentation: 25,00 EUR/Stück\n"
                    + "Eine weitere Rechnung über 29,75 EUR ist bezahlt.\n");
        }
        System.out.println("FAKTURA_DEMO_READY: " + basis);
    }
}
