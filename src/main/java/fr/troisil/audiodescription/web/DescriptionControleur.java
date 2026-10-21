package fr.troisil.audiodescription.web;

import fr.troisil.audiodescription.agent.DescripteurImage;
import fr.troisil.audiodescription.agent.Description;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException;
import fr.troisil.audiodescription.agent.Image;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Reçoit une image et une question facultative, et renvoie leur description.
 * <p>
 * Le contrôleur ne connaît que l'interface {@link DescripteurImage} : il ignore
 * quelle implémentation Spring lui a injectée (Ollama, fausse implémentation…).
 */
@RestController
public class DescriptionControleur {

    private final DescripteurImage descripteur;
    private final ValidateurImage validateurImage;
    private final ValidateurQuestion validateurQuestion;

    public DescriptionControleur(
            DescripteurImage descripteur, ValidateurImage validateurImage, ValidateurQuestion validateurQuestion) {
        this.descripteur = descripteur;
        this.validateurImage = validateurImage;
        this.validateurQuestion = validateurQuestion;
    }

    /**
     * @param fichier  l'image envoyée par l'utilisateur
     * @param question la question facultative ; vide ou absente, elle demande une description générale
     * @return la description, à afficher et à lire à voix haute
     * @throws ImageRefuseeException          si le fichier n'est pas une image valide
     * @throws QuestionTropLongueException    si la question dépasse la longueur autorisée
     * @throws DescriptionImpossibleException si l'agent ne peut pas répondre
     */
    @PostMapping(
            path = "/api/descriptions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DescriptionReponse decrire(
            @RequestParam(name = "image", required = false) MultipartFile fichier,
            @RequestParam(name = "question", required = false) String question)
            throws DescriptionImpossibleException {
        Image image = validateurImage.valider(fichier);
        String questionValidee = validateurQuestion.valider(question);
        Description description = descripteur.decrire(image, questionValidee);
        return new DescriptionReponse(description.texte());
    }
}
