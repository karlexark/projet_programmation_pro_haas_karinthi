"use strict";

/**
 * Lecture à voix haute avec la Web Speech API (speechSynthesis), dans le navigateur.
 *
 * - Choisit une voix française si le navigateur en propose une, sans supposer qu'elle existe.
 * - Découpe le texte en phrases : certains navigateurs coupent les énoncés trop longs.
 * - Ne fait rien (sans erreur) si le navigateur ne sait pas parler.
 */
const Voix = (() => {
    const synthese = window.speechSynthesis;
    let voixFrancaise = null;

    function choisirVoix() {
        const voix = synthese.getVoices();
        voixFrancaise =
            voix.find((v) => v.lang === "fr-FR") ||
            voix.find((v) => v.lang.toLowerCase().startsWith("fr")) ||
            null;
    }

    if (synthese) {
        choisirVoix();
        // Les voix sont souvent chargées après la page : on refait le choix quand elles arrivent.
        synthese.addEventListener("voiceschanged", choisirVoix);
    }

    function decouperEnPhrases(texte) {
        return texte.match(/[^.!?…]+[.!?…]*/g)?.map((p) => p.trim()).filter(Boolean) ?? [texte];
    }

    return {
        /** @returns {boolean} vrai si le navigateur sait lire à voix haute */
        estDisponible() {
            return Boolean(synthese);
        },

        /** Lit le texte en interrompant toute lecture en cours. */
        lire(texte) {
            if (!synthese || !texte) {
                return;
            }
            synthese.cancel();
            for (const phrase of decouperEnPhrases(texte)) {
                const enonce = new SpeechSynthesisUtterance(phrase);
                enonce.lang = "fr-FR";
                if (voixFrancaise) {
                    enonce.voice = voixFrancaise;
                }
                synthese.speak(enonce);
            }
        },

        /** Arrête immédiatement la lecture en cours. */
        arreter() {
            if (synthese) {
                synthese.cancel();
            }
        },
    };
})();
