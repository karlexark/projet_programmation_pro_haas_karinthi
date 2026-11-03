package fr.troisil.audiodescription.agent.langchain4j;

import dev.langchain4j.agent.tool.Tool;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Les outils que l'agent peut appeler pendant qu'il prépare sa réponse.
 * <p>
 * Un modèle ne connaît pas la date du jour : il la devinerait. L'outil la lui donne, ce qui
 * permet de répondre à « Ce yaourt est-il périmé ? » en comparant la date lue sur l'emballage
 * à la date réelle.
 * <p>
 * L'horloge est reçue par le constructeur : les tests utilisent une horloge fixe.
 */
public final class OutilsAgent {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' H 'h' mm", Locale.FRENCH);

    private final Clock horloge;

    /**
     * @param horloge l'horloge qui donne la date, l'heure et le fuseau horaire, jamais null
     */
    public OutilsAgent(Clock horloge) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
    }

    /**
     * @return la date et l'heure actuelles, en toutes lettres
     */
    @Tool("Donne la date et l'heure actuelles. À utiliser chaque fois que la réponse dépend du jour présent : "
            + "savoir si une date de péremption est dépassée, quel jour on est, combien de temps il reste.")
    public String dateEtHeureActuelles() {
        return "Nous sommes le " + ZonedDateTime.now(horloge).format(FORMAT) + ".";
    }
}
