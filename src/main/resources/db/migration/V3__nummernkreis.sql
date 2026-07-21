-- Nummernkreise für die transaktionssichere Vergabe fortlaufender Nummern (GR-01).
--
-- Bisher liefen die Zähler ausschließlich im Speicher und wurden erhöht, bevor
-- der Beleg gespeichert war: schlug das Speichern fehl, war die Nummer
-- verbraucht und es entstand eine Lücke in der Rechnungsnummernfolge -- genau
-- das verbietet GR-01. Die Vergabe läuft jetzt in derselben Transaktion wie
-- das Speichern und wird bei einem Fehlschlag mit zurückgerollt.
--
-- "letzter_wert" ist die zuletzt vergebene laufende Nummer. Die Zeilen werden
-- beim ersten Zugriff je Bereich angelegt und dabei aus dem vorhandenen
-- Bestand abgeleitet; laufende Installationen übernehmen damit nahtlos, ohne
-- dass diese Migration die Belegnummern selbst zerlegen muss.
--
-- Bereich ist der Nummernpräfix (AN, AB, LS, R, K, P). Jahresunabhängige
-- Kreise -- Kunden und Produkte -- verwenden jahr = 0.

CREATE TABLE nummernkreis (
    bereich      TEXT    NOT NULL,
    jahr         INTEGER NOT NULL,
    letzter_wert INTEGER NOT NULL,
    PRIMARY KEY (bereich, jahr)
);
