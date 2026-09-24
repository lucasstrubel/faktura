-- Kopfzeile, Titelblock und Seitenumbrüche für die PDF-Fassung der Dokumentation.
--
-- * Titel und Version aus dem Front Matter landen in der Kopfzeile
--   (\fakturatitel, \fakturaversion aus kopf.tex).
-- * Der Titelblock zeigt "Version X · Datum" statt nur des Datums.
-- * Seitenumbrüche setzt der Filter (siehe Pandoc unten); in den
--   Markdown-Dateien steht dafür kein \newpage, das GitHub als Text
--   anzeigen würde.
-- * Inline-Code erhält Umbruchstellen (siehe Code).

local function latex_escape(text)
  return (text:gsub("([&%%$#_{}])", "\\%1"))
end

-- LaTeX-Ersatz für Sonderzeichen in Code-Abschnitten
local SONDERZEICHEN = {
  ["\\"] = "\\textbackslash{}", ["{"] = "\\{", ["}"] = "\\}", ["$"] = "\\$",
  ["&"] = "\\&", ["#"] = "\\#", ["^"] = "\\^{}", ["_"] = "\\_", ["%"] = "\\%",
  ["~"] = "\\textasciitilde{}", ["<"] = "\\textless{}", [">"] = "\\textgreater{}",
}

-- Lange Bezeichner wie `StandardDokumentService.STANDARD_ZAHLUNGSZIEL_TAGE`
-- sprengen sonst schmale Tabellenspalten: Umbruchstellen nach . _ / , ( und
-- an Wortgrenzen im camelCase.
function Code(code)
  if not FORMAT:match("latex") then
    return nil
  end
  local ergebnis = {}
  local vorher = ""
  for _, zeichen in utf8.codes(code.text) do
    local z = utf8.char(zeichen)
    if vorher:match("%l") and z:match("%u") then
      table.insert(ergebnis, "\\allowbreak{}")
    end
    table.insert(ergebnis, SONDERZEICHEN[z] or z)
    if z:match("[%._/,(]") then
      table.insert(ergebnis, "\\allowbreak{}")
    end
    vorher = z
  end
  return pandoc.RawInline("latex", "\\texttt{" .. table.concat(ergebnis) .. "}")
end

function Meta(meta)
  local titel = meta.title and pandoc.utils.stringify(meta.title) or ""
  local version = meta.version and pandoc.utils.stringify(meta.version) or ""
  local versionstext = version ~= "" and ("Version " .. version) or ""

  -- Der gemeinsame Vorspann wird hier eingelesen statt über --include-in-header:
  -- Diese Option überschreibt die header-includes aus den Metadaten.
  local verzeichnis = pandoc.path.directory(PANDOC_SCRIPT_FILE)
  local datei = io.open(pandoc.path.join({ verzeichnis, "kopf.tex" }), "r")
  local vorspanntext = datei and datei:read("a") or ""
  if datei then datei:close() end

  local makros = "\\newcommand{\\fakturatitel}{" .. latex_escape(titel) .. "}\n"
      .. "\\newcommand{\\fakturaversion}{" .. latex_escape(versionstext) .. "}\n"
      .. vorspanntext
  local vorspann = meta["header-includes"]
  local eintrag = pandoc.MetaBlocks({ pandoc.RawBlock("latex", makros) })
  if vorspann == nil then
    meta["header-includes"] = pandoc.MetaList({ eintrag })
  elseif vorspann.t == "MetaList" then
    table.insert(vorspann, 1, eintrag)
  else
    meta["header-includes"] = pandoc.MetaList({ eintrag, vorspann })
  end

  if version ~= "" then
    local datum = meta.date and pandoc.utils.stringify(meta.date) or ""
    meta.date = pandoc.MetaString(versionstext .. (datum ~= "" and (" · " .. datum) or ""))
  end
  return meta
end

-- Neue Seite nach dem Inhaltsverzeichnis; mit "seitenumbruch: kapitel" im
-- Front Matter zusätzlich vor jeder Überschrift der Ebene 1 (Pflichtenheft,
-- dessen Teile A–D je auf einer neuen Seite beginnen sollen).
function Pandoc(dokument)
  local umbruch = dokument.meta.seitenumbruch
      and pandoc.utils.stringify(dokument.meta.seitenumbruch) == "kapitel"
  local bloecke = { pandoc.RawBlock("latex", "\\clearpage") }
  for _, block in ipairs(dokument.blocks) do
    if umbruch and block.t == "Header" and block.level == 1 then
      table.insert(bloecke, pandoc.RawBlock("latex", "\\clearpage"))
    end
    table.insert(bloecke, block)
  end
  dokument.blocks = bloecke
  return dokument
end
