package fr.troisil.audiodescription.agent.langchain4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.LangChain4jException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import fr.troisil.audiodescription.agent.Description;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException.Raison;
import fr.troisil.audiodescription.agent.Image;
import java.net.ConnectException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class DescripteurLangChain4jTest {

    private static final byte[] OCTETS = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2};
    private static final Image IMAGE = new Image(OCTETS, "image/jpeg");

    /** Le mardi 3 novembre 2026 à 14 h 05, heure de Paris. */
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-03T13:05:00Z"), ZoneId.of("Europe/Paris"));

    private static final String NOM_OUTIL_DATE = "dateEtHeureActuelles";

    private final ChatModel modele = mock(ChatModel.class);
    private final DescripteurLangChain4j descripteur = new DescripteurLangChain4j(modele, new OutilsAgent(HORLOGE));

    @Test
    void renvoieLeTexteDuModele() throws DescriptionImpossibleException {
        when(modele.chat(any(ChatRequest.class))).thenReturn(texte("  Un chat dort sur un canapé rouge.  "));

        Description description = descripteur.decrire(IMAGE, null);

        assertThat(description.texte()).isEqualTo("Un chat dort sur un canapé rouge.");
    }

    @Test
    void envoieLaConsigneEtLImageEnBase64() throws DescriptionImpossibleException {
        when(modele.chat(any(ChatRequest.class))).thenReturn(texte("ok"));

        descripteur.decrire(IMAGE, null);

        List<ChatMessage> messages = requetesEnvoyees(1).get(0).messages();
        assertThat(messages).hasSize(2);
        assertThat(((SystemMessage) messages.get(0)).text()).isEqualTo(Consigne.SYSTEME);
        UserMessage demande = (UserMessage) messages.get(1);
        assertThat(((TextContent) demande.contents().get(0)).text()).isEqualTo(Consigne.DESCRIPTION_GENERALE);
        ImageContent image = (ImageContent) demande.contents().get(1);
        assertThat(image.image().base64Data()).isEqualTo(Base64.getEncoder().encodeToString(OCTETS));
        assertThat(image.image().mimeType()).isEqualTo("image/jpeg");
    }

    @Test
    void transmetLaQuestionAuModele() throws DescriptionImpossibleException {
        when(modele.chat(any(ChatRequest.class))).thenReturn(texte("ok"));

        descripteur.decrire(IMAGE, "Quelle est la date de péremption ?");

        UserMessage demande =
                (UserMessage) requetesEnvoyees(1).get(0).messages().get(1);
        assertThat(((TextContent) demande.contents().get(0)).text()).contains("« Quelle est la date de péremption ? »");
    }

    @Test
    void presenteLOutilDateAuModele() throws DescriptionImpossibleException {
        when(modele.chat(any(ChatRequest.class))).thenReturn(texte("ok"));

        descripteur.decrire(IMAGE, null);

        assertThat(requetesEnvoyees(1).get(0).toolSpecifications())
                .extracting(ToolSpecification::name)
                .containsExactly(NOM_OUTIL_DATE);
    }

    @Test
    void executeLOutilDemandeEtRenvoieSonResultatAuModele() throws DescriptionImpossibleException {
        when(modele.chat(any(ChatRequest.class)))
                .thenReturn(demandeOutil(NOM_OUTIL_DATE))
                .thenReturn(texte("Ce yaourt est périmé depuis le 18 août 2024."));

        Description description = descripteur.decrire(IMAGE, "Ce yaourt est-il périmé ?");

        assertThat(description.texte()).isEqualTo("Ce yaourt est périmé depuis le 18 août 2024.");
        List<ChatMessage> secondeConversation = requetesEnvoyees(2).get(1).messages();
        assertThat(secondeConversation).hasSize(4);
        assertThat(secondeConversation.get(2)).isInstanceOf(AiMessage.class);
        ToolExecutionResultMessage resultat = (ToolExecutionResultMessage) secondeConversation.get(3);
        assertThat(resultat.toolName()).isEqualTo(NOM_OUTIL_DATE);
        assertThat(resultat.text()).isEqualTo("Nous sommes le mardi 3 novembre 2026 à 14 h 05.");
    }

    @Test
    void unOutilInconnuEstSignaleAuModeleSansFaireEchouerLaDemande() throws DescriptionImpossibleException {
        when(modele.chat(any(ChatRequest.class)))
                .thenReturn(demandeOutil("meteo"))
                .thenReturn(texte("ok"));

        descripteur.decrire(IMAGE, null);

        ToolExecutionResultMessage resultat = (ToolExecutionResultMessage)
                requetesEnvoyees(2).get(1).messages().get(3);
        assertThat(resultat.text()).isEqualTo("Outil inconnu : meteo");
    }

    @Test
    void unModeleQuiRedemandeSansFinUnOutilEstArrete() {
        when(modele.chat(any(ChatRequest.class))).thenReturn(demandeOutil(NOM_OUTIL_DATE));

        assertEchec(Raison.INCONNUE);
        verify(modele, times(DescripteurLangChain4j.TOURS_MAXIMUM)).chat(any(ChatRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n"})
    void uneReponseVideEstUnEchec(String texte) {
        when(modele.chat(any(ChatRequest.class))).thenReturn(texte(texte));

        assertEchec(Raison.REPONSE_VIDE);
    }

    @Test
    void unModeleArreteRendLAgentIndisponible() {
        when(modele.chat(any(ChatRequest.class)))
                .thenThrow(new LangChain4jException("échec", new ConnectException("Connection refused")));

        assertEchec(Raison.AGENT_INDISPONIBLE);
    }

    @Test
    void uneCleRefuseeParUnServiceEnLigneRendLAgentIndisponible() {
        when(modele.chat(any(ChatRequest.class))).thenThrow(new AuthenticationException("clé invalide"));

        assertEchec(Raison.AGENT_INDISPONIBLE);
    }

    @Test
    void unQuotaAtteintRendLAgentIndisponible() {
        when(modele.chat(any(ChatRequest.class))).thenThrow(new RateLimitException("trop de requêtes"));

        assertEchec(Raison.AGENT_INDISPONIBLE);
    }

    @Test
    void unDelaiDepasseEstSignale() {
        when(modele.chat(any(ChatRequest.class))).thenThrow(new TimeoutException("trop long"));

        assertEchec(Raison.DELAI_DEPASSE);
    }

    @Test
    void uneErreurInconnueEstSignalee() {
        when(modele.chat(any(ChatRequest.class))).thenThrow(new IllegalStateException("bug"));

        assertEchec(Raison.INCONNUE);
    }

    private static ChatResponse texte(String texte) {
        return ChatResponse.builder().aiMessage(AiMessage.from(texte)).build();
    }

    private static ChatResponse demandeOutil(String nom) {
        ToolExecutionRequest demande = ToolExecutionRequest.builder()
                .id("appel-1")
                .name(nom)
                .arguments("{}")
                .build();
        return ChatResponse.builder().aiMessage(AiMessage.from(demande)).build();
    }

    private List<ChatRequest> requetesEnvoyees(int nombre) {
        ArgumentCaptor<ChatRequest> requetes = ArgumentCaptor.forClass(ChatRequest.class);
        verify(modele, times(nombre)).chat(requetes.capture());
        return requetes.getAllValues();
    }

    private void assertEchec(Raison raisonAttendue) {
        assertThatThrownBy(() -> descripteur.decrire(IMAGE, null))
                .isInstanceOf(DescriptionImpossibleException.class)
                .extracting(e -> ((DescriptionImpossibleException) e).getRaison())
                .isEqualTo(raisonAttendue);
    }
}
