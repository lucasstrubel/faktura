# CLAUDE.md

Leitfaden für Claude Code. Die gemeinsamen Projektregeln für alle Agenten — Projekt, Build,
Architektur, fachliche Invarianten, Codestil, Pull Requests und die Zusammenarbeit mit
Codex — stehen in `AGENTS.md`:

@AGENTS.md

## Nur für Claude Code

- **Codex als zweiter Agent:** CLI `codex` (npm-Paket `@openai/codex`, angemeldet mit
  ChatGPT). Review eines Zweigs mit `/codex-review`, Aufträge an Codex mit `/codex-aufgabe`
  (lokale Befehle unter `.claude/commands/`). Codex-Ergebnisse sind Vorschläge: am Code
  nachprüfen und `verify` selbst ausführen, bevor sie übernommen werden.
- **Fenster auf Lucas' Desktop:** GUI-Tests (`-Dfaktura.gui.tests=true`) und App-Starts
  vorher ankündigen.
