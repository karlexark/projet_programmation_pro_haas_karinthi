package fr.troisil.audiodescription.agent.faux;

import fr.troisil.audiodescription.agent.DescripteurImage;
import fr.troisil.audiodescription.agent.Description;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException;
import fr.troisil.audiodescription.agent.Image;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Fausse implémentation de {@link DescripteurImage}, qui n'appelle aucun modèle.
 * <p>
 * Elle renvoie une réponse fixe et prévisible. Elle sert à développer la page,
 * à tester le contrôleur et à faire la démonstration sur un poste sans modèle.
 * Elle est active quand {@code descripteur.fournisseur=faux}, ce que fait le profil Spring {@code faux}.
 * <p>
 * Deux propriétés permettent de simuler le comportement d'un vrai modèle :
 * <ul>
 *     <li>{@code descripteur.faux.delai} : temps d'attente avant de répondre,
 *     pour tester le message d'attente de la page ;</li>
 *     <li>{@code descripteur.faux.echec} : une {@link DescriptionImpossibleException.Raison}
 *     à renvoyer systématiquement, pour tester l'affichage des erreurs.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "descripteur.fournisseur", havingValue = "faux")
public final class FauxDescripteur implements DescripteurImage {

    static final String DESCRIPTION_FIXE =
            "Ceci est une description de démonstration. "
                    + "L'image n'a pas été analysée : "
                    + "l'application fonctionne sans modèle d'intelligence artificielle.";

    private final Duration delai;
    private final DescriptionImpossibleException.Raison echec;

    /**
     * @param delai temps d'attente simulé avant chaque réponse, jamais null ni négatif
     * @param echec raison d'échec à simuler, ou null pour répondre normalement
     */
    public FauxDescripteur(
            @Value("${descripteur.faux.delai:PT0S}") Duration delai,
            @Value("${descripteur.faux.echec:#{null}}") DescriptionImpossibleException.Raison echec) {
        Objects.requireNonNull(delai, "delai");
        if (delai.isNegative()) {
            throw new IllegalArgumentException("Le délai ne doit pas être négatif");
        }
        this.delai = delai;
        this.echec = echec;
    }

    @Override
    public Description decrire(Image image, String question) throws DescriptionImpossibleException {
        Objects.requireNonNull(image, "image");
        patienter();
        if (echec != null) {
            throw new DescriptionImpossibleException(echec, "Échec simulé par la fausse implémentation : " + echec);
        }
        if (question == null || question.isBlank()) {
            return new Description(DESCRIPTION_FIXE);
        }
        return new Description("Vous avez demandé : « " + question.strip() + " ». " + DESCRIPTION_FIXE);
    }

    private void patienter() throws DescriptionImpossibleException {
        if (delai.isZero()) {
            return;
        }
        try {
            Thread.sleep(delai);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DescriptionImpossibleException(
                    DescriptionImpossibleException.Raison.INCONNUE, "Attente interrompue", e);
        }
    }
}
