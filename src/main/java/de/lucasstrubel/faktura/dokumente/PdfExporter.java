package de.lucasstrubel.faktura.dokumente;

import java.nio.file.Path;

/**
 * PDF-Export eines Belegs in das lokale Dateisystem
 * (IF-01; A-F-04, F-07, F-10, F-15).
 */
public interface PdfExporter {

    void exportiere(Dokument dokument, Path zielDatei);
}
