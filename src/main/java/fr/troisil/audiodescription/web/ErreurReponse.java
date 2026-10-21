package fr.troisil.audiodescription.web;

/**
 * Le corps JSON renvoyé à la page quand la demande échoue.
 *
 * @param code    un identifiant stable de l'erreur, utilisé par la page, jamais lu à l'utilisateur
 * @param message une phrase destinée à l'utilisateur : ce qui s'est passé et ce qu'il peut faire.
 *                Elle est affichée et lue à voix haute ; elle ne contient ni code HTTP ni détail technique.
 */
public record ErreurReponse(String code, String message) {}
