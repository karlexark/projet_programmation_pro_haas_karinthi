package fr.troisil.audiodescription.web;

import org.springframework.stereotype.Component;

/**
 * Vérifie et normalise la question facultative envoyée avec l'image.
 * <p>
 * La page limite déjà la saisie ({@code maxlength} du champ), mais une requête peut être
 * envoyée sans passer par la page : le serveur vérifie donc lui-même la longueur.
 */
@Component
public class ValidateurQuestion {

    /** Longueur maximale d'une question, identique au {@code maxlength} du champ de la page. */
    public static final int LONGUEUR_MAXIMALE = 500;

    /**
     * @param question la question reçue, éventuellement null
     * @return la question sans les espaces autour, ou null si elle est absente, vide
     *         ou faite d'espaces (description générale)
     * @throws QuestionTropLongueException si la question dépasse {@link #LONGUEUR_MAXIMALE} caractères
     */
    public String valider(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }
        String nettoyee = question.strip();
        if (nettoyee.length() > LONGUEUR_MAXIMALE) {
            throw new QuestionTropLongueException(nettoyee.length(), LONGUEUR_MAXIMALE);
        }
        return nettoyee;
    }
}
