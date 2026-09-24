package de.lucasstrubel.faktura.dokumente;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Modultests der Kennzahlen für die Übersichtsansicht (D-F-18): offener
 * Betrag, überfällige Rechnungen, Jahresumsatz und jüngste Belege.
 */
class KennzahlenDienstTest {

    /** Fester Stichtag, damit die Erwartungen nicht vom Testdatum abhängen. */
    private static final LocalDate HEUTE = LocalDate.of(2026, 7, 21);

    private final List<Dokument> bestand = new ArrayList<>();
    private KennzahlenDienst dienst;

    @BeforeEach
    void setUp() {
        dienst = new KennzahlenDienst(new DokumentServiceStub());
    }

    @Test
    @DisplayName("KZ-01: Offene und versendete Rechnungen bilden den offenen Betrag")
    void offenerBetragZaehltUnbezahlteRechnungen() {
        bestand.add(rechnung("R-2026-000001", "119.00", DokumentStatus.OFFEN, HEUTE.plusDays(7)));
        bestand.add(rechnung("R-2026-000002", "238.00", DokumentStatus.VERSENDET, HEUTE.plusDays(7)));
        bestand.add(rechnung("R-2026-000003", "500.00", DokumentStatus.STORNIERT, HEUTE.plusDays(7)));

        Kennzahlen kennzahlen = dienst.ermittle(HEUTE);

        assertEquals(2, kennzahlen.offeneAnzahl(), "stornierte Rechnungen zählen nicht");
        assertEquals(new BigDecimal("357.00"), kennzahlen.offenerBetrag());
    }

    @Test
    @DisplayName("KZ-02: Überfällig ist nur, wessen Zahlungsziel vor dem Stichtag liegt (GR-06)")
    void ueberfaelligkeitRichtetSichNachDemZahlungsziel() {
        bestand.add(rechnung("R-2026-000001", "100.00", DokumentStatus.OFFEN, HEUTE.minusDays(1)));
        bestand.add(rechnung("R-2026-000002", "200.00", DokumentStatus.OFFEN, HEUTE));
        bestand.add(rechnung("R-2026-000003", "300.00", DokumentStatus.OFFEN, HEUTE.plusDays(1)));
        bestand.add(rechnung("R-2026-000004", "400.00", DokumentStatus.OFFEN, null));

        Kennzahlen kennzahlen = dienst.ermittle(HEUTE);

        assertEquals(1, kennzahlen.ueberfaelligAnzahl(),
                "weder das heutige Zahlungsziel noch ein fehlendes gilt als überfällig");
        assertEquals(new BigDecimal("100.00"), kennzahlen.ueberfaelligBetrag());
    }

    @Test
    @DisplayName("KZ-03: Der Jahresumsatz zählt nur nicht stornierte Rechnungen des laufenden Jahres")
    void jahresumsatzGrenztJahrUndStornoAb() {
        bestand.add(rechnung("R-2026-000001", "119.00", DokumentStatus.OFFEN, null));
        bestand.add(rechnung("R-2026-000002", "100.00", DokumentStatus.STORNIERT, null));
        bestand.add(rechnung("R-2025-000001", "999.00", DokumentStatus.VERSENDET, null,
                LocalDate.of(2025, 12, 31)));
        // Angebote und Lieferscheine sind kein Umsatz
        bestand.add(angebot("AN-2026-000001", "5000.00"));

        assertEquals(new BigDecimal("119.00"), dienst.ermittle(HEUTE).umsatzJahr());
    }

    @Test
    @DisplayName("KZ-04: Die jüngsten Belege stehen zuerst und sind begrenzt")
    void letzteBelegeSindAbsteigendUndBegrenzt() {
        for (int i = 1; i <= KennzahlenDienst.ANZAHL_LETZTE_BELEGE + 3; i++) {
            Rechnung rechnung = rechnung(String.format("R-2026-%06d", i), "10.00",
                    DokumentStatus.OFFEN, null);
            rechnung.setDatum(LocalDate.of(2026, 1, 1).plusDays(i));
            bestand.add(rechnung);
        }

        Kennzahlen kennzahlen = dienst.ermittle(HEUTE);

        assertEquals(KennzahlenDienst.ANZAHL_LETZTE_BELEGE, kennzahlen.letzteBelege().size());
        assertEquals(String.format("R-2026-%06d", KennzahlenDienst.ANZAHL_LETZTE_BELEGE + 3),
                kennzahlen.letzteBelege().get(0).getBelegnummer(),
                "der jüngste Beleg muss zuerst stehen");
    }

    @Test
    @DisplayName("KZ-05: Ein leerer Bestand liefert Nullwerte statt einer Ausnahme")
    void leererBestandLiefertNullwerte() {
        Kennzahlen kennzahlen = dienst.ermittle(HEUTE);

        assertEquals(0, kennzahlen.belegeGesamt());
        assertEquals(0, kennzahlen.offeneAnzahl());
        assertEquals(new BigDecimal("0.00"), kennzahlen.offenerBetrag());
        assertEquals(new BigDecimal("0.00"), kennzahlen.umsatzJahr());
        assertEquals(List.of(), kennzahlen.letzteBelege());
    }

    private static Rechnung rechnung(String nummer, String brutto, DokumentStatus status,
                                     LocalDate zahlungsziel) {
        return rechnung(nummer, brutto, status, zahlungsziel, HEUTE);
    }

    /**
     * Der Status wird bewusst <em>zuletzt</em> gesetzt: Ein versendeter oder
     * stornierter Beleg lehnt jede weitere inhaltliche Änderung ab (GR-02).
     */
    private static Rechnung rechnung(String nummer, String brutto, DokumentStatus status,
                                     LocalDate zahlungsziel, LocalDate datum) {
        Rechnung rechnung = new Rechnung();
        rechnung.setBelegnummer(nummer);
        rechnung.setDatum(datum);
        rechnung.setzeKunde("K-000001", "Muster GmbH", "Hauptstr. 1, 68163 Mannheim");
        rechnung.setzePositionen(List.of(position(brutto)));
        rechnung.setZahlungsziel(zahlungsziel);
        if (status == DokumentStatus.STORNIERT) {
            rechnung.setzeStatus(DokumentStatus.OFFEN);
            rechnung.storniere(datum, "Test");
        } else {
            rechnung.setzeStatus(status);
        }
        return rechnung;
    }

    private static Angebot angebot(String nummer, String brutto) {
        Angebot angebot = new Angebot();
        angebot.setBelegnummer(nummer);
        angebot.setDatum(HEUTE);
        angebot.setzeKunde("K-000001", "Muster GmbH", "Hauptstr. 1, 68163 Mannheim");
        angebot.setzePositionen(List.of(position(brutto)));
        return angebot;
    }

    /** Position ohne Steuer, damit die Bruttosumme dem angegebenen Wert entspricht. */
    private static Dokumentposition position(String betrag) {
        return new Dokumentposition("P-000001", "Leistung", 1,
                new BigDecimal(betrag), BigDecimal.ZERO);
    }

    /** Nur {@code alleDokumente()} wird ausgewertet; alles Übrige bleibt ungenutzt. */
    private class DokumentServiceStub implements DokumentService {

        @Override
        public List<Dokument> alleDokumente() {
            return List.copyOf(bestand);
        }

        @Override
        public Angebot erstelleAngebot(String k, List<Positionsangabe> p, LocalDate g) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Auftragsbestaetigung erstelleAuftragsbestaetigung(String k, List<Positionsangabe> p) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Lieferschein erstelleLieferschein(String k, List<Positionsangabe> p, LocalDate l) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Rechnung erstelleRechnung(String k, List<Positionsangabe> p, LocalDate r,
                                         LocalDate l, LocalDate z) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Dokument erzeugeFolgebeleg(String belegnummer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void versende(String belegnummer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Rechnung storniere(String rechnungsnummer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markiereBezahlt(String rechnungsnummer, LocalDate bezahltAm) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Rechnung> offeneRechnungen() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Summen berechneSummen(List<Positionsangabe> positionen) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void exportierePdf(String belegnummer, Path zielDatei) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    @DisplayName("KZ-06: Bezahlte Rechnungen sind nicht mehr offen; Stornorechnungen zählen weder offen noch als Umsatz (A-F-28, A-F-29)")
    void bezahlteUndStornorechnungenZaehlenNicht() {
        Rechnung bezahlt = rechnung("R-2026-000001", "119.00", DokumentStatus.VERSENDET, HEUTE.minusDays(3));
        bezahlt.markiereBezahlt(HEUTE.minusDays(1));
        bestand.add(bezahlt);
        bestand.add(rechnung("R-2026-000002", "238.00", DokumentStatus.STORNIERT, HEUTE.minusDays(3)));
        Rechnung storno = new Rechnung();
        storno.setBelegnummer("R-2026-000003");
        storno.setDatum(HEUTE);
        storno.setzePositionen(List.of(position("238.00").negiert()));
        storno.setStornoZu("R-2026-000002");
        storno.setzeStatus(DokumentStatus.OFFEN);
        bestand.add(storno);

        Kennzahlen kennzahlen = dienst.ermittle(HEUTE);

        assertEquals(0, kennzahlen.offeneAnzahl());
        assertEquals(0, kennzahlen.ueberfaelligAnzahl());
        assertEquals(new BigDecimal("119.00"), kennzahlen.umsatzJahr(),
                "bezahlte Rechnung ist Umsatz; Storno und Stornorechnung heben sich auf");
    }
}
