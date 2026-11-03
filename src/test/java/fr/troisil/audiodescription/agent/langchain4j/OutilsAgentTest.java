package fr.troisil.audiodescription.agent.langchain4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutilsAgentTest {

    @Test
    void donneLaDateEtLHeureDuFuseauHoraire() {
        Clock mardiApresMidi = Clock.fixed(Instant.parse("2026-11-03T13:05:00Z"), ZoneId.of("Europe/Paris"));

        assertThat(new OutilsAgent(mardiApresMidi).dateEtHeureActuelles())
                .isEqualTo("Nous sommes le mardi 3 novembre 2026 à 14 h 05.");
    }

    @Test
    void tientCompteDuChangementDeJourSelonLeFuseau() {
        Clock minuitEtDemiAParis = Clock.fixed(Instant.parse("2026-11-03T23:30:00Z"), ZoneId.of("Europe/Paris"));

        assertThat(new OutilsAgent(minuitEtDemiAParis).dateEtHeureActuelles())
                .isEqualTo("Nous sommes le mercredi 4 novembre 2026 à 0 h 30.");
    }

    @Test
    void estPresenteAuModeleAvecUnNomEtUneDescription() {
        List<ToolSpecification> specifications =
                ToolSpecifications.toolSpecificationsFrom(new OutilsAgent(Clock.systemUTC()));

        assertThat(specifications).hasSize(1);
        assertThat(specifications.get(0).name()).isEqualTo("dateEtHeureActuelles");
        assertThat(specifications.get(0).description()).contains("péremption");
    }

    @Test
    void refuseUneHorlogeNulle() {
        assertThatThrownBy(() -> new OutilsAgent(null)).isInstanceOf(NullPointerException.class);
    }
}
