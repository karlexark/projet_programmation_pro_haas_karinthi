package fr.troisil.audiodescription.agent.faux;

import fr.troisil.audiodescription.agent.Description;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException.Raison;
import fr.troisil.audiodescription.agent.Image;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FauxDescripteurTest {

    private static final Image IMAGE = new Image(new byte[]{1, 2, 3}, "image/png");

    private final FauxDescripteur descripteur = new FauxDescripteur(Duration.ZERO, null);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void sansQuestionRenvoieLaDescriptionFixe(String question) throws DescriptionImpossibleException {
        Description description = descripteur.decrire(IMAGE, question);

        assertThat(description.texte()).isEqualTo(FauxDescripteur.DESCRIPTION_FIXE);
    }

    @Test
    void avecQuestionLaReprendDansLaReponse() throws DescriptionImpossibleException {
        Description description = descripteur.decrire(IMAGE, "  Quelle est la date ?  ");

        assertThat(description.texte()).startsWith("Vous avez demandé : « Quelle est la date ? ».");
    }

    @Test
    void refuseUneImageNulle() {
        assertThatThrownBy(() -> descripteur.decrire(null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void simuleLEchecConfigure() {
        FauxDescripteur enPanne = new FauxDescripteur(Duration.ZERO, Raison.DELAI_DEPASSE);

        assertThatThrownBy(() -> enPanne.decrire(IMAGE, null))
                .isInstanceOf(DescriptionImpossibleException.class)
                .extracting(e -> ((DescriptionImpossibleException) e).getRaison())
                .isEqualTo(Raison.DELAI_DEPASSE);
    }

    @Test
    void refuseUnDelaiNegatif() {
        assertThatThrownBy(() -> new FauxDescripteur(Duration.ofSeconds(-1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
