package de.lucasstrubel.faktura.dokumente;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Objects;

/** Unveränderlicher, vollständiger Betragsstand eines Belegs (GR-02, A-F-25). */
public record Betragssnapshot(BigDecimal netto, BigDecimal steuer, BigDecimal brutto,
                              List<Steuerzeile> steuerzeilen, Berechnungsart berechnungsart) {
    public Betragssnapshot {
        steuerzeilen = List.copyOf(steuerzeilen);
        Objects.requireNonNull(berechnungsart);
    }

    public static Betragssnapshot berechne(List<Dokumentposition> positionen) {
        return berechne(positionen, false);
    }

    private static Betragssnapshot berechne(List<Dokumentposition> positionen, boolean jePosition) {
        if (positionen.stream().anyMatch(p -> p.getPositionssummeNetto() == null || p.getSteuersatz() == null)) {
            BigDecimal netto = positionen.stream().anyMatch(p -> p.getPositionssummeNetto() == null) ? null
                    : positionen.stream().map(Dokumentposition::getPositionssummeNetto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
            return new Betragssnapshot(netto, null, null, List.of(), Berechnungsart.UNGEKLAERT);
        }
        Map<BigDecimal, BigDecimal> nettoJeSatz = new TreeMap<>();
        Map<BigDecimal, BigDecimal> steuerJeSatz = new TreeMap<>();
        for (Dokumentposition position : positionen) {
            BigDecimal satz = position.getSteuersatz().stripTrailingZeros();
            BigDecimal netto = position.getPositionssummeNetto();
            nettoJeSatz.merge(satz, netto, BigDecimal::add);
            steuerJeSatz.merge(satz, netto.multiply(satz).setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        }
        List<Steuerzeile> zeilen = new ArrayList<>();
        nettoJeSatz.forEach((satz, netto) -> zeilen.add(new Steuerzeile(satz,
                netto.setScale(2, RoundingMode.HALF_UP), jePosition ? steuerJeSatz.get(satz)
                : netto.multiply(satz).setScale(2, RoundingMode.HALF_UP))));
        BigDecimal netto = zeilen.stream().map(Steuerzeile::netto).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        BigDecimal steuer = zeilen.stream().map(Steuerzeile::steuer).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        return new Betragssnapshot(netto, steuer, netto.add(steuer), zeilen,
                jePosition ? Berechnungsart.ALTE_POSITIONSRUNDUNG : Berechnungsart.JE_STEUERSATZ);
    }

    /** Rekonstruiert ausschließlich nachweisbare Altregeln; gespeicherte Summen bleiben maßgeblich. */
    public static Betragssnapshot ausAltbestand(List<Dokumentposition> positionen,
                                                BigDecimal netto, BigDecimal steuer, BigDecimal brutto) {
        boolean vollstaendig = positionen.stream().allMatch(p -> p.getEinzelpreisNetto() != null
                && p.getPositionssummeNetto() != null && p.getSteuersatz() != null);
        if (vollstaendig) {
            for (boolean alteRegel : new boolean[]{false, true}) {
                Betragssnapshot probe = berechne(positionen, alteRegel);
                if (gleich(probe.netto, netto) && gleich(probe.steuer, steuer) && gleich(probe.brutto, brutto)) {
                    return new Betragssnapshot(netto, steuer, brutto, probe.steuerzeilen, probe.berechnungsart);
                }
            }
        }
        return new Betragssnapshot(netto, steuer, brutto, List.of(), Berechnungsart.UNGEKLAERT);
    }

    public Betragssnapshot negiert() {
        if (netto == null || steuer == null || brutto == null) {
            throw new IllegalStateException("Der historische Beleg enthält keine vollständigen Beträge.");
        }
        return new Betragssnapshot(netto.negate(), steuer.negate(), brutto.negate(),
                steuerzeilen.stream().map(z -> new Steuerzeile(z.steuersatz(), z.netto().negate(), z.steuer().negate())).toList(),
                berechnungsart);
    }

    private static boolean gleich(BigDecimal links, BigDecimal rechts) {
        return links != null && rechts != null && links.compareTo(rechts) == 0;
    }
}
