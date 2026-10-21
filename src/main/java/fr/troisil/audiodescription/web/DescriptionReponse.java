package fr.troisil.audiodescription.web;

/**
 * Le corps JSON renvoyé à la page après une description réussie.
 *
 * @param texte la description ou la réponse à la question, à afficher et à lire
 */
public record DescriptionReponse(String texte) {}
