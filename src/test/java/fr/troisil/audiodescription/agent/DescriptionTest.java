package fr.troisil.audiodescription.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescriptionTest {

    @Test
    void retireLesEspacesAutourDuTexte() {
        assertThat(new Description("  Un chat sur un canapé.\n").texte()).isEqualTo("Un chat sur un canapé.");
    }

    @Test
    void refuseUnTexteNul() {
        assertThatThrownBy(() -> new Description(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n\t"})
    void refuseUnTexteVide(String texte) {
        assertThatThrownBy(() -> new Description(texte)).isInstanceOf(IllegalArgumentException.class);
    }
}
