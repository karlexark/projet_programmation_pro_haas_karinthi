package fr.troisil.audiodescription.agent.ollama;

/**
 * Les textes envoyés au modèle : son rôle (message système) et la demande (message utilisateur).
 * <p>
 * Ils sont regroupés ici pour pouvoir faire évoluer la consigne sans toucher au code
 * qui appelle le modèle.
 */
final class Consigne {

    /** Le rôle du modèle, envoyé avant chaque demande. */
    static final String SYSTEME = """
            Tu es un audiodescripteur. Tu décris des images à des personnes aveugles \
            ou malvoyantes, qui écouteront ta réponse lue à voix haute.
            Règles :
            - Réponds uniquement en français, en phrases simples, sans liste ni mise en forme.
            - Commence par l'essentiel : ce que représente l'image, en une phrase.
            - Donne ensuite les détails utiles : personnes, objets, couleurs, disposition.
            - Lis mot pour mot tout texte visible dans l'image (panneau, étiquette, légende, menu).
            - Ne décris que ce que tu vois réellement. Si un élément est incertain ou illisible, dis-le.
            - Reste bref : cinq phrases au maximum.
            """;

    /** La demande quand l'utilisateur ne pose pas de question. */
    static final String DESCRIPTION_GENERALE = "Décris cette image.";

    private Consigne() {
    }

    /**
     * @param question la question de l'utilisateur, ou null pour une description générale
     * @return le texte de la demande à envoyer avec l'image
     */
    static String demande(String question) {
        if (question == null || question.isBlank()) {
            return DESCRIPTION_GENERALE;
        }
        return """
                L'utilisateur pose cette question sur l'image : « %s »
                Réponds d'abord directement à la question, à partir de ce que montre l'image. \
                Si l'image ne permet pas d'y répondre, dis-le clairement, puis décris brièvement l'image.\
                """.formatted(question.strip());
    }
}
