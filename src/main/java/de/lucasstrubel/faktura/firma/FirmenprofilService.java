package de.lucasstrubel.faktura.firma;

import de.lucasstrubel.faktura.gemeinsam.Validierung;

import org.springframework.stereotype.Service;

/**
 * Fachlogik des Firmenprofils: liefert das gespeicherte Profil (oder die
 * Voreinstellung, solange keines existiert) und validiert Änderungen mit
 * den zentralen Formatregeln (Q-09).
 */
@Service
public class FirmenprofilService {

    private final JdbcFirmenprofilRepository repository;

    public FirmenprofilService(JdbcFirmenprofilRepository repository) {
        this.repository = repository;
    }

    /** Aktuelles Profil; {@link Firmenprofil#standard()} als Voreinstellung. */
    public Firmenprofil lade() {
        Firmenprofil profil = repository.lade();
        return profil == null ? Firmenprofil.standard() : profil;
    }

    public Firmenprofil speichere(Firmenprofil profil) {
        validiere(profil);
        repository.speichere(profil);
        return profil;
    }

    private void validiere(Firmenprofil profil) {
        Validierung.pruefePflichtfeld(profil.name(), "Name");
        Validierung.pruefePflichtfeld(profil.strasse(), "Straße");
        Validierung.pruefePflichtfeld(profil.plz(), "PLZ");
        Validierung.pruefePflichtfeld(profil.ort(), "Ort");
        Validierung.pruefePlz(profil.plz());
        Validierung.pruefeUstIdNr(profil.ustIdNr());
        Validierung.pruefeTelefon(profil.telefon());
        Validierung.pruefeEMail(profil.eMail());
        Validierung.pruefeIban(profil.iban());
    }
}
