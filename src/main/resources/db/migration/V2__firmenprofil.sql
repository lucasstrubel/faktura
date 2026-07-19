-- Firmenprofil des Ausstellers (§ 14 UStG: Name und Anschrift auf Rechnungen;
-- Bankverbindung für den Zahlungshinweis und die E-Rechnung).
-- Genau eine Zeile (id = 1); Werte pflegt die Einstellungen-Ansicht.

CREATE TABLE firmenprofil (
    id          INTEGER PRIMARY KEY CHECK (id = 1),
    name        TEXT NOT NULL,
    strasse     TEXT NOT NULL,
    plz         TEXT NOT NULL,
    ort         TEXT NOT NULL,
    ust_id_nr   TEXT,
    telefon     TEXT,
    e_mail      TEXT,
    iban        TEXT,
    bic         TEXT,
    bank        TEXT
);
