package de.lucasstrubel.faktura.firma;

import de.lucasstrubel.faktura.gemeinsam.Validierung;
import de.lucasstrubel.faktura.gemeinsam.ValidierungsException;

import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Fachlogik des Firmenprofils: liefert das gespeicherte Profil und
 * validiert Änderungen mit den zentralen Formatregeln (Q-09).
 *
 * <p>Es gibt bewusst keine Voreinstellung mehr: Ein Platzhalter-Aussteller
 * ("Musterstraße 1") auf einer echten Rechnung wäre schlimmer als eine klare
 * Aufforderung, zuerst das Profil zu hinterlegen (A-F-26).
 */
@Service
public class FirmenprofilService {

    private final JdbcFirmenprofilRepository repository;

    public FirmenprofilService(JdbcFirmenprofilRepository repository) {
        this.repository = repository;
    }

    /** Das gespeicherte Profil; leer, solange noch keines hinterlegt wurde. */
    public Optional<Firmenprofil> lade() {
        return Optional.ofNullable(repository.lade());
    }

    /**
     * Profil für einen neuen Beleg (A-F-26): Ohne gespeichertes Profil wird
     * kein Beleg erstellt; eine Rechnung verlangt zusätzlich Steuernummer
     * oder USt-IdNr. (§ 14 Abs. 4 Nr. 2 UStG).
     */
    public Firmenprofil fuerBeleg(boolean rechnung) {
        Firmenprofil profil = lade().orElseThrow(() -> new ValidierungsException("Firmenprofil",
                "Bitte zuerst das Firmenprofil unter 'Einstellungen' hinterlegen — "
                        + "Name und Anschrift des Ausstellers stehen auf jedem Beleg (A-F-26)."));
        if (rechnung && !profil.hatSteuerkennung()) {
            throw new ValidierungsException("Steuernummer",
                    "Für Rechnungen ist im Firmenprofil eine Steuernummer oder USt-IdNr. "
                            + "erforderlich (§ 14 Abs. 4 Nr. 2 UStG, A-F-26).");
        }
        return profil;
    }

    public Firmenprofil speichere(Firmenprofil profil) {
        Firmenprofil bereinigt = profil.bereinigt();
        validiere(bereinigt);
        repository.speichere(bereinigt);
        return bereinigt;
    }

    private static void validiere(Firmenprofil profil) {
        Validierung.pruefePflichtfeld(profil.name(), "Name");
        Validierung.pruefePflichtfeld(profil.strasse(), "Straße");
        Validierung.pruefePflichtfeld(profil.plz(), "PLZ");
        Validierung.pruefePflichtfeld(profil.ort(), "Ort");
        Validierung.pruefePlz(profil.plz());
        Validierung.pruefeUstIdNr(profil.ustIdNr());
        Validierung.pruefeSteuernummer(profil.steuernummer());
        Validierung.pruefeTelefon(profil.telefon());
        Validierung.pruefeEMail(profil.eMail());
        Validierung.pruefeIban(profil.iban());
        Validierung.pruefeBic(profil.bic());
    }
}
