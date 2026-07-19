-- Initiales Schema der SQLite-Datenbank (IF-01).
--
-- Geldbeträge und Steuersätze werden als TEXT gespeichert und in Java als
-- BigDecimal gelesen: SQLite-NUMERIC würde auf Gleitkomma zurückfallen und
-- Skala/Rundung verfälschen (Designgrundsatz: BigDecimal, Scale 2, HALF_UP).
-- Datumswerte sind ISO-8601-Strings (offenes, dokumentiertes Format, Q-08).
-- Belege bilden die Jackson-Polymorphie als Single-Table-Vererbung mit
-- Diskriminator "typ" ab; Belege werden nie gelöscht (GoBD, GR-01).

CREATE TABLE kunde (
    kundennummer      TEXT PRIMARY KEY,
    name              TEXT NOT NULL,
    strasse           TEXT NOT NULL,
    plz               TEXT NOT NULL,
    ort               TEXT NOT NULL,
    e_mail            TEXT,
    telefon           TEXT,
    ust_id_nr         TEXT
);

CREATE TABLE produkt (
    produktnummer     TEXT PRIMARY KEY,
    bezeichnung       TEXT NOT NULL,
    beschreibung      TEXT,
    einzelpreis_netto TEXT NOT NULL,
    steuersatz        TEXT NOT NULL,
    einheit           TEXT
);

CREATE TABLE dokument (
    belegnummer       TEXT PRIMARY KEY,
    typ               TEXT NOT NULL CHECK (typ IN
                          ('ANGEBOT', 'AUFTRAGSBESTAETIGUNG', 'LIEFERSCHEIN', 'RECHNUNG')),
    datum             TEXT NOT NULL,
    kunden_referenz   TEXT NOT NULL,
    kunde_name        TEXT,
    kunde_anschrift   TEXT,
    status            TEXT NOT NULL CHECK (status IN
                          ('ENTWURF', 'OFFEN', 'VERSENDET', 'STORNIERT')),
    vorgaenger_nr     TEXT,
    summe_netto       TEXT NOT NULL,
    summe_steuer      TEXT NOT NULL,
    summe_brutto      TEXT NOT NULL,
    gueltig_bis       TEXT,
    lieferdatum       TEXT,
    leistungsdatum    TEXT,
    zahlungsziel      TEXT,
    storniert_am      TEXT,
    storniert_von     TEXT
);

CREATE TABLE dokumentposition (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    belegnummer          TEXT NOT NULL REFERENCES dokument (belegnummer),
    position             INTEGER NOT NULL,
    produkt_referenz     TEXT,
    bezeichnung          TEXT,
    menge                INTEGER NOT NULL,
    einzelpreis_netto    TEXT,
    steuersatz           TEXT
);

CREATE INDEX idx_dokumentposition_belegnummer ON dokumentposition (belegnummer);
CREATE INDEX idx_dokument_kunden_referenz ON dokument (kunden_referenz);
