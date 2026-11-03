package fr.troisil.audiodescription.agent.langchain4j.ollama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fr.troisil.audiodescription.agent.Description;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException;
import fr.troisil.audiodescription.agent.DescriptionImpossibleException.Raison;
import fr.troisil.audiodescription.agent.Image;
import fr.troisil.audiodescription.agent.langchain4j.DescripteurLangChain4j;
import fr.troisil.audiodescription.agent.langchain4j.OutilsAgent;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-03T13:05:00Z"), ZoneId.of("Europe/Paris"));

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

    @Test
    void comprendUnAppelDOutilAuFormatDOllamaEtRenvoieLeResultat() throws IOException, DescriptionImpossibleException {
        List<String> requetes = new CopyOnWriteArrayList<>();
        String url = demarrerServeur(List.of("""
                        {"model":"test","created_at":"2026-11-03T13:05:00Z",
                         "message":{"role":"assistant","content":"",
                           "tool_calls":[{"function":{"name":"dateEtHeureActuelles","arguments":{}}}]},
                         "done":true}""", """
                        {"model":"test","created_at":"2026-11-03T13:05:01Z",
                         "message":{"role":"assistant","content":"Ce yaourt est périmé."},
                         "done":true}"""), requetes);

        Description description = descripteur(url, Duration.ofSeconds(5)).decrire(IMAGE, "Est-il périmé ?");

        assertThat(description.texte()).isEqualTo("Ce yaourt est périmé.");
        assertThat(requetes).hasSize(2);
        assertThat(requetes.get(0)).contains("dateEtHeureActuelles");
        assertThat(requetes.get(1)).contains("Nous sommes le mardi 3 novembre 2026 à 14 h 05.");
    }

    private String demarrerServeur(int statut, String corps, AtomicReference<String> requete) throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        serveur.createContext("/", echange -> {
            requete.set(new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            repondre(echange, statut, corps);
        });
        serveur.start();
        return adresse();
    }

    /** Un serveur qui renvoie les réponses dans l'ordre, une par requête reçue. */
    private String demarrerServeur(List<String> reponses, List<String> requetes) throws IOException {
        AtomicInteger numero = new AtomicInteger();
        serveur = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        serveur.createContext("/", echange -> {
            requetes.add(new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            repondre(echange, 200, reponses.get(Math.min(numero.getAndIncrement(), reponses.size() - 1)));
        });
        serveur.start();
        return adresse();
    }

    private static void repondre(HttpExchange echange, int statut, String corps) throws IOException {
        byte[] reponse = corps.getBytes(StandardCharsets.UTF_8);
        echange.getResponseHeaders().add("Content-Type", "application/json");
        echange.sendResponseHeaders(statut, reponse.length);
        try (OutputStream sortie = echange.getResponseBody()) {
            sortie.write(reponse);
        }
    }

    private String adresse() {
        return "http://localhost:" + serveur.getAddress().getPort();
    }

    private static DescripteurLangChain4j descripteur(String url, Duration delai) {
        return new DescripteurLangChain4j(
                new OllamaConfiguration().modeleOllama(url, "test", delai, 0.1, 8192), new OutilsAgent(HORLOGE));
    }

    private static void assertEchec(DescripteurLangChain4j descripteur, Raison raisonAttendue) {
        assertThatThrownBy(() -> descripteur.decrire(IMAGE, null))
                .isInstanceOf(DescriptionImpossibleException.class)
                .extracting(e -> ((DescriptionImpossibleException) e).getRaison())
                .isEqualTo(raisonAttendue);
    }
}
