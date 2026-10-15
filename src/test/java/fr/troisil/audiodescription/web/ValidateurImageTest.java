package fr.troisil.audiodescription.web;

import fr.troisil.audiodescription.agent.Image;
import fr.troisil.audiodescription.web.ImageRefuseeException.Motif;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static fr.troisil.audiodescription.web.ImagesDeTest.JPEG;
import static fr.troisil.audiodescription.web.ImagesDeTest.PNG;
import static fr.troisil.audiodescription.web.ImagesDeTest.TEXTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidateurImageTest {

    private final ValidateurImage validateur = new ValidateurImage();

    @Test
    void accepteUnJpeg() {
        Image image = validateur.valider(fichier("image/jpeg", JPEG));

        assertThat(image.typeMime()).isEqualTo("image/jpeg");
        assertThat(image.octets()).isEqualTo(JPEG);
    }

    @Test
    void accepteUnPng() {
        Image image = validateur.valider(fichier("image/png", PNG));

        assertThat(image.typeMime()).isEqualTo("image/png");
    }

    @Test
    void retientLeTypeReelPlutotQueLeTypeAnnonce() {
        Image image = validateur.valider(fichier("image/png", JPEG));

        assertThat(image.typeMime()).isEqualTo("image/jpeg");
    }

    @Test
    void refuseUnFichierAbsent() {
        assertRefuse(null, Motif.ABSENTE);
    }

    @Test
    void refuseUnFichierVide() {
        assertRefuse(fichier("image/jpeg", new byte[0]), Motif.VIDE);
    }

    @Test
    void refuseUnTypeAnnonceNonPrisEnCharge() {
        assertRefuse(fichier("application/pdf", JPEG), Motif.FORMAT_NON_PRIS_EN_CHARGE);
    }

    @Test
    void refuseUnFichierSansTypeAnnonce() {
        assertRefuse(fichier(null, JPEG), Motif.FORMAT_NON_PRIS_EN_CHARGE);
    }

    @Test
    void refuseUnContenuQuiNestPasUneImage() {
        assertRefuse(fichier("image/jpeg", TEXTE), Motif.FORMAT_NON_PRIS_EN_CHARGE);
    }

    private void assertRefuse(MockMultipartFile fichier, Motif motifAttendu) {
        assertThatThrownBy(() -> validateur.valider(fichier))
                .isInstanceOf(ImageRefuseeException.class)
                .extracting(e -> ((ImageRefuseeException) e).getMotif())
                .isEqualTo(motifAttendu);
    }

    private static MockMultipartFile fichier(String type, byte[] contenu) {
        return new MockMultipartFile("image", "photo.jpg", type, contenu);
    }
}
