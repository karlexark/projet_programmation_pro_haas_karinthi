package fr.troisil.audiodescription.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static fr.troisil.audiodescription.web.ValidateurQuestion.LONGUEUR_MAXIMALE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidateurQuestionTest {

    private final ValidateurQuestion validateur = new ValidateurQuestion();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    void uneQuestionVideEquivautAUneQuestionAbsente(String question) {
        assertThat(validateur.valider(question)).isNull();
    }

    @Test
    void retireLesEspacesAutourDeLaQuestion() {
        assertThat(validateur.valider("  Quelle rue ?  ")).isEqualTo("Quelle rue ?");
    }

    @Test
    void accepteUneQuestionDeLongueurMaximale() {
        String question = "a".repeat(LONGUEUR_MAXIMALE);

        assertThat(validateur.valider(question)).isEqualTo(question);
    }

    @Test
    void lesEspacesAutourNeComptentPasDansLaLongueur() {
        String question = "  " + "a".repeat(LONGUEUR_MAXIMALE) + "  ";

        assertThat(validateur.valider(question)).hasSize(LONGUEUR_MAXIMALE);
    }

    @Test
    void refuseUneQuestionTropLongue() {
        String question = "a".repeat(LONGUEUR_MAXIMALE + 1);

        assertThatThrownBy(() -> validateur.valider(question))
                .isInstanceOf(QuestionTropLongueException.class);
    }
}
