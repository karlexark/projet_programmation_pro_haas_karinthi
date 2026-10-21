package fr.troisil.audiodescription.web;

import fr.troisil.audiodescription.agent.Image;
import fr.troisil.audiodescription.web.ImageRefuseeException.Motif;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Vérifie le fichier envoyé par l'utilisateur et le transforme en {@link Image}.
 * <p>
 * C'est ici que sont contrôlées les préconditions de
 * {@link fr.troisil.audiodescription.agent.DescripteurImage#decrire} : l'agent
 * ne reçoit que des images valides.
 * <p>
 * Le nom et l'extension du fichier ne sont pas pris en compte. Le type annoncé
 * par le navigateur doit être un format accepté, et le contenu doit commencer
 * par la signature d'un format accepté : un fichier texte renommé en
 * {@code .jpg} est refusé.
 * <p>
 * La taille maximale n'est pas vérifiée ici : elle est imposée par Spring
 * ({@code spring.servlet.multipart.max-file-size}).
 */
@Component
public class ValidateurImage {

    /** Formats acceptés, avec la signature (premiers octets) de leur contenu. */
    private static final Map<String, byte[]> SIGNATURES = Map.of(
            "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

    /**
     * @param fichier le fichier reçu, éventuellement null
     * @return l'image prête à être décrite, avec son type réel
     * @throws ImageRefuseeException si le fichier est absent, vide, illisible
     *                               ou n'est pas une image JPEG ou PNG
     */
    public Image valider(MultipartFile fichier) {
        if (fichier == null) {
            throw new ImageRefuseeException(Motif.ABSENTE, "Aucun fichier reçu");
        }
        if (fichier.isEmpty()) {
            throw new ImageRefuseeException(Motif.VIDE, "Le fichier reçu est vide");
        }
        String typeAnnonce = fichier.getContentType();
        if (typeAnnonce == null || !SIGNATURES.containsKey(typeAnnonce)) {
            throw new ImageRefuseeException(
                    Motif.FORMAT_NON_PRIS_EN_CHARGE, "Type annoncé non pris en charge : " + typeAnnonce);
        }
        byte[] octets = lire(fichier);
        String typeReel = detecterType(octets)
                .orElseThrow(() -> new ImageRefuseeException(
                        Motif.FORMAT_NON_PRIS_EN_CHARGE, "Le contenu ne correspond à aucun format pris en charge"));
        return new Image(octets, typeReel);
    }

    private static byte[] lire(MultipartFile fichier) {
        try {
            return fichier.getBytes();
        } catch (IOException e) {
            throw new ImageRefuseeException(Motif.ILLISIBLE, "Lecture du fichier impossible", e);
        }
    }

    private static Optional<String> detecterType(byte[] octets) {
        return SIGNATURES.entrySet().stream()
                .filter(entree -> commencePar(octets, entree.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static boolean commencePar(byte[] octets, byte[] signature) {
        return octets.length >= signature.length
                && Arrays.equals(octets, 0, signature.length, signature, 0, signature.length);
    }
}
