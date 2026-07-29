# Case Study: Faktura

**Turning a university software-engineering project into a compliant, shippable desktop product.**

Repository: [github.com/lucasstrubel/faktura](https://github.com/lucasstrubel/faktura) ·
[Releases](https://github.com/lucasstrubel/faktura/releases) ·
[German documentation](dokumentation/) · [README](README.md)

---

## At a glance

| | |
|---|---|
| **What** | Faktura — a local-only invoicing application for freelancers and micro-businesses |
| **Role** | Part of a seven-person student team for v1.0; sole developer for v2.0 and v3.0 |
| **Timeline** | v1.0 as a course project, summer term 2026 (TH Mannheim, *Software Engineering 1*); solo rebuild from July 2026 |
| **Scale** | ~10,500 lines of Java (96 main + 20 test files), 6 FXML views, 3 database migrations, ~3,300 lines of German specification |
| **Stack** | Java 21 · Spring Boot · JavaFX · SQLite + Flyway · PDFBox · Mustang (EN 16931) · Maven · JUnit 5 |
| **State** | 135 tests green, coverage gate + SpotBugs enforced in CI, Windows installer published from a tag |

---

## The problem

There are two problems in this project, stacked on top of each other. The interesting one is
the second.

### 1. Invoicing is a compliance problem wearing a CRUD costume

A freelancer in Germany writing an invoice has three plausible options, and all of them are bad.
Spreadsheets are free but not legal: German bookkeeping rules (**GoBD**) require invoice numbers
to be *gapless* and sent invoices to be *unalterable*, neither of which a spreadsheet enforces.
SaaS invoicing is legal but charges rent forever for a few documents a month, and puts customer
data on someone else's servers. Open-source alternatives such as *Fakturama* are capable but
demand real setup and maintenance effort from a user whose job is not IT.

On top of that, structured electronic invoices (ZUGFeRD / XRechnung per EN 16931) are moving from
optional to mandatory for domestic B2B trade in Germany: since **1 January 2025** every business
must be able to *receive* and process them, and the obligation to *issue* them phases in from 2027
for larger companies and 2028 for everyone else. A PDF stops being sufficient. The target user
ends up needing software for something they cannot reasonably do by hand.

That makes the domain deceptively hard. The screens look like ordinary CRUD, but underneath
sit rules that must hold *always*, not usually:

| Rule | What it actually demands |
|---|---|
| GoBD — gapless numbering | An invoice number may never be skipped, even if saving crashes |
| GoBD — immutability | Once sent, an invoice can be cancelled but never edited or deleted |
| § 14 UStG | Every invoice carries a fixed set of mandatory fields |
| EN 16931 | Structured XML alongside the human-readable PDF |
| GDPR | Customer data stays on the machine — no cloud, no telemetry |

### 2. A project that passes its acceptance is not a product

Version 1.0 did what a course project is supposed to do. It went through the full V-model —
requirements specification, system design, module test plan, acceptance — and passed with all
mandatory requirements implemented and 71/71 module tests green.

It was also not something anyone could ship:

- Data lived in **JSON files**, rewritten wholesale on every change.
- The invoice counter was an **integer in memory**, incremented before the save. If the save
  failed, that number was simply gone — silently violating the one rule the domain cares most about.
- Wiring was **manual dependency injection** in `Main.java`.
- The UI was **Swing**, and every long-running action — PDF export, CSV, printing — ran on the
  event thread, so the window froze.
- Installation meant "have a JDK, run a JAR".
- E-invoicing, the thing the law now requires, was an explicit non-goal.

The real project was the second one: take a codebase that *works* and make it *hold* — without
throwing away the parts that earned their place.

---

## The solution

Faktura covers the full German commercial document cycle — *Angebot* (quote) →
*Auftragsbestätigung* (order confirmation) → *Lieferschein* (delivery note) → *Rechnung*
(invoice) — with each document derivable from its predecessor, PDF and EN 16931 export,
customer and product management, and a company profile that feeds the letterhead.

The architecture kept the four-component split from the original specification, because the
requirement IDs in the specs map onto it and that traceability is worth preserving:

| Package | Component | Responsibility |
|---|---|---|
| `dokumente` | A | Document cycle, numbering, PDF export, EN 16931 e-invoice |
| `produkte` | B | Product management (CRUD, numbering, delete protection) |
| `kunden` | C | Customer management (CRUD, numbering, delete protection) |
| `gui` | D | JavaFX UI (FXML views, Spring-injected controllers, invoice wizard) |
| `gemeinsam` | — | Event bus, validation, CSV, backup, number ranges, exceptions |
| `firma` | — | Company profile (letterhead, bank details) |

Four decisions carry the compliance rules. Each one is a place where the obvious implementation
is wrong.

### Gapless numbering is a property of *allocation*, not of the numbers you see

The v1.0 counter produced gapless numbers right up until something failed. The fix was to stop
treating numbering as bookkeeping the application does and start treating it as a database
invariant: a `nummernkreis` table (Flyway migration V3), read-and-increment through
[`JdbcNummernkreis`](src/main/java/de/lucasstrubel/faktura/gemeinsam/JdbcNummernkreis.java),
running **inside the same transaction** as the document insert. A failed save rolls the
allocation back with it.

The proof is a test that forces the failure —
[`NummernkreisIntegritaetTest`](src/test/java/de/lucasstrubel/faktura/dokumente/NummernkreisIntegritaetTest.java)
injects a `@Primary` repository that throws on demand, then asserts the next successful invoice
gets number 2 rather than 3, and that the failed document was never persisted. It also asserts
the counter survives a restart, because "gapless" outlives the process.

### Sent documents are immutable, and nothing is ever deleted

`Dokument.pruefeAenderbar()` rejects every content change once a document reaches `VERSENDET` or
`STORNIERT`. Cancellation replaces deletion — a cancelled invoice keeps its number, because
removing it would create the gap the rules forbid. One subtlety that only appears in practice:
when loading a sent document from the database, the status has to be set **last**, or the guard
rejects the very data it is supposed to protect.

### Documents snapshot their customer and prices

A document stores the customer's name and address and each position's price as they were *at
creation time*. Editing a customer's address next year must not silently rewrite last year's
invoices. This was in the original specification (`C-F-06`) and is one of the parts of v1.0 that
survived every rewrite untouched.

### The UI only hears about data that survived the commit

Services publish a `DatenGeaendertEreignis` after every write and views refresh themselves. Once
real transactions existed, the naive listener became a bug: after a rollback the UI would show a
record that did not exist. The bridge in
[`EreignisBus`](src/main/java/de/lucasstrubel/faktura/gemeinsam/EreignisBus.java) is therefore
`@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` — the fallback
keeps events flowing for calls outside a transaction.

Beyond those four: everything that can take time (PDF, e-invoice, CSV, backup, print, mail) runs
on a background executor; interim PDFs live in a session directory wiped on shutdown instead of
leaking customer data into the system temp folder; and backups are taken with SQLite's
`VACUUM INTO` rather than copying a file that has open connections and a WAL side file.

---

## How it was built

Eleven phases across two releases, each landed as one commit behind green CI:

| Release | Phases | What changed |
|---|---|---|
| **2.0** | 1–7 | Quality baseline (validation, logging, CI, JaCoCo, SpotBugs) · Spring Boot IoC · SQLite + Flyway behind the existing repository interfaces · complete JavaFX rewrite · company profile · EN 16931 e-invoicing · jpackage MSI installer with a tag-triggered release workflow |
| **3.0** | 8–11 | Transactional numbering · background execution and session-scoped temp files · `VACUUM INTO` backups · document output extracted into its own component · redesigned UI (sidebar navigation, dashboard, master-detail, status badges, dark mode) · coverage floor as a build gate |

The ordering was deliberate: the quality baseline came *first*, so every subsequent structural
change — swapping the persistence layer, replacing the entire UI toolkit — had a test suite and
a static-analysis gate underneath it rather than beside it.

---

## Tech stack, and why

| Layer | Choice | Why this one |
|---|---|---|
| Language | **Java 21** | An LTS release; records for the value types (`Summen`, `Kennzahlen`, `Positionsangabe`, …) and text blocks for readable SQL |
| Wiring | **Spring Boot** | Not for the web server — for the IoC container, declarative `@Transactional`, and an event system. Transactional events are exactly what the numbering invariant needed |
| UI | **JavaFX + AtlantaFX + Ikonli** | Local-only is the product promise, so a desktop toolkit, not a browser. AtlantaFX supplies theme *variables*, so dark mode is one stylesheet rather than two |
| Database | **SQLite + Spring JDBC + Flyway** | Single user, zero install, one file to back up. Postgres would add a service to operate for no benefit here. Flyway makes schema changes reviewable; JDBC over JPA because the schema is small and explicit SQL is easier to reason about |
| PDF | **Apache PDFBox** | Full control over a fixed business layout |
| E-invoice | **Mustang** | EN 16931 is a large, validated standard — hand-rolling ZUGFeRD CII XML would be a project of its own |
| Persistence format | **Jackson (JSON)** | Retained for one-time import and backups; polymorphic document types via `@JsonTypeInfo` |
| Build & quality | **Maven · JUnit 5 · JaCoCo · SpotBugs · GitHub Actions** | `verify` runs tests, the coverage gate and static analysis; CI runs the identical command |
| Packaging | **jpackage** | A per-user MSI, so installing does not require admin rights or a preinstalled JDK |

Money is `BigDecimal` with scale 2 and `HALF_UP` throughout. Domain identifiers, comments and
specifications are in German — the language of the domain and its regulations — while the README
and this document are in English.

---

## Results

- **135 automated tests**, zero failures; the suite grew from 71 at v1.0 acceptance.
- **Coverage gate in the build**: `verify` fails below 70 % instruction and 60 % branch coverage
  on the non-UI code. FXML-bound view classes are excluded, since their dialog logic lives in
  separate GUI-free controller classes that *are* counted.
- **SpotBugs: zero findings**, enforced — not reported and ignored.
- **Performance requirements are assertions, not anecdotes.** `PerformanceTest` runs the
  Lastenheft's reference size — 5,000 customers, 5,000 products, 1,000 documents — against the
  specified ceilings (start ≤ 5 s, search ≤ 1 s, PDF of 50 positions ≤ 2 s, full CSV export
  ≤ 30 s). If a change makes the application slower than the requirement allows, CI says so.
- **Full requirements traceability**: [`Anforderungsabgleich.md`](dokumentation/anforderungen/Anforderungsabgleich.md)
  maps every requirement to the code that implements it and the test that proves it. Two items
  are honestly marked as *not* provable by code — the usability tests need five real people.
- **One-click distribution**: pushing a `v*` tag builds and publishes a Windows MSI.

---

## What I learned

**A counter in memory is not a business rule.** "Gapless" is a property of how numbers are
*allocated*, not of the numbers you happen to see in the list. The v1.0 implementation looked
correct in every test that didn't fail, which is exactly why it was dangerous. The lesson I take
forward: for an invariant that has to hold under failure, the test that *forces the failure* is
the requirement. Everything else is decoration.

**Auto-configuration is not a contract.** Flyway did not run under Spring Boot 4 with
`flyway-core` on the classpath alone; migrations had to be invoked programmatically in the
`dataSource` bean. I lost time assuming a framework had done something because it usually does.
Now I check that the magic happened before building on top of it.

**Interfaces are what make a rewrite survivable.** The repository interfaces were written in v1.0
for JSON files. Moving to SQLite required new implementations and changed *nothing* above them.
The same held for the UI: because the dialog logic sat in GUI-free controller classes, throwing
away Swing entirely for JavaFX left those classes untouched. Neither payoff was visible when the
boundaries were drawn — that is the point of drawing them.

**Compiling and passing tests is not "working".** Four real defects — a KPI card pushed off the
window, truncated table columns, a toolbar with every label clipped, detail labels rendering as
"K…" — were invisible to the build and obvious within seconds of launching the app. For anything
with a UI, looking at it is part of the definition of done, not a nicety afterwards.

**A coverage threshold should ratchet, not aspire.** The floor sits just below what the suite
actually achieves. Its job is to prevent regression, not to announce an ambition nobody acts on;
whoever raises coverage raises the floor behind them. A threshold set to an aspirational number
gets disabled the first time it blocks someone.

**Documentation rots faster than code, and more quietly.** Nothing fails when a document goes
stale. The final presentation describes Swing, manual DI, and e-invoicing as a non-goal — all
three were true the week they were written and false a fortnight later, and nothing anywhere
signalled the difference. Specs need the same "is this still true?" pass as code; and where a
document is a historical record rather than a current one, it should say so on its face instead
of quietly reading as fact.

### Working with Claude Code

This project was built with **[Claude Code](https://claude.com/claude-code)** as a working tool,
and it seems more useful to describe that accurately than to leave it implied.

**What the agent did well:** mechanical work at a scale I would not have done by hand — renaming
a package across 96 files, generating repository and service boilerplate, drafting the German
specification documents and test scaffolding, writing PlantUML sources and Maven configuration.
It is fast at producing consistent structure once the structure has been decided.

**What stayed mine:** every decision in the "solution" section above. Which rules are invariants,
whether numbering belongs in a transaction, what to keep from v1.0 and what to delete, and
whether a change was actually finished. The agent proposed reasonable code; it did not know that
gapless numbering was the requirement the whole design had to bend around, because that is domain
knowledge from the Lastenheft, not from the code.

**Where it needed correction:** left unspecified, suggestions default to the generic solution —
the plausible-looking counter rather than the transactional one. Domain rules have to be stated
explicitly and then verified in the output, especially in business logic.

**The workflow that made it safe:** changes sized to one phase and one commit; CI, SpotBugs and
the coverage gate as a standing guardrail rather than an afterthought; and, for anything visual,
launching the application and looking at it. The guardrails matter more when work moves quickly —
a fast contributor with no test suite is just a fast way to break things.

---

## What's next

- **Usability testing with five real users** — the one acceptance criterion (Q-05 / AC-11) that
  code cannot prove, and still open.
- **Linux and macOS packaging** — building from source already works on all three platforms;
  only the installer is Windows-only today.
- **Dunning (*Mahnwesen*)** — listed as a non-goal in v1.0, and the natural next step now that
  invoices and payment terms exist.

---

*Developed by Lucas Strubel. Source under the [MIT License](LICENSE); full German
software-engineering documentation under [`dokumentation/`](dokumentation/).*
