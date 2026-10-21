package fr.troisil.audiodescription.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ImageTest {

    private static final byte[] OCTETS = {1, 2, 3};

    @Test
    void conserveLeContenuEtLeType() {
        Image image = new Image(OCTETS, "image/png");

        assertThat(image.octets()).isEqualTo(OCTETS);
        assertThat(image.typeMime()).isEqualTo("image/png");
    }

    @Test
    void refuseDesOctetsNuls() {
        assertThatThrownBy(() -> new Image(null, "image/png")).isInstanceOf(NullPointerException.class);
    }

    @Test
    void refuseUnTypeNul() {
        assertThatThrownBy(() -> new Image(OCTETS, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void refuseUneImageVide() {
        assertThatThrownBy(() -> new Image(new byte[0], "image/png")).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void refuseUnTypeBlanc(String type) {
        assertThatThrownBy(() -> new Image(OCTETS, type)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void modifierLeTableauDOrigineNeModifiePasLImage() {
        byte[] origine = {1, 2, 3};
        Image image = new Image(origine, "image/png");

        origine[0] = 99;

        assertThat(image.octets()).containsExactly(1, 2, 3);
    }

    @Test
    void modifierLeTableauLuNeModifiePasLImage() {
        Image image = new Image(OCTETS, "image/png");

        image.octets()[0] = 99;

        assertThat(image.octets()).containsExactly(1, 2, 3);
    }

    @Test
    void deuxImagesDeMemeContenuSontEgales() {
        Image premiere = new Image(new byte[] {1, 2, 3}, "image/png");
        Image seconde = new Image(new byte[] {1, 2, 3}, "image/png");

        assertThat(premiere).isEqualTo(seconde).hasSameHashCodeAs(seconde);
    }

    @Test
    void deuxImagesDeTypesDifferentsNeSontPasEgales() {
        assertThat(new Image(OCTETS, "image/png")).isNotEqualTo(new Image(OCTETS, "image/jpeg"));
    }

    @Test
    void leTexteNAffichePasLesOctets() {
        assertThat(new Image(OCTETS, "image/png")).hasToString("Image[image/png, 3 octets]");
    }
}
