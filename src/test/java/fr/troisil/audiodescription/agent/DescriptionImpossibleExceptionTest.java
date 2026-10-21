package fr.troisil.audiodescription.agent;

import fr.troisil.audiodescription.agent.DescriptionImpossibleException.Raison;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescriptionImpossibleExceptionTest {

    @Test
    void estUneExceptionVerifiee() {
        // Vérifiée = sous-classe d'Exception, mais pas de RuntimeException.
        assertThat(Exception.class.isAssignableFrom(DescriptionImpossibleException.class)).isTrue();
        assertThat(RuntimeException.class.isAssignableFrom(DescriptionImpossibleException.class)).isFalse();
    }

    @Test
    void conserveLaRaisonLeMessageEtLaCause() {
        ConnectException cause = new ConnectException("Connection refused");

        DescriptionImpossibleException e =
                new DescriptionImpossibleException(Raison.AGENT_INDISPONIBLE, "Ollama ne répond pas", cause);

        assertThat(e.getRaison()).isEqualTo(Raison.AGENT_INDISPONIBLE);
        assertThat(e).hasMessage("Ollama ne répond pas").hasCause(cause);
    }

    @Test
    void refuseUneRaisonNulle() {
        // C'est la construction elle-même qui doit échouer, avant que l'exception soit lancée.
        assertThatThrownBy(() -> {
            throw new DescriptionImpossibleException(null, "message");
        }).isInstanceOf(NullPointerException.class);
    }
}
