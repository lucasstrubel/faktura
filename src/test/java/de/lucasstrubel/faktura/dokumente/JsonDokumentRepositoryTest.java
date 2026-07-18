package de.lucasstrubel.faktura.dokumente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Persistenz der Belege (IF-01): gespeicherte Belege müssen nach einem
 * Neustart (Neuinstanziierung des Repositories) wieder geladen werden,
 * inklusive der polymorphen Belegtypen (GoBD: lückenlose Erfassung).
 */
class JsonDokumentRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("TC-14: Belege überleben den Neustart und behalten ihren Belegtyp")
    void belegeWerdenNachNeustartGeladen() {
        Path datei = tempDir.resolve("dokumente.json");

        JsonDokumentRepository ersteInstanz = new JsonDokumentRepository(datei);
        ersteInstanz.speichere(TestBelege.rechnung("R-2026-000001", DokumentStatus.OFFEN));
        ersteInstanz.speichere(TestBelege.angebot("AN-2026-000001", DokumentStatus.ENTWURF));

        JsonDokumentRepository neustart = new JsonDokumentRepository(datei);
        assertEquals(2, neustart.alle().size());
        assertInstanceOf(Rechnung.class, neustart.findeNachNummer("R-2026-000001"));
        assertInstanceOf(Angebot.class, neustart.findeNachNummer("AN-2026-000001"));
        assertNotNull(neustart.findeNachNummer("R-2026-000001").getPositionen());
    }
}
