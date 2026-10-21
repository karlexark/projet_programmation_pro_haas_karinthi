package fr.troisil.audiodescription.agent.langchain4j.ollama;

import com.sun.net.httpserver.HttpServer;
import fr.troisil.audiodescription.agent.Description;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException.Raison;
import fr.troisil.audiodescription.agent.Image;
import fr.troisil.audiodescription.agent.langchain4j.DescripteurLangChain4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vérifie l'adaptateur avec le vrai client LangChain4J, face à un faux serveur Ollama local.
 * <p>
 * Ces tests ne demandent pas Ollama. Ils contrôlent que les erreurs réelles de la
 * bibliothèque (connexion refusée, délai, modèle absent) sont bien classées : son
 * comportement change d'une version à l'autre.
 */
class OllamaReseauTest {

    private static final byte[] OCTETS = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
    private static final Image IMAGE = new Image(OCTETS, "image/png");

    private HttpServer serveur;

    @AfterEach
    void arreterServeur() {
        if (serveur != null) {
            serveur.stop(0);
        }
    }

    @Test
    void envoieLImageEtLitLaReponseDOllama() throws IOException, DescriptionImpossibleException {
        AtomicReference<String> requete = new AtomicReference<>();
        String url = demarrerServeur(200, """
                {"model":"test","created_at":"2026-10-15T10:00:00Z",
                 "message":{"role":"assistant","content":"Un carré noir sur fond blanc."},
                 "done":true}""", requete);

        Description description = descripteur(url, Duration.ofSeconds(5)).decrire(IMAGE, "Quelle forme ?");

        assertThat(description.texte()).isEqualTo("Un carré noir sur fond blanc.");
        assertThat(requete.get())
                .contains(Base64.getEncoder().encodeToString(OCTETS))
                .contains("Quelle forme ?");
    }

    @Test
    void unServeurArreteRendLAgentIndisponible() throws IOException {
        int portFerme;
        try (ServerSocket socket = new ServerSocket(0)) {
            portFerme = socket.getLocalPort();
        }

        assertEchec(descripteur("http://localhost:" + portFerme, Duration.ofSeconds(5)), Raison.AGENT_INDISPONIBLE);
    }

    @Test
    void unModeleAbsentDuServeurRendLAgentIndisponible() throws IOException {
        String url = demarrerServeur(404, "{\"error\":\"model 'test' not found\"}", new AtomicReference<>());

        assertEchec(descripteur(url, Duration.ofSeconds(5)), Raison.AGENT_INDISPONIBLE);
    }

    @Test
    void unServeurTropLentDepasseLeDelai() throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        serveur.createContext("/", echange -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        serveur.start();

        assertEchec(descripteur(adresse(), Duration.ofMillis(500)), Raison.DELAI_DEPASSE);
    }

    private String demarrerServeur(int statut, String corps, AtomicReference<String> requete) throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        serveur.createContext("/", echange -> {
            requete.set(new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] reponse = corps.getBytes(StandardCharsets.UTF_8);
            echange.getResponseHeaders().add("Content-Type", "application/json");
            echange.sendResponseHeaders(statut, reponse.length);
            try (OutputStream sortie = echange.getResponseBody()) {
                sortie.write(reponse);
            }
        });
        serveur.start();
        return adresse();
    }

    private String adresse() {
        return "http://localhost:" + serveur.getAddress().getPort();
    }

    private static DescripteurLangChain4j descripteur(String url, Duration delai) {
        return new DescripteurLangChain4j(new OllamaConfiguration().modeleOllama(url, "test", delai, 0.1));
    }

    private static void assertEchec(DescripteurLangChain4j descripteur, Raison raisonAttendue) {
        assertThatThrownBy(() -> descripteur.decrire(IMAGE, null))
                .isInstanceOf(DescriptionImpossibleException.class)
                .extracting(e -> ((DescriptionImpossibleException) e).getRaison())
                .isEqualTo(raisonAttendue);
    }
}
