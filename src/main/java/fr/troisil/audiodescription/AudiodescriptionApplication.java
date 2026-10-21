package fr.troisil.audiodescription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application d'audiodescription.
 * <p>
 * Spring détecte les composants des sous-packages {@code agent} (les implémentations de
 * l'agent) et {@code web} (le contrôleur, la validation et la traduction des erreurs).
 */
@SpringBootApplication
public class AudiodescriptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AudiodescriptionApplication.class, args);
    }
}
