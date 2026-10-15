package fr.troisil.audiodescription.web;

import java.util.Objects;

/**
 * Signale que le fichier envoyé par l'utilisateur ne peut pas être analysé.
 * <p>
 * C'est une exception non vérifiée : elle traduit une entrée invalide,
 * détectée à la frontière de l'application, et n'est rattrapée qu'à un seul
 * endroit, là où elle est transformée en message pour l'utilisateur.
 */
public class ImageRefuseeException extends RuntimeException {

    /** La raison du refus, pour choisir le message à donner à l'utilisateur. */
    public enum Motif {
        /** Aucun fichier n'a été envoyé. */
        ABSENTE,
        /** Le fichier envoyé est vide. */
        VIDE,
        /** Le fichier n'est pas une image dans un format pris en charge. */
        FORMAT_NON_PRIS_EN_CHARGE,
        /** Le fichier n'a pas pu être lu. */
        ILLISIBLE
    }

    private final Motif motif;

    public ImageRefuseeException(Motif motif, String message) {
        super(message);
        this.motif = Objects.requireNonNull(motif, "motif");
    }

    public ImageRefuseeException(Motif motif, String message, Throwable cause) {
        super(message, cause);
        this.motif = Objects.requireNonNull(motif, "motif");
    }

    public Motif getMotif() {
        return motif;
    }
}
