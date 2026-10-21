package fr.troisil.audiodescription;

import static org.assertj.core.api.Assertions.assertThat;

import fr.troisil.audiodescription.agent.DescripteurImage;
import fr.troisil.audiodescription.agent.faux.FauxDescripteur;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("faux")
class AudiodescriptionApplicationTests {

    @Autowired
    private DescripteurImage descripteur;

    @Test
    void leProfilFauxInjecteLaFausseImplementation() {
        assertThat(descripteur).isInstanceOf(FauxDescripteur.class);
    }
}
