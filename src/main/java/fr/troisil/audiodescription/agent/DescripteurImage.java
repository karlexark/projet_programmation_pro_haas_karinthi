package fr.troisil.audiodescription.agent;

/**
 * Un agent capable de décrire une image à une personne qui ne la voit pas.
 * <p>
 * C'est le seul point de contact entre l'application web et l'agent :
 * le contrôleur ne connaît que cette interface, jamais l'implémentation
 * (Ollama, fausse implémentation, autre fournisseur).
 *
 * <h2>Préconditions</h2>
 * La validation de ce qu'envoie l'utilisateur (fichier présent, taille,
 * format pris en charge) est faite <strong>par l'appelant</strong>, avant
 * l'appel. L'agent reçoit donc toujours une image valide ; il n'a pas à
 * re-vérifier le format.
 */
public interface DescripteurImage {

    /**
     * Décrit le contenu d'une image en français, en quelques phrases,
     * ou répond à la question posée sur cette image.
     * <p>
     * Sans question, la description donne d'abord l'essentiel (ce que
     * représente l'image), puis les détails utiles, et lit le texte présent
     * dans l'image. Avec une question, la réponse s'y concentre.
     *
     * @param image    l'image à décrire, jamais null ; son format a déjà été
     *                 validé par l'appelant
     * @param question la question de l'utilisateur, ou {@code null} pour une
     *                 description générale. Une question vide ou faite
     *                 d'espaces est traitée comme {@code null}.
     * @return la description ou la réponse, jamais null ni vide
     * @throws DescriptionImpossibleException si l'agent ne peut pas répondre
     *                                        (modèle indisponible, délai dépassé, réponse vide…)
     * @throws NullPointerException           si {@code image} est null
     */
    Description decrire(Image image, String question) throws DescriptionImpossibleException;
}
