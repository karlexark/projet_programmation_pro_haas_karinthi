package fr.troisil.audiodescription.agent.langchain4j.ollama;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import fr.troisil.audiodescription.agent.DescripteurImage;
import fr.troisil.audiodescription.agent.langchain4j.DescripteurLangChain4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Fournisseur Ollama : actif quand {@code descripteur.fournisseur=ollama} (valeur par défaut).
 * <p>
 * Construit le modèle à partir des propriétés {@code descripteur.ollama.*}. Changer de serveur,
 * de modèle ou de délai se fait dans {@code application.properties} (ou par les variables
 * d'environnement {@code OLLAMA_URL} et {@code OLLAMA_MODELE}), sans modifier le code.
 */
@Configuration
@ConditionalOnProperty(name = "descripteur.fournisseur", havingValue = "ollama", matchIfMissing = true)
public class OllamaConfiguration {

    @Bean
    public ChatModel modeleOllama(
            @Value("${descripteur.ollama.url}") String url,
            @Value("${descripteur.ollama.modele}") String modele,
            @Value("${descripteur.ollama.timeout}") Duration delai,
            @Value("${descripteur.ollama.temperature}") double temperature) {
        return OllamaChatModel.builder()
                .baseUrl(url)
                .modelName(modele)
                .timeout(delai)
                .temperature(temperature)
                // Pas de nouvel essai automatique : avec un modèle lent, l'utilisateur
                // attendrait plusieurs fois le délai avant d'être prévenu.
                .maxRetries(0)
                .build();
    }

    @Bean
    public DescripteurImage descripteurOllama(ChatModel modeleOllama) {
        return new DescripteurLangChain4j(modeleOllama);
    }
}
