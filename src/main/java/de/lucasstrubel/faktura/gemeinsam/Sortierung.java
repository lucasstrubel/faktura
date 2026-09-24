package de.lucasstrubel.faktura.gemeinsam;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Alphabetische Sortierung nach deutschen Regeln für Namen und
 * Bezeichnungen (C-F-13, B-F-13): Umlaute und Buchstaben wie „Ł“ oder „Ş“
 * stehen bei ihrem Grundbuchstaben, Groß-/Kleinschreibung spielt keine
 * Rolle. Ein reiner Zeichencode-Vergleich sortierte „Łukasz“ hinter „Z“.
 */
public final class Sortierung {

    /** Vergleicht Texte wie ein deutsches Telefonbuch; {@code null} zuletzt. */
    public static final Comparator<String> DEUTSCH = Comparator.nullsLast(erzeuge());

    private Sortierung() {
    }

    private static Comparator<String> erzeuge() {
        Collator collator = Collator.getInstance(Locale.GERMANY);
        collator.setStrength(Collator.SECONDARY);
        return collator::compare;
    }
}
