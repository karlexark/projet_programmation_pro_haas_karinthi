package fr.troisil.audiodescription.agent.ollama;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/**
 * Construit le modèle Ollama à partir des propriétés {@code descripteur.ollama.*}.
 * <p>
 * Changer de serveur, de modèle ou de délai se fait dans {@code application.properties}
 * (ou par les variables d'environnement {@code OLLAMA_URL} et {@code OLLAMA_MODELE}),
 * sans modifier le code.
 */
@Configuration
@Profile("!faux")
public class OllamaConfiguration {

    @Bean
    public ChatModel modeleOllama(
            @Value("${descripteur.ollama.url}") String url,
            @Value("${descripteur.ollama.modele}") String modele,
            @Value("${descripteur.ollama.timeout}") Duration delai) {
        return OllamaChatModel.builder()
                .baseUrl(url)
                .modelName(modele)
                .timeout(delai)
                // Pas de nouvel essai automatique : avec un modèle lent, l'utilisateur
                // attendrait plusieurs fois le délai avant d'être prévenu.
                .maxRetries(0)
                .build();
    }
}
