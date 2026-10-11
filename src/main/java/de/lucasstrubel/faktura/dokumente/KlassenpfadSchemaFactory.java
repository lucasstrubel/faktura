package de.lucasstrubel.faktura.dokumente;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.mustangproject.validator.XMLValidator;

/**
 * JAXP-Adapter für die E-Rechnung-Prüfung (A-F-30): lädt ausschließlich
 * Mustang-Schemas aus dem gebündelten Klassenpfad, nie aus dem Netz. Wird von
 * {@link ERechnungsPruefung} JVM-weit als XSD-{@code SchemaFactory} registriert.
 */
public final class KlassenpfadSchemaFactory extends SchemaFactory {
    private final SchemaFactory fabrik = SchemaFactory.newDefaultInstance();

    public KlassenpfadSchemaFactory() {
        try {
            fabrik.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrik.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var dom = DocumentBuilderFactory.newDefaultInstance()
                    .newDocumentBuilder().getDOMImplementation();
            DOMImplementationLS ls = (DOMImplementationLS) dom.getFeature("LS", "3.0");
            fabrik.setResourceResolver((typ, namensraum, publicId, systemId, basis) -> {
                if (systemId == null || !systemId.matches("[A-Za-z0-9_.-]+\\.xsd")) {
                    throw new IllegalArgumentException("Externe Schemaressource blockiert: " + systemId);
                }
                String basisPfad = schemaPfad(basis);
                // Mustang 2.24.0: zwei RAM-Imports verwenden den älteren Dateipräfix.
                // Exakte Aliase bleiben innerhalb desselben gepinnten EN-16931-Pakets.
                if (basisPfad.startsWith("/schema/ZF_250/EN16931/")) {
                    systemId = switch (systemId) {
                        case "Factur-X_1.09_EN16931_urn_un_unece_uncefact_data_standard_QualifiedDataType_100.xsd" ->
                                "FACTUR-X_EN16931_urn_un_unece_uncefact_data_standard_QualifiedDataType_100.xsd";
                        case "Factur-X_1.09_EN16931_urn_un_unece_uncefact_data_standard_UnqualifiedDataType_100.xsd" ->
                                "FACTUR-X_EN16931_urn_un_unece_uncefact_data_standard_UnqualifiedDataType_100.xsd";
                        default -> systemId;
                    };
                }
                String pfad = basisPfad.substring(0, basisPfad.lastIndexOf('/') + 1) + systemId;
                var ressource = XMLValidator.class.getResource(pfad);
                if (ressource == null) { throw new IllegalArgumentException("Gebündeltes Schema fehlt: " + pfad); }
                var eingabe = ls.createLSInput();
                eingabe.setPublicId(publicId);
                eingabe.setSystemId(ressource.toExternalForm());
                try { eingabe.setByteStream(ressource.openStream()); }
                catch (IOException e) { throw new UncheckedIOException(e); }
                return eingabe;
            });
        } catch (SAXException | ParserConfigurationException e) {
            throw new IllegalStateException("Sichere Schemafabrik nicht verfügbar", e);
        }
    }

    private static String schemaPfad(String uri) {
        for (String intern : new String[]{"/schemas/xml.xsd", "/external/schemas/svrl.xsd"}) {
            var ressource = KlassenpfadSchemaFactory.class.getResource(intern);
            if (ressource != null && ressource.toExternalForm().equals(uri)) { return intern; }
        }
        int anfang = uri == null ? -1 : uri.indexOf("/schema/");
        if (anfang < 0 || uri.contains("..")) { throw new IllegalArgumentException("Schema außerhalb des Pakets: " + uri); }
        String pfad = uri.substring(anfang);
        var erlaubt = XMLValidator.class.getResource(pfad);
        if (erlaubt == null || !erlaubt.toExternalForm().equals(uri)) {
            throw new IllegalArgumentException("Fremde Schemaquelle blockiert");
        }
        return pfad;
    }

    @Override public Schema newSchema(Source[] quellen) throws SAXException {
        for (Source quelle : quellen) { schemaPfad(quelle.getSystemId()); }
        return fabrik.newSchema(quellen);
    }
    @Override public Schema newSchema() throws SAXException { return fabrik.newSchema(); }
    @Override public boolean isSchemaLanguageSupported(String sprache) { return fabrik.isSchemaLanguageSupported(sprache); }
    @Override public void setErrorHandler(ErrorHandler handler) { fabrik.setErrorHandler(handler); }
    @Override public ErrorHandler getErrorHandler() { return fabrik.getErrorHandler(); }
    @Override public LSResourceResolver getResourceResolver() { return fabrik.getResourceResolver(); }
    @Override public void setResourceResolver(LSResourceResolver resolver) {
        // Die feste Klassenpfad-Allowlist darf vom aufrufenden Validator nicht ersetzt werden.
    }
    @Override public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException { return fabrik.getFeature(name); }
    @Override public void setFeature(String name, boolean wert) throws SAXNotRecognizedException, SAXNotSupportedException { fabrik.setFeature(name, wert); }
    @Override public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        // Xerces akzeptiert diese Schreib-Properties, bietet aber keinen Getter dafür.
        if (name.equals(XMLConstants.ACCESS_EXTERNAL_SCHEMA) || name.equals(XMLConstants.ACCESS_EXTERNAL_DTD)) { return ""; }
        return fabrik.getProperty(name);
    }
    @Override public void setProperty(String name, Object wert) throws SAXNotRecognizedException, SAXNotSupportedException {
        fabrik.setProperty(name, name.equals(XMLConstants.ACCESS_EXTERNAL_SCHEMA) || name.equals(XMLConstants.ACCESS_EXTERNAL_DTD) ? "" : wert);
    }
}
