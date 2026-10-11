# Portfolio-Abschluss und Vorführung

Stand: 10.10.2026. Ziel ist ein nachvollziehbares studentisches Softwareprojekt mit
reproduzierbarer Windows-Demo. Die vollständige Produktfreigabe 3.1.0-rc.1 bleibt ein
separates Ziel. Bestehende Anforderungen und historische Ergebnisse werden nicht
nachträglich als bestanden umgedeutet.

## Umfang und bewusste Zurückstellungen

| Punkt | Entscheidung für das Portfolio | Nachweis / Grenze |
|---|---|---|
| Preise, Daten, Betrags-/Einheitensnapshots, Exporte, Sicherung | Bestandteil | Regressionen im Modultestbericht, Kapitel 8 |
| Windows-Demo mit fiktiven Daten | Bestandteil | `tools/portfolio-demo.ps1`, PORT-01 |
| Portables Windows-Paket mit Laufzeit | Lokal bestanden | `tools/windows-paket-pruefen.ps1`, PORT-02; Details im Testbericht 8.5 |
| Abhängigkeitsscan | Erforderlicher offener Nachweis | NVD-Abruf erneut HTTP 429; kein NVD-Schlüssel konfiguriert |
| MSI-Installation / Deinstallation | Offener CI-Nachweis | Nur disponibler Windows-Runner; portabler Start ersetzt diesen Test nicht |
| Startzeit höchstens 5 Sekunden | Weiterhin nicht erfüllt | Letzte Serie 4.709 / 5.085 / 5.972 ms; keine Grenzwertänderung |
| Linux-/macOS-JAR | Ungeprüfte Zielplattformen des RC | Kein aktueller Plattformnachweis; Portfolio-Demo beschränkt sich auf Windows |
| Usability mit mindestens fünf Personen | Auf Wunsch des Projektverantwortlichen zurückgestellt | Q-05/Q-09 bleiben ohne empirischen Nachweis; keine erfundenen Ergebnisse |
| Netzwerk-Monitoring | Noch nicht durchgeführt | Spezifizierte Nachweismethode für Q-06 bleibt offen; Codeprüfung ersetzt sie nicht |
| Steuerbefreiungen/Kleinunternehmer, Mahnwesen, native Linux-/macOS-Pakete | Spätere Erweiterungen | Kein Bestandteil des Portfolio-Abschlusses |
| Rechtliche Gesamtbewertung | Außerhalb des Portfolio-Abschlusses | Keine pauschale Compliance- oder Produktivfreigabe |

Eine Vorführung ist trotz dokumentierter Grenzen möglich. Der Portfolio-Abschluss
ist erst vollständig nachgewiesen, wenn die vorgesehenen Sicherheits- und
Windows-Installationsprüfungen tatsächlich bestanden sind. Der RC verlangt zusätzlich
alle ursprünglich vereinbarten Release-Gates. Veröffentlichung und Tags bleiben separat.

## Reproduzierbare Vorbereitung

Voraussetzungen: Windows x64, JDK 21 (mit `java` und `jpackage` im PATH), Maven-Wrapper.
Im Projektverzeichnis:

```powershell
./mvnw.cmd verify '-Dfaktura.gui.tests=true'
powershell -NoProfile -ExecutionPolicy Bypass -File tools/portfolio-demo.ps1 -Starten
```

Der zweite Befehl kompiliert die Demoklasse und startet das zuvor gebaute JAR. Nach
Änderungen am Anwendungscode zuerst erneut `verify` ausführen. Die Java-Klasse unter
`src/test` gehört nicht zur ausgelieferten Anwendung. Sie verwendet die produktiven
Spring-Dienste, SQLite-Migrationen und Exporte.

Jeder Aufruf erstellt ein neues Verzeichnis `target/portfolio-demo-<UUID>`; vorhandene
Ziele werden abgelehnt. Es gibt keine automatische Löschung. Der letzte Pfad steht in
`target/portfolio-demo-pfad.txt`. Ohne `-Starten` wird nur vorbereitet. Ausschließlich
erfundene Firmendaten verwenden, keine Belege versenden und keine Zahlungen auslösen.

Enthalten: eine Firma, ein Kunde, zwei Produkte, Angebot, Auftragsbestätigung,
Lieferschein und versendete Rechnung sowie eine weitere bezahlte Rechnung.
Daneben liegen Beispiel-PDF/XML, Gesamt-CSV-ZIP, SQLite-Sicherung und `DEMOSTAND.txt`.
PORT-01 prüft beim Aufbau die unveränderte Rechnung nach einer Produktpreisänderung.

## Ablauf für eine Vorführung von etwa drei Minuten

| Zeit | Handlung | Aussage / erwartetes Ergebnis |
|---|---|---|
| 0:00–0:25 | Übersicht und Belegkette öffnen | Lokal gespeicherter Dokumentenzyklus; offene und bezahlte Rechnung |
| 0:25–1:15 | Neue Rechnung: Beispielstudio, 2 Stunden Beratung + 1 Stück Dokumentation | 185,00 EUR netto, 35,15 EUR Steuer, 220,15 EUR brutto; Daten prüfen und speichern |
| 1:15–1:45 | PDF exportieren und öffnen | Preis, Einheit, Steuersatz und Summen vollständig lesbar |
| 1:45–2:20 | Produkt Beratung von 80,00 auf 95,00 EUR ändern; bestehende Rechnung erneut öffnen | Bestehender Beleg bleibt bei 220,15 EUR; neue Berechnung wäre 255,85 EUR brutto |
| 2:20–2:45 | XML-Ausgabe und vorbereiteten Gesamtexport zeigen | Schema-/Schematron-Prüfung; dokumentierte CSV-Daten; historische Einschränkungen nennen |
| 2:45–3:00 | Grenzen nennen | Usability zurückgestellt; Startzeit, Security- und externe Release-Nachweise offen |

Die Tabelle ist ein Drehbuch, keine Behauptung über eine bereits aufgezeichnete
Video-Demo oder eine durchgeführte Usability-Studie. Für einen neuen Versuch das Skript
erneut starten; so bleiben Produktpreise und Ausgangsbelege reproduzierbar.

## Technische Prüfungen ohne Veröffentlichung

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/windows-paket-pruefen.ps1 -Jar target/faktura-3.1.0-rc.1.jar
```

Das Skript baut mit `jpackage` ein portables App-Verzeichnis und startet dessen EXE mit
gebündelter Laufzeit, ohne `JAVA_HOME` und mit isoliertem Datenpfad. Es installiert
nichts. Die 30-Sekunden-Frist ist ein Smoke-Test, kein Ersatz für Q-04 (5 Sekunden).
Der Bericht enthält den SHA-256 des tatsächlich geprüften JARs.

Nach erfolgreicher Vorbereitung lässt sich derselbe Demobestand auch mit der gebündelten
Laufzeit öffnen (kein systemweit installiertes Java für diesen Start erforderlich):

```powershell
$demopfad = Get-Content target/portfolio-demo-pfad.txt
$paketpfad = Get-Content target/windows-paket-pfad.txt
& "$paketpfad/Faktura/Faktura.exe" "--faktura.daten-verzeichnis=$demopfad/daten"
```

`.github/workflows/portfolio.yml` führt Windows-Build, Demo, portablen Start und einen
echten MSI-Test aus; der vollständige Sicherheitsscan läuft unabhängig daneben.
Der Workflow veröffentlicht nichts und lockert keine Release-Gates. Er muss zuerst
auf einem autorisierten Remote-Branch verfügbar sein. Der aktuelle lokale Arbeitsstand
wurde nicht gepusht. In GitHub ist kein `NVD_API_KEY`-Secret hinterlegt; dessen Einrichtung
und ein erfolgreicher Scan bleiben erforderlich. Ein Lauf gegen älteren Remote-Code
wäre kein Nachweis für diese Änderungen.

## Eigenleistung und KI-Unterstützung erläutern

Die Herkunft aus dem Teamprojekt, die eigene Weiterentwicklung sowie Claude- und
Codex-Unterstützung getrennt darstellen. Nur persönlich getroffene oder nachvollzogene
Entscheidungen als eigene Leistung beanspruchen. Für ein Gespräch anhand konkreten Codes
erläutern können:

- Warum Nummernvergabe und Belegspeicherung dieselbe Transaktion brauchen.
- Warum historische Beträge beim Laden nicht neu berechnet werden dürfen.
- Warum fehlende Daten von bekannten Nullwerten unterschieden werden.
- Wie PDF und validiertes XML auf denselben Snapshot zurückgeführt werden.
- Wie ein fehlgeschlagener Export oder ein Upgrade bestehende Daten schützt.

Die bestehende Fallstudie und die aktuelle Ergänzung der Präsentation dienen als Einstieg;
die umfangreichen Spezifikationen bleiben vertiefende Belege. Neue Features beginnen erst
nach dieser Abschlussrunde, sofern dafür ein konkretes Lernziel oder Nutzungsszenario besteht.
