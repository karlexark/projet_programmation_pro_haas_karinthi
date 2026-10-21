package fr.troisil.audiodescription.web;

import fr.troisil.audiodescription.agent.DescriptionImpossibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Traduit les erreurs en réponses compréhensibles par une personne qui ne voit pas l'écran.
 * <p>
 * Chaque message dit ce qui s'est passé et ce que l'utilisateur peut faire ensuite,
 * sans code HTTP ni trace d'exception. Le détail technique est écrit dans les journaux
 * du serveur, jamais renvoyé à la page.
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger JOURNAL = LoggerFactory.getLogger(GestionnaireErreurs.class);

    private final DataSize tailleMaximale;

    public GestionnaireErreurs(@Value("${spring.servlet.multipart.max-file-size}") DataSize tailleMaximale) {
        this.tailleMaximale = tailleMaximale;
    }

    @ExceptionHandler(ImageRefuseeException.class)
    public ResponseEntity<ErreurReponse> imageRefusee(ImageRefuseeException e) {
        JOURNAL.info("Image refusée : {}", e.getMessage());
        String message = switch (e.getMotif()) {
            case ABSENTE -> "Aucune image n'a été choisie. Choisissez une image, puis relancez la description.";
            case VIDE -> "Le fichier choisi est vide. Choisissez une autre image.";
            case FORMAT_NON_PRIS_EN_CHARGE ->
                "Ce fichier n'est pas une image que l'application sait lire. "
                        + "Choisissez une photo au format JPEG ou PNG.";
            case ILLISIBLE -> "Le fichier n'a pas pu être lu. Choisissez-le de nouveau, ou essayez une autre image.";
        };
        return reponse(HttpStatus.BAD_REQUEST, e.getMotif().name(), message);
    }

    @ExceptionHandler(QuestionTropLongueException.class)
    public ResponseEntity<ErreurReponse> questionTropLongue(QuestionTropLongueException e) {
        JOURNAL.info("Question refusée : {}", e.getMessage());
        return reponse(
                HttpStatus.BAD_REQUEST,
                "QUESTION_TROP_LONGUE",
                "Votre question est trop longue : " + e.getLongueurMaximale()
                        + " caractères au maximum. Raccourcissez-la, puis relancez la description.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErreurReponse> imageTropLourde(MaxUploadSizeExceededException e) {
        JOURNAL.info("Image trop lourde : {}", e.getMessage());
        return reponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "TROP_LOURDE",
                "L'image est trop lourde. Choisissez une image de moins de " + tailleMaximale.toMegabytes()
                        + " mégaoctets.");
    }

    @ExceptionHandler(DescriptionImpossibleException.class)
    public ResponseEntity<ErreurReponse> descriptionImpossible(DescriptionImpossibleException e) {
        JOURNAL.warn("Description impossible ({}) : {}", e.getRaison(), e.getMessage(), e);
        return switch (e.getRaison()) {
            case AGENT_INDISPONIBLE ->
                reponse(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        e.getRaison().name(),
                        "Le service de description ne répond pas pour le moment. Réessayez dans quelques minutes.");
            case DELAI_DEPASSE ->
                reponse(
                        HttpStatus.GATEWAY_TIMEOUT,
                        e.getRaison().name(),
                        "L'analyse de l'image a pris trop de temps. Réessayez, ou choisissez une image plus simple.");
            case REPONSE_VIDE ->
                reponse(
                        HttpStatus.BAD_GATEWAY,
                        e.getRaison().name(),
                        "Aucune description n'a pu être produite pour cette image. "
                                + "Réessayez, ou choisissez une autre image.");
            case INCONNUE -> erreurInattendue();
        };
    }

    /**
     * Toute autre exception.
     * <p>
     * Les exceptions web de Spring (ressource introuvable, méthode non autorisée, format de
     * requête non pris en charge…) implémentent {@link ErrorResponse} et portent leur propre
     * code HTTP : ce ne sont pas des pannes du serveur. On garde ce code, sans trace d'erreur
     * dans les journaux. Seules les vraies erreurs inattendues donnent un 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurReponse> inattendue(Exception e) {
        if (e instanceof ErrorResponse erreurWeb) {
            JOURNAL.debug("Requête refusée par Spring : {}", e.getMessage());
            return ResponseEntity.status(erreurWeb.getStatusCode())
                    .body(new ErreurReponse(
                            "REQUETE_INVALIDE",
                            "La demande n'a pas pu être traitée. Rechargez la page, puis réessayez."));
        }
        JOURNAL.error("Erreur inattendue", e);
        return erreurInattendue();
    }

    private static ResponseEntity<ErreurReponse> erreurInattendue() {
        return reponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INATTENDUE",
                "Une erreur inattendue s'est produite. "
                        + "Réessayez ; si le problème continue, choisissez une autre image.");
    }

    private static ResponseEntity<ErreurReponse> reponse(HttpStatus statut, String code, String message) {
        return ResponseEntity.status(statut).body(new ErreurReponse(code, message));
    }
}
