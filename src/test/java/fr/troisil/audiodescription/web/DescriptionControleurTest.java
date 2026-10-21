package fr.troisil.audiodescription.web;

import static fr.troisil.audiodescription.web.ImagesDeTest.JPEG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.troisil.audiodescription.agent.DescripteurImage;
import fr.troisil.audiodescription.agent.Description;
import fr.troisil.audiodescription.agent.Image;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DescriptionControleur.class)
@Import({ValidateurImage.class, ValidateurQuestion.class})
class DescriptionControleurTest {

    private static final MockMultipartFile IMAGE_JPEG = new MockMultipartFile("image", "photo.jpg", "image/jpeg", JPEG);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DescripteurImage descripteur;

    @Test
    void renvoieLaDescriptionDeLAgent() throws Exception {
        when(descripteur.decrire(any(), any())).thenReturn(new Description("Un chat sur un canapé."));

        mockMvc.perform(multipart("/api/descriptions").file(IMAGE_JPEG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texte").value("Un chat sur un canapé."));
    }

    @Test
    void transmetLImageValideeALAgent() throws Exception {
        when(descripteur.decrire(any(), any())).thenReturn(new Description("ok"));

        mockMvc.perform(multipart("/api/descriptions").file(IMAGE_JPEG));

        ArgumentCaptor<Image> image = ArgumentCaptor.forClass(Image.class);
        verify(descripteur).decrire(image.capture(), isNull());
        assertThat(image.getValue()).isEqualTo(new Image(JPEG, "image/jpeg"));
    }

    @Test
    void transmetLaQuestionSansLesEspacesAutour() throws Exception {
        when(descripteur.decrire(any(), any())).thenReturn(new Description("ok"));

        mockMvc.perform(multipart("/api/descriptions").file(IMAGE_JPEG).param("question", "  Quelle rue ?  "));

        verify(descripteur).decrire(any(), eq("Quelle rue ?"));
    }

    @Test
    void uneQuestionBlancheEquivautAUneQuestionAbsente() throws Exception {
        when(descripteur.decrire(any(), any())).thenReturn(new Description("ok"));

        mockMvc.perform(multipart("/api/descriptions").file(IMAGE_JPEG).param("question", "   "));

        verify(descripteur).decrire(any(), isNull());
    }
}
