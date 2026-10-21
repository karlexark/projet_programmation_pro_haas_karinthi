package fr.troisil.audiodescription.agent.langchain4j;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.InternalServerException;
import dev.langchain4j.exception.ModelNotFoundException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.exception.UnresolvedModelServerException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import fr.troisil.audiodescription.agent.DescripteurImage;
import fr.troisil.audiodescription.agent.Description;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException.Raison;
import fr.troisil.audiodescription.agent.Image;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Implémentation de {@link DescripteurImage} qui interroge un modèle de vision à travers LangChain4J.
 * <p>
 * C'est un <em>adaptateur</em> : il traduit les types de l'application ({@link Image},
 * {@link Description}) en messages LangChain4J, et les erreurs de la bibliothèque en
 * {@link DescriptionImpossibleException}. Aucune classe de LangChain4J ne sort de ce package.
 * <p>
 * Il ne dépend d'aucun fournisseur : il reçoit un {@link ChatModel}, construit par la
 * configuration du fournisseur choisi (Ollama aujourd'hui, un autre demain). Ajouter un
 * fournisseur revient à écrire une configuration, sans modifier cette classe.
 */
public final class DescripteurLangChain4j implements DescripteurImage {

    private final ChatModel modele;

    public DescripteurLangChain4j(ChatModel modele) {
        this.modele = Objects.requireNonNull(modele, "modele");
    }

    @Override
    public Description decrire(Image image, String question) throws DescriptionImpossibleException {
        Objects.requireNonNull(image, "image");
        String texte = interroger(messages(image, question));
        if (texte == null || texte.isBlank()) {
            throw new DescriptionImpossibleException(Raison.REPONSE_VIDE, "Le modèle a renvoyé une réponse vide");
        }
        return new Description(texte);
    }

    private static List<ChatMessage> messages(Image image, String question) {
        String base64 = Base64.getEncoder().encodeToString(image.octets());
        return List.of(
                SystemMessage.from(Consigne.SYSTEME),
                UserMessage.from(
                        TextContent.from(Consigne.demande(question)),
                        ImageContent.from(base64, image.typeMime())));
    }

    private String interroger(List<ChatMessage> messages) throws DescriptionImpossibleException {
        try {
            ChatResponse reponse = modele.chat(messages);
            return reponse == null || reponse.aiMessage() == null ? null : reponse.aiMessage().text();
        } catch (RuntimeException e) {
            throw new DescriptionImpossibleException(raison(e), "Échec de l'appel au modèle : " + e.getMessage(), e);
        }
    }

    /**
     * Classe l'erreur de la bibliothèque. Selon la version de LangChain4J et le client HTTP,
     * la cause réelle (délai, connexion refusée…) peut être enveloppée : on parcourt donc
     * toute la chaîne des causes.
     */
    static Raison raison(Throwable erreur) {
        for (Throwable cause = erreur; cause != null; cause = cause.getCause()) {
            if (cause instanceof TimeoutException
                    || cause instanceof HttpTimeoutException
                    || cause instanceof SocketTimeoutException) {
                return Raison.DELAI_DEPASSE;
            }
            if (cause instanceof ConnectException
                    || cause instanceof UnresolvedModelServerException
                    || cause instanceof UnresolvedAddressException
                    || cause instanceof ModelNotFoundException
                    || cause instanceof InternalServerException
                    // Erreurs propres aux services en ligne (clé refusée, quota atteint).
                    || cause instanceof AuthenticationException
                    || cause instanceof RateLimitException) {
                return Raison.AGENT_INDISPONIBLE;
            }
        }
        return Raison.INCONNUE;
    }
}
