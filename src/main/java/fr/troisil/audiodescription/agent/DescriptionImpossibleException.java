package fr.troisil.audiodescription.agent;

import java.util.Objects;

/**
 * Signale que l'agent n'a pas pu décrire l'image, pour une raison extérieure
 * au code de l'application (modèle arrêté, délai dépassé, réponse vide…).
 * <p>
 * C'est une exception <em>vérifiée</em> : ces échecs sont prévisibles et
 * l'appelant doit obligatoirement prévoir quoi dire à l'utilisateur.
 */
public class DescriptionImpossibleException extends Exception {

    /**
     * La cause de l'échec, exprimée sans référence au fournisseur,
     * pour que l'appelant puisse choisir le message à donner à l'utilisateur.
     */
    public enum Raison {
        /** Le modèle ne répond pas : service arrêté ou injoignable. */
        AGENT_INDISPONIBLE,
        /** Le modèle a mis trop de temps à répondre. */
        DELAI_DEPASSE,
        /** Le modèle a répondu, mais sans texte exploitable. */
        REPONSE_VIDE,
        /** Toute autre erreur de l'agent. */
        INCONNUE
    }

    private final Raison raison;

    public DescriptionImpossibleException(Raison raison, String message) {
        super(message);
        this.raison = Objects.requireNonNull(raison, "raison");
    }

    public DescriptionImpossibleException(Raison raison, String message, Throwable cause) {
        super(message, cause);
        this.raison = Objects.requireNonNull(raison, "raison");
    }

    public Raison getRaison() {
        return raison;
    }
}
