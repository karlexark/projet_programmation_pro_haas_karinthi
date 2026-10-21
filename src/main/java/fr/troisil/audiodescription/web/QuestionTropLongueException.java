package fr.troisil.audiodescription.web;

/**
 * Signale que la question de l'utilisateur dépasse la longueur autorisée.
 * <p>
 * Comme {@link ImageRefuseeException}, c'est une exception non vérifiée : elle traduit
 * une entrée invalide, rattrapée à un seul endroit pour être transformée en message.
 */
public class QuestionTropLongueException extends RuntimeException {

    private final int longueurMaximale;

    public QuestionTropLongueException(int longueur, int longueurMaximale) {
        super("Question de " + longueur + " caractères, " + longueurMaximale + " au maximum");
        this.longueurMaximale = longueurMaximale;
    }

    public int getLongueurMaximale() {
        return longueurMaximale;
    }
}
