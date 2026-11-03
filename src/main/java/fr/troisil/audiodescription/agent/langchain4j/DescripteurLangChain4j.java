package fr.troisil.audiodescription.agent.langchain4j;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.InternalServerException;
import dev.langchain4j.exception.ModelNotFoundException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.exception.UnresolvedModelServerException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import fr.troisil.audiodescription.agent.DescripteurImage;
import fr.troisil.audiodescription.agent.Description;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException.Raison;
import fr.troisil.audiodescription.agent.Image;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p>
 * C'est un <em>agent</em> : le modèle peut appeler des outils ({@link OutilsAgent}) avant de
 * répondre, par exemple pour connaître la date du jour.
 */
public final class DescripteurLangChain4j implements DescripteurImage {

    /**
     * Nombre maximal d'allers-retours avec le modèle pour une même demande. Il évite qu'un modèle
     * qui redemande sans cesse un outil bloque la réponse indéfiniment.
     */
    static final int TOURS_MAXIMUM = 4;

    private static final Logger JOURNAL = LoggerFactory.getLogger(DescripteurLangChain4j.class);

    private final ChatModel modele;
    private final List<ToolSpecification> specificationsOutils;
    private final Map<String, ToolExecutor> executeursOutils;

    /**
     * @param modele le modèle de vision, jamais null
     * @param outils les outils que l'agent peut appeler (méthodes annotées {@code @Tool}), jamais null
     */
    public DescripteurLangChain4j(ChatModel modele, OutilsAgent outils) {
        this.modele = Objects.requireNonNull(modele, "modele");
        Objects.requireNonNull(outils, "outils");
        this.specificationsOutils = ToolSpecifications.toolSpecificationsFrom(outils);
        this.executeursOutils = executeurs(outils);
    }

    @Override
    public Description decrire(Image image, String question) throws DescriptionImpossibleException {
        Objects.requireNonNull(image, "image");
        String texte = dialoguer(messages(image, question));
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
                        TextContent.from(Consigne.demande(question)), ImageContent.from(base64, image.typeMime())));
    }

    /**
     * La boucle de l'agent : le modèle répond, ou demande d'abord un ou plusieurs outils.
     * Dans ce cas, on exécute chaque outil, on lui renvoie les résultats, et il reprend.
     */
    private String dialoguer(List<ChatMessage> messagesInitiaux) throws DescriptionImpossibleException {
        List<ChatMessage> conversation = new ArrayList<>(messagesInitiaux);
        for (int tour = 0; tour < TOURS_MAXIMUM; tour++) {
            AiMessage reponse = interroger(conversation);
            if (reponse == null || !reponse.hasToolExecutionRequests()) {
                return reponse == null ? null : reponse.text();
            }
            conversation.add(reponse);
            for (ToolExecutionRequest demande : reponse.toolExecutionRequests()) {
                conversation.add(ToolExecutionResultMessage.from(demande, executer(demande)));
            }
        }
        throw new DescriptionImpossibleException(
                Raison.INCONNUE, "Le modèle demande encore un outil après " + TOURS_MAXIMUM + " tours");
    }

    private AiMessage interroger(List<ChatMessage> conversation) throws DescriptionImpossibleException {
        ChatRequest requete = ChatRequest.builder()
                // Copie : la conversation continue d'évoluer après cet appel.
                .messages(List.copyOf(conversation))
                .toolSpecifications(specificationsOutils)
                .build();
        try {
            ChatResponse reponse = modele.chat(requete);
            return reponse == null ? null : reponse.aiMessage();
        } catch (RuntimeException e) {
            throw new DescriptionImpossibleException(raison(e), "Échec de l'appel au modèle : " + e.getMessage(), e);
        }
    }

    /** Exécute un outil demandé par le modèle. Un outil inconnu lui est signalé, sans faire échouer la demande. */
    private String executer(ToolExecutionRequest demande) {
        ToolExecutor executeur = executeursOutils.get(demande.name());
        if (executeur == null) {
            JOURNAL.warn("Le modèle a demandé un outil inconnu : {}", demande.name());
            return "Outil inconnu : " + demande.name();
        }
        String resultat = executeur.execute(demande, null);
        JOURNAL.info("Outil {} appelé par l'agent : {}", demande.name(), resultat);
        return resultat;
    }

    private static Map<String, ToolExecutor> executeurs(OutilsAgent outils) {
        Map<String, ToolExecutor> executeurs = new HashMap<>();
        for (Method methode : outils.getClass().getDeclaredMethods()) {
            if (methode.isAnnotationPresent(Tool.class)) {
                executeurs.put(ToolSpecifications.toolNameFrom(methode), new DefaultToolExecutor(outils, methode));
            }
        }
        return Map.copyOf(executeurs);
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
