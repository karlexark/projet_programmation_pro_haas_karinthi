package fr.troisil.audiodescription.web;

import fr.troisil.audiodescription.agent.DescripteurImage;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException.Raison;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static fr.troisil.audiodescription.web.ImagesDeTest.JPEG;
import static fr.troisil.audiodescription.web.ImagesDeTest.TEXTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DescriptionControleur.class)
@Import({ValidateurImage.class, ValidateurQuestion.class})
class GestionnaireErreursTest {

    private static final MockMultipartFile IMAGE_JPEG =
            new MockMultipartFile("image", "photo.jpg", "image/jpeg", JPEG);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DescripteurImage descripteur;

    @Test
    void sansImageExpliqueQuIlFautEnChoisirUne() throws Exception {
        mockMvc.perform(multipart("/api/descriptions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ABSENTE"))
                .andExpect(jsonPath("$.message").value(containsString("Choisissez une image")));

        verifyNoInteractions(descripteur);
    }

    @Test
    void unFichierQuiNestPasUneImageEstRefuseAvantLAgent() throws Exception {
        MockMultipartFile texte = new MockMultipartFile("image", "photo.jpg", "image/jpeg", TEXTE);

        mockMvc.perform(multipart("/api/descriptions").file(texte))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FORMAT_NON_PRIS_EN_CHARGE"))
                .andExpect(jsonPath("$.message").value(containsString("JPEG ou PNG")));

        verifyNoInteractions(descripteur);
    }

    @ParameterizedTest
    @CsvSource({
            "AGENT_INDISPONIBLE, 503, AGENT_INDISPONIBLE",
            "DELAI_DEPASSE,      504, DELAI_DEPASSE",
            "REPONSE_VIDE,       502, REPONSE_VIDE",
            "INCONNUE,           500, INATTENDUE"
    })
    void chaquePanneDeLAgentDonneUnMessageAdapte(Raison raison, int statut, String code) throws Exception {
        when(descripteur.decrire(any(), any()))
                .thenThrow(new DescriptionImpossibleException(raison, "détail technique interne"));

        mockMvc.perform(multipart("/api/descriptions").file(IMAGE_JPEG))
                .andExpect(status().is(statut))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(content().string(not(containsString("détail technique interne"))));
    }

    @Test
    void uneErreurInattendueNeRevelePasLeDetailTechnique() throws Exception {
        when(descripteur.decrire(any(), any())).thenThrow(new IllegalStateException("NullPointer quelque part"));

        mockMvc.perform(multipart("/api/descriptions").file(IMAGE_JPEG))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INATTENDUE"))
                .andExpect(content().string(not(containsString("NullPointer"))));
    }

    @Test
    void uneRessourceIntrouvableDonne404EtNonUneErreurServeur() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }

    @Test
    void uneMauvaiseMethodeDonne405() throws Exception {
        mockMvc.perform(get("/api/descriptions"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }

    @Test
    void unFormatDeRequeteNonPrisEnChargeDonne415() throws Exception {
        mockMvc.perform(post("/api/descriptions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));

        verifyNoInteractions(descripteur);
    }

    @Test
    void uneQuestionTropLongueEstRefuseeAvantLAgent() throws Exception {
        String question = "a".repeat(ValidateurQuestion.LONGUEUR_MAXIMALE + 1);

        mockMvc.perform(multipart("/api/descriptions").file(IMAGE_JPEG).param("question", question))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUESTION_TROP_LONGUE"))
                .andExpect(jsonPath("$.message").value(containsString("500 caractères au maximum")));

        verifyNoInteractions(descripteur);
    }

    @Test
    void uneImageTropLourdeIndiqueLaTailleMaximale() {
        GestionnaireErreurs gestionnaire = new GestionnaireErreurs(DataSize.ofMegabytes(10));

        ResponseEntity<ErreurReponse> reponse =
                gestionnaire.imageTropLourde(new MaxUploadSizeExceededException(10_000_000));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(reponse.getBody().code()).isEqualTo("TROP_LOURDE");
        assertThat(reponse.getBody().message()).contains("moins de 10 mégaoctets");
    }
}
