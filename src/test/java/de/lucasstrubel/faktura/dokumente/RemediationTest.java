package de.lucasstrubel.faktura.dokumente;

import de.lucasstrubel.faktura.FakturaApplication;
import de.lucasstrubel.faktura.gemeinsam.*;
import de.lucasstrubel.faktura.gui.Datumseingabe;
import de.lucasstrubel.faktura.gui.TabellenFormat;
import de.lucasstrubel.faktura.produkte.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class RemediationTest {
    @TempDir Path ordner;

    private ConfigurableApplicationContext kontext(Path pfad) {
        return new SpringApplicationBuilder(FakturaApplication.class).headless(true)
                .run("--faktura.daten-verzeichnis=" + pfad, "--spring.main.banner-mode=off");
    }

    private Rechnung rechnung() {
        Rechnung r = TestBelege.rechnung("R-2026-000001", DokumentStatus.ENTWURF);
        r.setDatum(LocalDate.of(2026, 10, 7));
        r.setzeKunde("K-000001", "Muster", "Hauptstraße 1, 68163 Mannheim");
        r.setLeistungsdatum(r.getDatum());
        r.setZahlungsziel(r.getDatum().plusDays(14));
        r.setzePositionen(List.of(new Dokumentposition("P-000001", "Teil", 1,
                new BigDecimal("0.03"), new BigDecimal("0.19"), "Stück", "C62"),
                new Dokumentposition("P-000001", "Teil", 1,
                new BigDecimal("0.03"), new BigDecimal("0.19"), "Stück", "C62")));
        return r;
    }

    @Test @DisplayName("REM-01: Preisformat über SQLite-Neuanlage, Neustart und Fremdfeldänderung stabil")
    void preise() {
        Map<String, BigDecimal> erwartet = new LinkedHashMap<>();
        try (var k = kontext(ordner)) {
            var service = k.getBean(ProduktVerwaltungsService.class);
            for (String text : List.of("1,000", "12,500", "12,50", "1.234,56", "1234.56", "12500")) {
                Produkt p = new Produkt(); p.setBezeichnung(text);
                p.setEinzelpreisNetto(TabellenFormat.parseBetrag(text)); p.setSteuersatz(new BigDecimal("0.19"));
                p.setEinheit("Stk."); service.legeAn(p);
                erwartet.put(p.getProduktnummer(), p.getEinzelpreisNetto());
                assertEquals(2, p.getEinzelpreisNetto().scale());
            }
        }
        try (var k = kontext(ordner)) {
            var service = k.getBean(ProduktVerwaltungsService.class);
            erwartet.forEach((nr, wert) -> {
                Produkt p = service.findeProdukt(nr);
                p.setEinzelpreisNetto(TabellenFormat.parseBetrag(TabellenFormat.betragFuerEingabe(p.getEinzelpreisNetto())));
                p.setBeschreibung("Nur Beschreibung geändert"); service.aendere(p);
                assertEquals(wert, service.findeProdukt(nr).getEinzelpreisNetto());
                assertEquals("Stück", p.getEinheit());
            });
            Produkt falsch = service.findeProdukt(erwartet.keySet().iterator().next());
            falsch.setEinzelpreisNetto(new BigDecimal("1.001"));
            assertThrows(ValidierungsException.class, () -> service.aendere(falsch));
        }
    }

    @Test @DisplayName("REM-02: Kalenderstrenge Kurzformen und Schaltjahre")
    void datum() {
        for (String text : List.of("31.2.2026", "29.2.2025", "31.4.26", "1.13.26", "0.1.26", "abc")) {
            assertEquals("Leistungsdatum", assertThrows(ValidierungsException.class,
                    () -> Datumseingabe.parse(text, "Leistungsdatum")).getFeldname());
        }
        assertEquals(LocalDate.of(2024, 2, 29), Datumseingabe.parse("29.2.24", "Datum"));
        assertEquals(LocalDate.of(2000, 1, 1), Datumseingabe.parse("1.1.00", "Datum"));
        assertEquals(LocalDate.of(2099, 1, 1), Datumseingabe.parse("1.1.99", "Datum"));
        assertNull(Datumseingabe.parse(" ", "Datum")); assertNull(Datumseingabe.parse(null, "Datum"));
        assertEquals("12,50", TabellenFormat.betragFuerEingabe(new BigDecimal("12.500")));
    }

    @Test @DisplayName("REM-03: Historische 0,08 EUR über Status, Storno, PDF und Neustart erhalten")
    void historisch() throws Exception {
        String stornoNr;
        try (var k = kontext(ordner)) {
            Rechnung r = rechnung();
            r.stelleSummenWiederHer(new BigDecimal("0.06"), new BigDecimal("0.02"), new BigDecimal("0.08"));
            r.setzeStatus(DokumentStatus.OFFEN);
            r.versende();
            var repo = k.getBean(DokumentRepository.class); repo.speichere(r);
            assertEquals(Berechnungsart.ALTE_POSITIONSRUNDUNG, r.getBetragssnapshot().berechnungsart());
            Path xml = ordner.resolve("rechnung.xml"); Files.writeString(xml, "vorher");
            assertThrows(ValidierungsException.class, () -> k.getBean(ERechnungExport.class).exportiereXml(r, xml));
            assertEquals("vorher", Files.readString(xml));
            Path pdf = ordner.resolve("rechnung.pdf"); k.getBean(PdfExporter.class).exportiere(r, pdf);
            try (var dokument = Loader.loadPDF(pdf.toFile())) {
                assertTrue(new PDFTextStripper().getText(dokument).contains("0,08 EUR"));
            }
            Rechnung storno = k.getBean(DokumentService.class).storniere(r.getBelegnummer());
            stornoNr = storno.getBelegnummer();
            assertEquals(new BigDecimal("-0.08"), storno.getSummeBrutto());
            assertThrows(IllegalStateException.class, storno::berechneSummen);
        }
        try (var k = kontext(ordner)) {
            var repo = k.getBean(DokumentRepository.class);
            assertEquals(new BigDecimal("0.08"), repo.findeNachNummer("R-2026-000001").getSummeBrutto());
            Dokument storno = repo.findeNachNummer(stornoNr);
            assertEquals(new BigDecimal("-0.08"), storno.getSummeBrutto());
            assertEquals(DokumentStatus.OFFEN, storno.getStatus());
            assertEquals("C62", storno.getPositionen().get(0).getEinheitCode());
            assertThrows(IllegalStateException.class, repo.findeNachNummer("R-2026-000001")::berechneSummen);
        }
    }

    @Test @DisplayName("REM-04: V4-Upgrade sichert und erhält Summen und Nummernkreise")
    void migration() throws Exception {
        SQLiteDataSource quelle = new SQLiteDataSource(); quelle.setUrl("jdbc:sqlite:" + ordner.resolve("faktura.db"));
        Flyway.configure().dataSource(quelle).target("4").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(quelle);
        jdbc.update("INSERT INTO dokument(belegnummer,typ,datum,kunden_referenz,status,summe_netto,summe_steuer,summe_brutto) VALUES ('R-2026-000001','RECHNUNG','2026-10-07','K-000001','VERSENDET','0.06','0.02','0.08')");
        for (int i = 1; i <= 2; i++) {
            jdbc.update("INSERT INTO dokumentposition(belegnummer,position,menge,einzelpreis_netto,steuersatz) VALUES ('R-2026-000001',?,1,'0.03','0.19')", i);
        }
        jdbc.update("INSERT INTO nummernkreis(bereich,jahr,letzter_wert) VALUES ('RECHNUNG',2026,77)");
        jdbc.update("INSERT INTO dokument(belegnummer,typ,datum,kunden_referenz,status,summe_netto,summe_steuer,summe_brutto) VALUES ('R-2026-000002','RECHNUNG','2026-10-07','K-000001','OFFEN','','','')");
        try (var k = kontext(ordner)) {
            Dokument r = k.getBean(DokumentRepository.class).findeNachNummer("R-2026-000001");
            assertEquals(new BigDecimal("0.08"), r.getSummeBrutto());
            assertEquals(Berechnungsart.ALTE_POSITIONSRUNDUNG, r.getBetragssnapshot().berechnungsart());
            assertNull(r.getPositionen().get(0).getEinheit());
            assertEquals(77, jdbc.queryForObject("SELECT letzter_wert FROM nummernkreis", Integer.class));
            Dokument unbekannt = k.getBean(DokumentRepository.class).findeNachNummer("R-2026-000002");
            assertNull(unbekannt.getSummeBrutto());
            assertEquals(Berechnungsart.UNGEKLAERT, unbekannt.getBetragssnapshot().berechnungsart());
        }
        try (var dateien = Files.list(ordner.resolve("sicherungen"))) {
            assertEquals(1, dateien.count());
        }
        try (var k = kontext(ordner)) { assertNotNull(k.getBean(DokumentRepository.class)); }
    }

    @Test @DisplayName("REM-05: Unbekannte Beträge und Positionen bleiben unbekannt, Einheiten sind Snapshots")
    void ungeklaertUndEinheiten() {
        for (String[] paar : List.of(new String[]{"Stk.", "C62"}, new String[]{"Std.", "HUR"},
                new String[]{"Tag", "DAY"}, new String[]{"kg", "KGM"}, new String[]{"g", "GRM"},
                new String[]{"m", "MTR"}, new String[]{"l", "LTR"})) {
            assertEquals(paar[1], Einheiten.code(paar[0]));
        }
        assertEquals("LS", Einheiten.code("Pauschale")); assertEquals("MON", Einheiten.code("Monat"));
        assertNull(Einheiten.code("Paket")); assertNull(Einheiten.code(null));
        assertEquals("Pauschale", Einheiten.normalisiere(" pauschal "));
        assertEquals("Paket", Einheiten.normalisiere(" Paket "));
        Rechnung r = rechnung(); r.stelleBetragssnapshotWiederHer(Betragssnapshot.ausAltbestand(r.getPositionen(), null, null, null));
        assertEquals(Berechnungsart.UNGEKLAERT, r.getBetragssnapshot().berechnungsart());
        assertThrows(IllegalStateException.class, () -> r.getBetragssnapshot().negiert());
        try (var k = kontext(ordner)) {
            var repo = k.getBean(DokumentRepository.class); repo.speichere(r);
            assertNull(repo.findeNachNummer(r.getBelegnummer()).getSummeBrutto());
            assertTrue(repo.istProduktReferenziert("P-000001")); assertFalse(repo.istProduktReferenziert("unbekannt"));
            assertEquals(1, repo.anzahlFuerKunde("K-000001"));
        }
        var position = Dokumentposition.wiederhergestellt("P", "Alt", 2, new BigDecimal("1.234"), null, "alt", null, new BigDecimal("2.47"));
        assertEquals(new BigDecimal("-2.47"), position.negiert().getPositionssummeNetto());
        assertEquals(new BigDecimal("1.234"), position.negiert().getEinzelpreisNetto());
        assertNull(position.getSteuerbetrag());
        assertNull(position.getPositionssummeBrutto());
        assertNull(Betragssnapshot.berechne(List.of(position)).brutto());
        assertEquals(new BigDecimal("2.47"), Betragssnapshot.berechne(List.of(position)).netto());
    }

    @Test @DisplayName("REM-06: Gesamt-CSV, Backup, Wiederherstellung und fehlgeschlagener Austausch")
    void exportUndSicherung() throws Exception {
        Path zip = ordner.resolve("daten.zip"), backup = ordner.resolve("backup.zip");
        try (var k = kontext(ordner)) {
            k.getBean(DokumentRepository.class).speichere(rechnung());
            k.getBean(GesamtExport.class).exportiere(zip);
            Path steuern = ordner.resolve("steuern.csv"), profil = ordner.resolve("firmenprofil.csv");
            k.getBean(GesamtExport.class).exportiereSteuern(steuern);
            k.getBean(GesamtExport.class).exportiereFirmenprofil(profil);
            try (ZipFile archiv = new ZipFile(zip.toFile())) {
                assertEquals(7, archiv.size());
                assertArrayEquals(archiv.getInputStream(archiv.getEntry("steuern.csv")).readAllBytes(), Files.readAllBytes(steuern));
                assertArrayEquals(archiv.getInputStream(archiv.getEntry("firmenprofil.csv")).readAllBytes(), Files.readAllBytes(profil));
                String csv = new String(archiv.getInputStream(archiv.getEntry("dokumente.csv")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(csv.startsWith("\uFEFF")); assertTrue(csv.contains("Faktura Software")); assertTrue(csv.contains("JE_STEUERSATZ"));
                String format = new String(archiv.getInputStream(archiv.getEntry("FORMAT.txt")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                // Die Version stammt aus dem Build (faktura-version.properties), nicht aus einem festen Text
                String kopf = format.lines().findFirst().orElse("");
                assertTrue(kopf.matches("Faktura Gesamt-Export, Formatversion 1, Anwendung \\d+\\.\\d+\\.\\d+.*"), kopf);
            }
            k.getBean(Datensicherung.class).erstelle(ordner, backup);
            byte[] vorher = Files.readAllBytes(backup);
            // Eine als JSON benannte Verzeichnisquelle provoziert den Fehler nach dem Öffnen des temporären ZIP.
            Path defekt = Files.createDirectory(ordner.resolve("defekt.json"));
            assertThrows(RuntimeException.class, () -> k.getBean(Datensicherung.class).erstelle(ordner, backup));
            assertArrayEquals(vorher, Files.readAllBytes(backup)); Files.delete(defekt);
            assertThrows(RuntimeException.class, () -> k.getBean(Datensicherung.class).erstelle(ordner, ordner.resolve("faktura.db")));
            assertThrows(java.io.IOException.class, () -> AtomareDatei.schreibe(zip, p -> { throw new java.io.IOException("Test"); }));
        }
        Path restauriert = Files.createDirectory(ordner.resolve("restauriert"));
        try (ZipFile archiv = new ZipFile(backup.toFile()); var strom = archiv.getInputStream(archiv.getEntry("faktura.db"))) {
            Files.copy(strom, restauriert.resolve("faktura.db"));
        }
        try (var k = kontext(restauriert)) {
            assertEquals(new BigDecimal("0.07"), k.getBean(DokumentRepository.class).findeNachNummer("R-2026-000001").getSummeBrutto());
            assertEquals("ok", k.getBean(JdbcTemplate.class).queryForObject("PRAGMA integrity_check", String.class));
        }
    }

    @Test @DisplayName("REM-09: Produktänderungen beeinflussen Einheiten in Folgebelegen und Stornos nicht")
    void einheitenImBelegzyklus() {
        String rechnungsnummer;
        String stornonummer;
        try (var k = kontext(ordner)) {
            k.getBean(de.lucasstrubel.faktura.firma.FirmenprofilService.class).speichere(TestBelege.FIRMA);
            var kunde = k.getBean(de.lucasstrubel.faktura.kunden.KundenVerwaltungsService.class).legeAn(
                    new de.lucasstrubel.faktura.kunden.Kunde("Prüfkunde", "Weg 1", "68163", "Mannheim"));
            var produkte = k.getBean(ProduktVerwaltungsService.class);
            Produkt produkt = new Produkt("Leistung", new BigDecimal("12.50"), new BigDecimal("0.19"));
            produkt.setEinheit("Std."); produkte.legeAn(produkt);
            var service = k.getBean(DokumentService.class);
            Dokument beleg = service.erstelleAngebot(kunde.getKundennummer(),
                    List.of(new Positionsangabe(produkt.getProduktnummer(), 2)), null);
            produkt.setEinheit("kg"); produkte.aendere(produkt);
            for (int i = 0; i < 3; i++) {
                beleg = service.erzeugeFolgebeleg(beleg.getBelegnummer());
                assertEquals("Stunde", beleg.getPositionen().get(0).getEinheit());
                assertEquals("HUR", beleg.getPositionen().get(0).getEinheitCode());
            }
            rechnungsnummer = beleg.getBelegnummer();
            beleg.setzeStatus(DokumentStatus.VERSENDET);
            k.getBean(DokumentRepository.class).speichere(beleg);
            stornonummer = service.storniere(rechnungsnummer).getBelegnummer();
        }
        try (var k = kontext(ordner)) {
            var repo = k.getBean(DokumentRepository.class);
            assertEquals("HUR", repo.findeNachNummer(rechnungsnummer).getPositionen().get(0).getEinheitCode());
            assertEquals("HUR", repo.findeNachNummer(stornonummer).getPositionen().get(0).getEinheitCode());
        }
    }

    @Test @DisplayName("REM-07: Alter JSON-Import erhält historische und fehlende Beträge")
    void jsonImport() throws Exception {
        var mapper = JsonPersistenz.mapper();
        var liste = mapper.createArrayNode();
        Rechnung original = rechnung();
        var alt = mapper.readTree(mapper.writerFor(Dokument.class).writeValueAsString(original));
        ((com.fasterxml.jackson.databind.node.ObjectNode) alt).remove("betragssnapshot");
        ((com.fasterxml.jackson.databind.node.ObjectNode) alt).put("summeSteuer", "0.02").put("summeBrutto", "0.08");
        alt.get("positionen").forEach(p -> ((com.fasterxml.jackson.databind.node.ObjectNode) p).remove(List.of("einheit", "einheitCode")));
        liste.add(alt);
        var unbekannt = alt.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) unbekannt).put("belegnummer", "R-2026-000002")
                .remove(List.of("summeNetto", "summeSteuer", "summeBrutto"));
        liste.add(unbekannt);
        mapper.writeValue(ordner.resolve("dokumente.json").toFile(), liste);
        try (var k = kontext(ordner)) {
            var repo = k.getBean(DokumentRepository.class);
            assertEquals(new BigDecimal("0.08"), repo.findeNachNummer("R-2026-000001").getSummeBrutto());
            assertNull(repo.findeNachNummer("R-2026-000001").getPositionen().get(0).getEinheit());
            assertNull(repo.findeNachNummer("R-2026-000002").getSummeNetto());
            assertEquals(Berechnungsart.UNGEKLAERT, repo.findeNachNummer("R-2026-000002").getBetragssnapshot().berechnungsart());
            assertNull(k.getBean(KennzahlenDienst.class).ermittle(LocalDate.of(2026, 10, 7)).umsatzJahr());
        }
    }

    @Test @DisplayName("REM-08: Fehlgeschlagene Upgradeprobe ändert V4 nicht, Wiederholung gelingt")
    void migrationsfehlerUndWiederholung() {
        SQLiteDataSource quelle = new SQLiteDataSource(); quelle.setUrl("jdbc:sqlite:" + ordner.resolve("faktura.db"));
        Flyway.configure().dataSource(quelle).target("4").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(quelle);
        jdbc.update("INSERT INTO dokument(belegnummer,typ,datum,kunden_referenz,status,summe_netto,summe_steuer,summe_brutto) VALUES ('R-2026-000001','RECHNUNG','2026-10-07','K','OFFEN','0','0','0')");
        jdbc.update("INSERT INTO dokumentposition(belegnummer,position,menge,einzelpreis_netto,steuersatz) VALUES ('R-2026-000001',1,1,'nicht lesbar','0.19')");
        assertThrows(RuntimeException.class, () -> UpgradeSicherung.pruefe(quelle, ordner, Flyway.configure().dataSource(quelle).load()));
        assertEquals("4", jdbc.queryForObject("SELECT MAX(version) FROM flyway_schema_history", String.class));
        jdbc.update("UPDATE dokumentposition SET einzelpreis_netto = '0'");
        try (var k = kontext(ordner)) {
            assertEquals(BigDecimal.ZERO, k.getBean(DokumentRepository.class).findeNachNummer("R-2026-000001").getSummeBrutto());
        }
    }

    @Test @DisplayName("REM-10: Produkte ohne Einheit werden Stück (C62); Pauschale (LS) und Monat (MON) bestehen die EN-16931-Prüfung (A-F-33)")
    void einheitenStandardUndNeueCodes() throws Exception {
        assertEquals("Stück", Einheiten.fuerBeleg(null));
        assertEquals("Stück", Einheiten.fuerBeleg(" "));
        assertEquals("Stunde", Einheiten.fuerBeleg("Std."));
        assertEquals("Paket", Einheiten.fuerBeleg("Paket"));
        try (var k = kontext(ordner)) {
            k.getBean(de.lucasstrubel.faktura.firma.FirmenprofilService.class).speichere(TestBelege.FIRMA);
            var kunde = k.getBean(de.lucasstrubel.faktura.kunden.KundenVerwaltungsService.class).legeAn(
                    new de.lucasstrubel.faktura.kunden.Kunde("Prüfkunde", "Weg 1", "68163", "Mannheim"));
            Produkt ohneEinheit = new Produkt("Leistung", new BigDecimal("12.50"), new BigDecimal("0.19"));
            k.getBean(ProduktVerwaltungsService.class).legeAn(ohneEinheit);
            assertNull(k.getBean(ProduktVerwaltungsService.class).findeProdukt(ohneEinheit.getProduktnummer()).getEinheit());
            Dokument angebot = k.getBean(DokumentService.class).erstelleAngebot(kunde.getKundennummer(),
                    List.of(new Positionsangabe(ohneEinheit.getProduktnummer(), 1)), null);
            assertEquals("Stück", angebot.getPositionen().get(0).getEinheit());
            assertEquals("C62", angebot.getPositionen().get(0).getEinheitCode());
        }
        Rechnung r = new Rechnung();
        r.setBelegnummer("R-2026-000003");
        r.setDatum(LocalDate.of(2026, 10, 10));
        r.setLeistungsdatum(LocalDate.of(2026, 10, 9));
        r.setZahlungsziel(LocalDate.of(2026, 10, 24));
        r.setzeKunde("K-000001", "Prüfkunde GmbH", "Weg 1, 68163 Mannheim");
        r.setzeAussteller(new de.lucasstrubel.faktura.firma.Firmenprofil("Prüffirma", "Testweg 2", "68163", "Mannheim",
                "DE123456789", "37/123/45678", null, null, "DE02 1203 0000 0000 2020 51", "BYLADEM1001", "Testbank"));
        r.setzePositionen(List.of(
                new Dokumentposition("P-000001", "Einrichtung", 1, new BigDecimal("250.00"), new BigDecimal("0.19"), "Pauschale", "LS"),
                new Dokumentposition("P-000002", "Wartung", 3, new BigDecimal("40.00"), new BigDecimal("0.19"), "Monat", "MON")));
        Path xml = ordner.resolve("einheiten.xml");
        new ERechnungExport(null).exportiereXml(r, xml);
        String inhalt = Files.readString(xml);
        assertTrue(inhalt.contains("unitCode=\"LS\""));
        assertTrue(inhalt.contains("unitCode=\"MON\""));
    }

    @Test @DisplayName("REM-11: Eingefrorene V6-Migration ordnet Altbestände wie Betragssnapshot ein (GR-02, A-F-25)")
    void migrationV6Eingefroren() {
        SQLiteDataSource quelle = new SQLiteDataSource(); quelle.setUrl("jdbc:sqlite:" + ordner.resolve("faktura.db"));
        Flyway.configure().dataSource(quelle).target("4").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(quelle);
        // A: Steuer je Satz gerundet; B: Steuer je Position gerundet; C: passt zu keiner Regel
        String[][] belege = {{"R-2026-000001", "1.06", "0.08", "1.14"}, {"R-2026-000002", "0.06", "0.02", "0.08"},
                {"R-2026-000003", "1", "1", "2"}};
        for (String[] beleg : belege) {
            jdbc.update("INSERT INTO dokument(belegnummer,typ,datum,kunden_referenz,status,summe_netto,summe_steuer,summe_brutto)"
                    + " VALUES (?,'RECHNUNG','2026-10-07','K-000001','VERSENDET',?,?,?)", beleg[0], beleg[1], beleg[2], beleg[3]);
            for (int i = 1; i <= 2; i++) {
                jdbc.update("INSERT INTO dokumentposition(belegnummer,position,menge,einzelpreis_netto,steuersatz)"
                        + " VALUES (?,?,1,'0.03','0.19')", beleg[0], i);
            }
        }
        jdbc.update("INSERT INTO dokumentposition(belegnummer,position,menge,einzelpreis_netto,steuersatz)"
                + " VALUES ('R-2026-000001',3,1,'1.00','0.07')");
        Flyway.configure().dataSource(quelle).load().migrate();

        Map<String, Berechnungsart> erwartet = Map.of("R-2026-000001", Berechnungsart.JE_STEUERSATZ,
                "R-2026-000002", Berechnungsart.ALTE_POSITIONSRUNDUNG, "R-2026-000003", Berechnungsart.UNGEKLAERT);
        for (String[] beleg : belege) {
            String art = jdbc.queryForObject("SELECT berechnungsart FROM dokument WHERE belegnummer = ?", String.class, beleg[0]);
            assertEquals(erwartet.get(beleg[0]).name(), art);
            List<Dokumentposition> positionen = jdbc.query("SELECT * FROM dokumentposition WHERE belegnummer = ? ORDER BY position",
                    (rs, n) -> Dokumentposition.wiederhergestellt(null, null, rs.getInt("menge"),
                            new BigDecimal(rs.getString("einzelpreis_netto")), new BigDecimal(rs.getString("steuersatz")),
                            null, null, new BigDecimal(rs.getString("positionssumme_netto"))), beleg[0]);
            // Gleiche Einordnung wie die Fachlogik zum Zeitpunkt der Migration (Stand 3.1)
            assertEquals(Betragssnapshot.ausAltbestand(positionen, new BigDecimal(beleg[1]), new BigDecimal(beleg[2]),
                    new BigDecimal(beleg[3])).berechnungsart().name(), art);
        }
        assertEquals(List.of("0.07;1.00;0.07", "0.19;0.06;0.01"), jdbc.queryForList(
                "SELECT steuersatz || ';' || netto || ';' || steuer FROM dokumentsteuer WHERE belegnummer = 'R-2026-000001'"
                        + " ORDER BY steuersatz", String.class));
        assertEquals(List.of("0.19;0.06;0.02"), jdbc.queryForList(
                "SELECT steuersatz || ';' || netto || ';' || steuer FROM dokumentsteuer WHERE belegnummer = 'R-2026-000002'", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dokumentsteuer WHERE belegnummer = 'R-2026-000003'", Integer.class));
        assertEquals("0.03", jdbc.queryForObject("SELECT positionssumme_netto FROM dokumentposition LIMIT 1", String.class));
    }

    @Test @DisplayName("REM-12: Nicht entfernbare Temp-Verzeichnisse blockieren weder Start noch Export (Q-06)")
    void aufraeumenOhneAbbruch() throws Exception {
        assertDoesNotThrow(() -> AtomareDatei.entferneTemporaerVerzeichnis(ordner.resolve("gibt-es-nicht")));
        Path verzeichnis = Files.createDirectory(ordner.resolve("temp"));
        Files.writeString(Files.createDirectory(verzeichnis.resolve("unterordner")).resolve("datei.txt"), "x");
        Files.writeString(verzeichnis.resolve("flach.txt"), "x");
        assertDoesNotThrow(() -> AtomareDatei.entferneTemporaerVerzeichnis(verzeichnis));
        assertTrue(Files.exists(verzeichnis.resolve("unterordner")));
    }
}
