package fr.troisil.audiodescription.agent;

import java.util.Arrays;
import java.util.Objects;

/**
 * Une image à décrire : son contenu brut et son type MIME.
 * <p>
 * Ce type appartient à l'application et ne dépend d'aucune bibliothèque d'IA :
 * c'est à chaque implémentation de {@link DescripteurImage} de le convertir
 * dans le format attendu par son fournisseur.
 * <p>
 * Le tableau d'octets est copié à la construction et à la lecture, pour que
 * l'image ne puisse pas être modifiée une fois créée.
 *
 * @param octets   le contenu du fichier, jamais null ni vide
 * @param typeMime le type MIME de l'image (par exemple {@code image/jpeg}), jamais null ni blanc
 */
public record Image(byte[] octets, String typeMime) {

    /**
     * @throws NullPointerException     si {@code octets} ou {@code typeMime} est null
     * @throws IllegalArgumentException si {@code octets} est vide ou {@code typeMime} est blanc
     */
    public Image {
        Objects.requireNonNull(octets, "octets");
        Objects.requireNonNull(typeMime, "typeMime");
        if (octets.length == 0) {
            throw new IllegalArgumentException("L'image ne doit pas être vide");
        }
        if (typeMime.isBlank()) {
            throw new IllegalArgumentException("Le type MIME ne doit pas être blanc");
        }
        octets = octets.clone();
    }

    /**
     * @return une copie du contenu de l'image
     */
    @Override
    public byte[] octets() {
        return octets.clone();
    }

    @Override
    public boolean equals(Object autre) {
        return autre instanceof Image image
                && Arrays.equals(octets, image.octets)
                && typeMime.equals(image.typeMime);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(octets) + typeMime.hashCode();
    }

    @Override
    public String toString() {
        return "Image[" + typeMime + ", " + octets.length + " octets]";
    }
}
