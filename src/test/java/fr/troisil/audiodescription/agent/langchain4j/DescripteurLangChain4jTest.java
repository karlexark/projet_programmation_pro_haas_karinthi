package fr.troisil.audiodescription.agent.langchain4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.LangChain4jException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import fr.troisil.audiodescription.agent.Description;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException.Raison;
import fr.troisil.audiodescription.agent.Image;
import java.net.ConnectException;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class DescripteurLangChain4jTest {

    private static final byte[] OCTETS = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2};
    private static final Image IMAGE = new Image(OCTETS, "image/jpeg");

    private final ChatModel modele = mock(ChatModel.class);
    private final DescripteurLangChain4j descripteur = new DescripteurLangChain4j(modele);

    @Test
    void renvoieLeTexteDuModele() throws DescriptionImpossibleException {
        repondre("  Un chat dort sur un canapé rouge.  ");

        Description description = descripteur.decrire(IMAGE, null);

        assertThat(description.texte()).isEqualTo("Un chat dort sur un canapé rouge.");
    }

    @Test
    void envoieLaConsigneEtLImageEnBase64() throws DescriptionImpossibleException {
        repondre("ok");

        descripteur.decrire(IMAGE, null);

        List<ChatMessage> messages = messagesEnvoyes();
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
        repondre("ok");

        descripteur.decrire(IMAGE, "Quelle est la date de péremption ?");

        UserMessage demande = (UserMessage) messagesEnvoyes().get(1);
        assertThat(((TextContent) demande.contents().get(0)).text()).contains("« Quelle est la date de péremption ? »");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n"})
    void uneReponseVideEstUnEchec(String texte) {
        repondre(texte);

        assertEchec(Raison.REPONSE_VIDE);
    }

    @Test
    void unModeleArreteRendLAgentIndisponible() {
        when(modele.chat(anyList()))
                .thenThrow(new LangChain4jException("échec", new ConnectException("Connection refused")));

        assertEchec(Raison.AGENT_INDISPONIBLE);
    }

    @Test
    void uneCleRefuseeParUnServiceEnLigneRendLAgentIndisponible() {
        when(modele.chat(anyList())).thenThrow(new AuthenticationException("clé invalide"));

        assertEchec(Raison.AGENT_INDISPONIBLE);
    }

    @Test
    void unQuotaAtteintRendLAgentIndisponible() {
        when(modele.chat(anyList())).thenThrow(new RateLimitException("trop de requêtes"));

        assertEchec(Raison.AGENT_INDISPONIBLE);
    }

    @Test
    void unDelaiDepasseEstSignale() {
        when(modele.chat(anyList())).thenThrow(new TimeoutException("trop long"));

        assertEchec(Raison.DELAI_DEPASSE);
    }

    @Test
    void uneErreurInconnueEstSignalee() {
        when(modele.chat(anyList())).thenThrow(new IllegalStateException("bug"));

        assertEchec(Raison.INCONNUE);
    }

    private void repondre(String texte) {
        when(modele.chat(anyList()))
                .thenReturn(
                        ChatResponse.builder().aiMessage(AiMessage.from(texte)).build());
    }

    @SuppressWarnings("unchecked")
    private List<ChatMessage> messagesEnvoyes() {
        ArgumentCaptor<List<ChatMessage>> messages = ArgumentCaptor.forClass(List.class);
        verify(modele).chat(messages.capture());
        return messages.getValue();
    }

    private void assertEchec(Raison raisonAttendue) {
        assertThatThrownBy(() -> descripteur.decrire(IMAGE, null))
                .isInstanceOf(DescriptionImpossibleException.class)
                .extracting(e -> ((DescriptionImpossibleException) e).getRaison())
                .isEqualTo(raisonAttendue);
    }
}
