package de.lucasstrubel.faktura.dokumente;

import org.mustangproject.validator.XMLValidator;
import org.mustangproject.validator.ValidationContext;
import org.mustangproject.validator.ESeverity;
import org.mustangproject.validator.IrrecoverableValidationError;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.xml.sax.SAXException;

/** Offline-Validierung und Abgleich mit dem gespeicherten Beleg (A-F-25, A-F-30). */
@Component
public class ERechnungsPruefung {
    static {
        // Gilt auch für die intern vom Validator erzeugten JAXP-Fabriken.
        // Eingebettete Artefakte löst Mustang über seinen Klassenpfad-Resolver auf.
        System.setProperty("javax.xml.accessExternalDTD", "");
        System.setProperty("javax.xml.accessExternalSchema", "");
        System.setProperty("javax.xml.accessExternalStylesheet", "");
        System.setProperty("javax.xml.validation.SchemaFactory:" + XMLConstants.W3C_XML_SCHEMA_NS_URI,
                KlassenpfadSchemaFactory.class.getName());
    }
    public record Fehler(String feld, String meldung) { }

    public List<Fehler> pruefe(Rechnung rechnung, byte[] xml) {
        List<Fehler> fehler = new ArrayList<>();
        try {
            var fabrik = DocumentBuilderFactory.newInstance();
            fabrik.setNamespaceAware(true);
            fabrik.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrik.setFeature("http://xml.org/sax/features/external-general-entities", false);
            fabrik.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            fabrik.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrik.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var dokument = fabrik.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
            var xpath = XPathFactory.newInstance().newXPath();
            String kopf = "//*[local-name()='ApplicableHeaderTradeSettlement']";
            Node summen = (Node) xpath.evaluate(kopf + "/*[local-name()='SpecifiedTradeSettlementHeaderMonetarySummation']",
                    dokument, XPathConstants.NODE);
            vergleiche(fehler, "Nettosumme", rechnung.getSummeNetto(), wert(summen, "LineTotalAmount"));
            vergleiche(fehler, "Steuersumme", rechnung.getSummeSteuer(), wert(summen, "TaxTotalAmount"));
            vergleiche(fehler, "Bruttosumme", rechnung.getSummeBrutto(), wert(summen, "GrandTotalAmount"));
            NodeList steuern = (NodeList) xpath.evaluate(kopf + "/*[local-name()='ApplicableTradeTax']", dokument, XPathConstants.NODESET);
            if (steuern.getLength() != rechnung.steueraufschluesselung().size()) {
                fehler.add(new Fehler("Steuer", "Anzahl der Steuerzeilen stimmt nicht mit dem Beleg überein."));
            }
            for (Steuerzeile zeile : rechnung.steueraufschluesselung()) {
                Node treffer = null;
                for (int i = 0; i < steuern.getLength(); i++) {
                    if (zeile.steuersatz().multiply(BigDecimal.valueOf(100)).compareTo(new BigDecimal(wert(steuern.item(i), "RateApplicablePercent"))) == 0) {
                        if (treffer != null) { fehler.add(new Fehler("Steuer", "Doppelter Steuersatz im XML.")); }
                        treffer = steuern.item(i);
                    }
                }
                vergleiche(fehler, "Steuerbasis", zeile.netto(), wert(treffer, "BasisAmount"));
                vergleiche(fehler, "Steuerbetrag", zeile.steuer(), wert(treffer, "CalculatedAmount"));
            }
            NodeList positionen = (NodeList) xpath.evaluate("//*[local-name()='IncludedSupplyChainTradeLineItem']", dokument, XPathConstants.NODESET);
            if (positionen.getLength() != rechnung.getPositionen().size()) {
                fehler.add(new Fehler("Positionen", "Anzahl der XML-Positionen stimmt nicht mit dem Beleg überein."));
            }
            for (int i = 0; i < Math.min(positionen.getLength(), rechnung.getPositionen().size()); i++) {
                Dokumentposition position = rechnung.getPositionen().get(i);
                Element menge = (Element) xpath.evaluate(".//*[local-name()='BilledQuantity']", positionen.item(i), XPathConstants.NODE);
                vergleiche(fehler, "Menge", BigDecimal.valueOf(position.getMenge()), menge == null ? "" : menge.getTextContent());
                if (menge == null || !Objects.equals(position.getEinheitCode(), menge.getAttribute("unitCode"))) {
                    fehler.add(new Fehler("Einheit", "Die XML-Einheit stimmt nicht mit dem Beleg überein."));
                }
                vergleiche(fehler, "Positionssumme", position.getPositionssummeNetto(), wert(positionen.item(i), "LineTotalAmount"));
            }
            // Instanz und umfangreiche Schematron-Ressourcen entstehen erst beim Export.
            ValidationContext kontext = new ValidationContext(org.slf4j.LoggerFactory.getLogger(ERechnungsPruefung.class));
            XMLValidator validator = new XMLValidator(kontext);
            validator.disableNotices();
            validator.setStringContent(new String(xml, StandardCharsets.UTF_8));
            // XMLValidator lässt interne Fehler durchreichen; der allgemeine Datei-Wrapper
            // kann trotz abgefangener Ausnahme einen zuvor gültigen Teilstatus behalten.
            validator.validate();
            for (var meldung : kontext.getResults()) {
                if (meldung.getSeverity() == ESeverity.error || meldung.getSeverity() == ESeverity.fatal
                        || meldung.getSeverity() == ESeverity.exception) {
                    fehler.add(new Fehler("E-Rechnung", meldung.getMessage()));
                }
            }
            if (!kontext.isValid() || validator.getFiredRules() == 0) {
                fehler.add(new Fehler("E-Rechnung", "Die EN-16931-Prüfung wurde nicht erfolgreich abgeschlossen."));
            }
        } catch (ParserConfigurationException | SAXException
                 | IOException | XPathExpressionException
                 | IrrecoverableValidationError | RuntimeException e) {
            fehler.add(new Fehler("E-Rechnung", "Die XML-Prüfung konnte nicht abgeschlossen werden: " + e.getMessage()));
        }
        return List.copyOf(fehler);
    }

    private static String wert(Node knoten, String name) {
        if (!(knoten instanceof Element element)) { return ""; }
        NodeList werte = element.getElementsByTagNameNS("*", name);
        return werte.getLength() == 0 ? "" : werte.item(0).getTextContent().strip();
    }

    private static void vergleiche(List<Fehler> fehler, String feld, BigDecimal erwartet, String text) {
        try {
            if (erwartet != null && erwartet.compareTo(new BigDecimal(text)) == 0) { return; }
        } catch (NumberFormatException ignored) { }
        fehler.add(new Fehler(feld, "XML und gespeicherter Beleg unterscheiden sich bei '" + feld + "'."));
    }
}
