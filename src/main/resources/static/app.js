"use strict";

const URL_DESCRIPTION = "/api/descriptions";
const MESSAGE_ATTENTE =
    "Analyse de l'image en cours. Cela peut prendre jusqu'à une minute, merci de patienter.";
const MESSAGE_SERVEUR_INJOIGNABLE =
    "Le serveur ne répond pas. Vérifiez votre connexion, puis réessayez.";
const MESSAGE_SANS_IMAGE =
    "Aucune image n'a été choisie. Choisissez une image, puis relancez la description.";

/** Codes d'erreur renvoyés par le serveur qui concernent le fichier choisi. */
const ERREURS_DE_FICHIER = ["ABSENTE", "VIDE", "FORMAT_NON_PRIS_EN_CHARGE", "ILLISIBLE", "TROP_LOURDE"];

const formulaire = document.getElementById("formulaire");
const champImage = document.getElementById("image");
const champQuestion = document.getElementById("question");
const bouton = document.getElementById("decrire");
const zoneEtat = document.getElementById("etat");
const zoneErreur = document.getElementById("erreur");
const zoneDescription = document.getElementById("description");
const apercu = document.getElementById("apercu");

let analyseEnCours = false;

formulaire.addEventListener("submit", async (evenement) => {
    evenement.preventDefault();
    if (analyseEnCours) {
        return;
    }
    const fichier = champImage.files[0];
    if (!fichier) {
        afficherErreur(MESSAGE_SANS_IMAGE);
        champImage.focus();
        return;
    }
    await decrire(fichier, champQuestion.value);
});

champImage.addEventListener("change", () => {
    effacerErreur();
    afficherApercu(champImage.files[0]);
});

async function decrire(fichier, question) {
    commencerAttente();
    try {
        const donnees = new FormData();
        donnees.append("image", fichier);
        donnees.append("question", question);

        const reponse = await fetch(URL_DESCRIPTION, { method: "POST", body: donnees });
        const corps = await lireJson(reponse);

        if (reponse.ok && corps && corps.texte) {
            afficherDescription(corps.texte);
        } else {
            traiterEchec(corps);
        }
    } catch (erreurReseau) {
        afficherErreur(MESSAGE_SERVEUR_INJOIGNABLE);
    } finally {
        terminerAttente();
    }
}

async function lireJson(reponse) {
    try {
        return await reponse.json();
    } catch {
        return null;
    }
}

function traiterEchec(corps) {
    const message = corps && corps.message
        ? corps.message
        : "Une erreur inattendue s'est produite. Réessayez.";
    afficherErreur(message);
    if (corps && ERREURS_DE_FICHIER.includes(corps.code)) {
        champImage.focus();
    }
}

function commencerAttente() {
    analyseEnCours = true;
    effacerErreur();
    // Vider la zone n'est pas annoncé par les lecteurs d'écran (seuls les ajouts le sont).
    zoneDescription.replaceChildren();
    bouton.setAttribute("aria-disabled", "true");
    formulaire.setAttribute("aria-busy", "true");
    zoneEtat.textContent = MESSAGE_ATTENTE;
}

function terminerAttente() {
    analyseEnCours = false;
    bouton.removeAttribute("aria-disabled");
    formulaire.removeAttribute("aria-busy");
    zoneEtat.textContent = "";
}

function afficherDescription(texte) {
    const paragraphe = document.createElement("p");
    paragraphe.textContent = texte;
    zoneDescription.replaceChildren(paragraphe);
    apercu.alt = texte;
}

function afficherErreur(message) {
    zoneErreur.textContent = message;
}

function effacerErreur() {
    zoneErreur.textContent = "";
}

function afficherApercu(fichier) {
    if (apercu.src) {
        URL.revokeObjectURL(apercu.src);
    }
    if (!fichier) {
        apercu.removeAttribute("src");
        apercu.hidden = true;
        return;
    }
    apercu.src = URL.createObjectURL(fichier);
    apercu.alt = "Image choisie, pas encore décrite.";
    apercu.hidden = false;
}
