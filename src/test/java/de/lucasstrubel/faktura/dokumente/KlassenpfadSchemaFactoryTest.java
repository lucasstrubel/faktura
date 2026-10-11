package de.lucasstrubel.faktura.dokumente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import static org.junit.jupiter.api.Assertions.*;

class KlassenpfadSchemaFactoryTest {
    @Test @DisplayName("ERE-12: Schemaauflösung blockiert Netzwerk, Dateisystem und Pfadmanipulation")
    void externeQuellenGesperrt() throws Exception {
        var fabrik = new KlassenpfadSchemaFactory();
        for (String uri : new String[]{"https://example.invalid/schema/test.xsd", "file:///schema/test.xsd",
                "jar:file:///fremd.jar!/schema/test.xsd", "/schema/../test.xsd"}) {
            assertThrows(IllegalArgumentException.class, () -> fabrik.newSchema(new StreamSource(uri)));
        }
        var resolver = fabrik.getResourceResolver();
        fabrik.setResourceResolver((typ, ns, pub, sys, basis) -> { throw new AssertionError("Fremder Resolver"); });
        assertSame(resolver, fabrik.getResourceResolver());
        for (String systemId : new String[]{"https://example.invalid/test.xsd", "file:///test.xsd", "../test.xsd", "test.xsd"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> resolver.resolveResource(null, null, null, systemId, "https://example.invalid/schema/base.xsd"));
        }
        fabrik.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");
        fabrik.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
        assertEquals("", fabrik.getProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA));
        assertEquals("", fabrik.getProperty(XMLConstants.ACCESS_EXTERNAL_DTD));
    }
}
