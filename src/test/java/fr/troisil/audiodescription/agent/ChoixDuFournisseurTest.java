package fr.troisil.audiodescription.agent;

import fr.troisil.audiodescription.agent.faux.FauxDescripteur;
import fr.troisil.audiodescription.agent.langchain4j.DescripteurLangChain4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le fournisseur de l'agent se choisit par la propriété {@code descripteur.fournisseur},
 * sans modifier une ligne de Java. Ollama n'a pas besoin de tourner pour ces tests.
 */
class ChoixDuFournisseurTest {

    @Nested
    @SpringBootTest
    class ParDefaut {

        @Autowired
        private DescripteurImage descripteur;

        @Test
        void lAgentInterrogeLeModeleViaLangChain4j() {
            assertThat(descripteur).isInstanceOf(DescripteurLangChain4j.class);
        }
    }

    @Nested
    @SpringBootTest(properties = "descripteur.fournisseur=faux")
    class Faux {

        @Autowired
        private DescripteurImage descripteur;

        @Test
        void lAgentEstLaFausseImplementation() {
            assertThat(descripteur).isInstanceOf(FauxDescripteur.class);
        }
    }
}
