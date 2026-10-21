"use strict";

const URL_DESCRIPTION = "/api/descriptions";
const MESSAGE_ATTENTE =
    "Analyse de l'image en cours. Cela prend quelques secondes, et jusqu'à deux minutes la première fois. Merci de patienter.";
const MESSAGE_SERVEUR_INJOIGNABLE =
    "Le serveur ne répond pas. Vérifiez votre connexion, puis réessayez.";
const MESSAGE_SANS_IMAGE =
    "Aucune image n'a été choisie. Choisissez une image, puis relancez la description.";
const CLE_LECTURE_AUTO = "lecture-auto";

/** Codes d'erreur renvoyés par le serveur qui concernent le fichier choisi. */
const ERREURS_DE_FICHIER = ["ABSENTE", "VIDE", "FORMAT_NON_PRIS_EN_CHARGE", "ILLISIBLE", "TROP_LOURDE"];

const formulaire = document.getElementById("formulaire");
const champImage = document.getElementById("image");
const champQuestion = document.getElementById("question");
const caseLectureAuto = document.getElementById("lecture-auto");
const bouton = document.getElementById("decrire");
const zoneEtat = document.getElementById("etat");
const zoneErreur = document.getElementById("erreur");
const zoneDescription = document.getElementById("description");
const zoneActions = document.getElementById("actions");
const boutonRelire = document.getElementById("relire");
const boutonArreter = document.getElementById("arreter");
const boutonNouvelle = document.getElementById("nouvelle");
const apercu = document.getElementById("apercu");

let analyseEnCours = false;
let derniereDescription = "";
let minuterieAnnonce;

initialiserLectureAuto();

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
    const fichier = champImage.files[0];
    afficherApercu(fichier);
    // Au retour de la fenêtre de choix, certains lecteurs d'écran relisent l'ancien état
    // du sélecteur (« aucun fichier choisi ») : la page annonce donc elle-même le choix.
    const message = fichier
        ? `Image ${fichier.name} choisie. Vous pouvez écrire une question, puis lancer la description.`
        : "Aucune image choisie.";
    annoncer(message);
    parler(message);
});

boutonRelire.addEventListener("click", () => Voix.lire(derniereDescription));
boutonArreter.addEventListener("click", () => Voix.arreter());
boutonNouvelle.addEventListener("click", recommencer);

document.addEventListener("keydown", (evenement) => {
    if (evenement.key === "Escape") {
        Voix.arreter();
    }
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
    } else if (corps && corps.code === "QUESTION_TROP_LONGUE") {
        champQuestion.focus();
    }
}

function commencerAttente() {
    analyseEnCours = true;
    effacerErreur();
    // Vider la zone n'est pas annoncé par les lecteurs d'écran (seuls les ajouts le sont).
    zoneDescription.replaceChildren();
    zoneActions.hidden = true;
    derniereDescription = "";
    bouton.setAttribute("aria-disabled", "true");
    formulaire.setAttribute("aria-busy", "true");
    annoncer(MESSAGE_ATTENTE);
    parler(MESSAGE_ATTENTE);
}

function terminerAttente() {
    analyseEnCours = false;
    bouton.removeAttribute("aria-disabled");
    formulaire.removeAttribute("aria-busy");
    effacerAnnonce();
}

function afficherDescription(texte) {
    derniereDescription = texte;
    const paragraphe = document.createElement("p");
    paragraphe.textContent = texte;
    zoneDescription.replaceChildren(paragraphe);
    // La description est déjà lue juste au-dessus : la répéter ici la ferait lire deux fois.
    apercu.alt = "Image choisie, décrite ci-dessus.";
    zoneActions.hidden = false;
    boutonRelire.hidden = !Voix.estDisponible();
    boutonArreter.hidden = !Voix.estDisponible();
    parler(texte);
}

/**
 * Écrit un message dans la zone d'état, que le lecteur d'écran annonce.
 * La zone est vidée puis remplie un instant plus tard : un même message répété
 * (la même image choisie deux fois) est ainsi annoncé de nouveau.
 */
function annoncer(message) {
    effacerAnnonce();
    minuterieAnnonce = setTimeout(() => {
        zoneEtat.textContent = message;
    }, 100);
}

function effacerAnnonce() {
    clearTimeout(minuterieAnnonce);
    zoneEtat.textContent = "";
}

function afficherErreur(message) {
    zoneErreur.textContent = message;
    parler(message);
}

function effacerErreur() {
    zoneErreur.textContent = "";
}

/** Remet la page dans son état initial et place le focus sur le choix de l'image. */
function recommencer() {
    Voix.arreter();
    formulaire.reset();
    caseLectureAuto.checked = lectureAutoMemorisee();
    afficherApercu(null);
    zoneDescription.replaceChildren();
    zoneActions.hidden = true;
    derniereDescription = "";
    effacerErreur();
    effacerAnnonce();
    champImage.focus();
}

/** Lit le texte à voix haute si l'utilisateur a gardé la lecture automatique. */
function parler(texte) {
    if (caseLectureAuto.checked) {
        Voix.lire(texte);
    }
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

/** Le choix de lecture automatique est mémorisé dans le navigateur, si c'est possible. */
function initialiserLectureAuto() {
    if (!Voix.estDisponible()) {
        caseLectureAuto.checked = false;
        caseLectureAuto.disabled = true;
        return;
    }
    caseLectureAuto.checked = lectureAutoMemorisee();
    caseLectureAuto.addEventListener("change", () => {
        if (!caseLectureAuto.checked) {
            Voix.arreter();
        }
        try {
            localStorage.setItem(CLE_LECTURE_AUTO, String(caseLectureAuto.checked));
        } catch {
            // Stockage indisponible (navigation privée…) : le choix vaut pour cette visite seulement.
        }
    });
}

function lectureAutoMemorisee() {
    try {
        return localStorage.getItem(CLE_LECTURE_AUTO) !== "false";
    } catch {
        return true;
    }
}
