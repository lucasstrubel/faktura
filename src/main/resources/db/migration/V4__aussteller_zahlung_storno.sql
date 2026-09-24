-- Aussteller-Snapshot, Zahlungseingang und Stornorechnung (Pflichtenheft v2.4).
--
-- Steuernummer (§ 14 Abs. 4 Nr. 2 UStG): Eine Rechnung braucht die
-- Steuernummer oder die USt-IdNr. des Ausstellers; bisher kannte das
-- Firmenprofil nur die USt-IdNr.
--
-- Aussteller-Snapshot (A-F-27): Bisher las jeder PDF-/E-Rechnungs-Export das
-- *aktuelle* Firmenprofil. Nach einem Umzug oder Bankwechsel zeigte der
-- erneute Export einer längst versendeten Rechnung plötzlich andere
-- Absenderdaten — GR-02 verlangt aber, dass ein versendeter Beleg
-- reproduzierbar bleibt. Neue Belege speichern die Ausstellerdaten daher wie
-- die Kundendaten zum Erstellzeitpunkt. Bestehende Belege behalten NULL und
-- fallen beim Export auf das aktuelle Profil zurück (dokumentierte Ausnahme
-- für Altbestand).
--
-- Zahlungseingang (A-F-28): Die Zahlung ist keine inhaltliche Änderung des
-- Belegs und darf deshalb auch bei VERSENDET erfasst werden; sie ist ein
-- eigenes Datum statt eines fünften Status, damit die Statusfolge
-- ENTWURF → OFFEN → VERSENDET / STORNIERT unverändert bleibt.
--
-- Stornorechnung (A-F-29): Eine versendete Rechnung wird über eine neue
-- Rechnung mit negativen Mengen korrigiert (F-24); "storno_zu" verweist auf
-- die stornierte Rechnung.

ALTER TABLE firmenprofil ADD COLUMN steuernummer TEXT;

ALTER TABLE dokument ADD COLUMN aussteller_name          TEXT;
ALTER TABLE dokument ADD COLUMN aussteller_strasse       TEXT;
ALTER TABLE dokument ADD COLUMN aussteller_plz           TEXT;
ALTER TABLE dokument ADD COLUMN aussteller_ort           TEXT;
ALTER TABLE dokument ADD COLUMN aussteller_ust_id_nr     TEXT;
ALTER TABLE dokument ADD COLUMN aussteller_steuernummer  TEXT;
ALTER TABLE dokument ADD COLUMN aussteller_telefon       TEXT;
ALTER TABLE dokument ADD COLUMN aussteller_e_mail        TEXT;
ALTER TABLE dokument ADD COLUMN aussteller_iban          TEXT;
ALTER TABLE dokument ADD COLUMN aussteller_bic           TEXT;
ALTER TABLE dokument ADD COLUMN aussteller_bank          TEXT;

ALTER TABLE dokument ADD COLUMN bezahlt_am TEXT;
ALTER TABLE dokument ADD COLUMN storno_zu  TEXT;
