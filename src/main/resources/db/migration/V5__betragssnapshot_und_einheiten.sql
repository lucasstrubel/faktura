-- Bestehende Summen werden nicht verändert (GR-02, A-F-25, RC-02).
ALTER TABLE dokument ADD COLUMN berechnungsart TEXT;
ALTER TABLE dokumentposition ADD COLUMN einheit TEXT;
ALTER TABLE dokumentposition ADD COLUMN einheit_code TEXT;
ALTER TABLE dokumentposition ADD COLUMN positionssumme_netto TEXT;
CREATE TABLE dokumentsteuer (
    belegnummer TEXT NOT NULL REFERENCES dokument(belegnummer),
    steuersatz TEXT NOT NULL,
    netto TEXT NOT NULL,
    steuer TEXT NOT NULL,
    PRIMARY KEY (belegnummer, steuersatz)
);
CREATE INDEX idx_dokumentposition_produkt ON dokumentposition(produkt_referenz);
