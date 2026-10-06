package fr.troisil.audiodescription.agent;

import java.util.Objects;

/**
 * La description d'une image, ou la réponse à une question posée sur elle,
 * telle qu'elle sera affichée et lue à voix haute.
 *
 * @param texte le texte en français, jamais null ni blanc
 */
public record Description(String texte) {

    /**
     * @throws NullPointerException     si {@code texte} est null
     * @throws IllegalArgumentException si {@code texte} est blanc
     */
    public Description {
        Objects.requireNonNull(texte, "texte");
        if (texte.isBlank()) {
            throw new IllegalArgumentException("La description ne doit pas être vide");
        }
        texte = texte.strip();
    }
}
