package fr.troisil.audiodescription.agent.ollama;

import fr.troisil.audiodescription.agent.DescripteurImage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Sans profil, l'application utilise le vrai agent (Ollama n'a pas besoin de tourner pour ce test). */
@SpringBootTest
class OllamaConfigurationTest {

    @Autowired
    private DescripteurImage descripteur;

    @Test
    void sansProfilFauxLAgentEstOllama() {
        assertThat(descripteur).isInstanceOf(OllamaDescripteur.class);
    }
}
