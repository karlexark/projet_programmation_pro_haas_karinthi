# Décrire une image à qui ne la voit pas

Application web d'audiodescription : l'utilisateur dépose une image, un agent d'IA l'analyse,
et l'application **lit à voix haute** la description de son contenu. Elle s'adresse d'abord aux
personnes aveugles ou malvoyantes, qui doivent pouvoir l'utiliser du début à la fin
**au clavier et avec un lecteur d'écran**, sans l'aide d'une personne voyante.

Projet réalisé par Alexandre KARINTHI et Malo HAAS (3iL, module Programmation professionnelle).

## Ce que fait l'application

1. L'utilisateur choisit une image (JPEG ou PNG, 10 Mo au maximum) et peut poser une question à son sujet,
   par exemple « Quelle est la date de péremption ? ».
2. La page envoie l'image et la question au serveur.
3. Le serveur les confie à l'agent, qui répond en français : une description (l'essentiel d'abord,
   puis les détails utiles et le texte présent dans l'image), ou la réponse à la question.
4. La page affiche la réponse et la lit à voix haute.
5. L'utilisateur peut relire, arrêter la lecture (bouton ou touche Échap) et recommencer avec une autre image.

L'agent dispose d'un **outil** qui lui donne la date et l'heure actuelles. Un modèle ne connaît pas
la date du jour et la devinerait ; avec l'outil, à la question « Ce yaourt est-il périmé ? », l'agent lit
la date sur l'emballage, appelle l'outil, compare, et répond par exemple : « Oui, ce yaourt est périmé.
La date indiquée est le 18 août 2024, et nous sommes en novembre 2026. » Chaque appel d'outil est
affiché dans la console du serveur.

En cas d'échec (fichier refusé, modèle arrêté, délai dépassé), un message en français dit ce qui
s'est passé et ce que l'utilisateur peut faire ; il est affiché, annoncé par le lecteur d'écran et lu à voix haute.

### Accessibilité

- Tout se fait au clavier ; chaque contrôle a une étiquette et une aide associée.
- Les messages d'attente, d'erreur et la description sont dans des zones annoncées par les lecteurs
  d'écran (`role="status"`, `role="alert"`, `aria-live`).
- La lecture automatique peut être désactivée par les personnes qui utilisent déjà un lecteur d'écran,
  pour ne pas entendre deux voix à la fois.
- Une voix française est choisie si le navigateur en propose une.
- Contrastes élevés, grands caractères, contour de focus visible, thème sombre automatique.

## Prérequis

- **Java 21** (JDK). Gradle n'a pas besoin d'être installé : le projet fournit `gradlew`.
- Pour la vraie implémentation : **[Ollama](https://ollama.com)** et le modèle **`qwen3.5:9b`**, qui gère
  à la fois les images et les appels d'outils (environ 7 Go ; une carte graphique avec 8 Go de mémoire vidéo
  est recommandée, sinon la description prend plusieurs dizaines de secondes).
- Un navigateur récent (Chrome, Edge ou Firefox) pour la synthèse vocale.

## Installation

```bash
git clone https://github.com/karlexark/projet_programmation_pro_haas_karinthi.git
cd projet_programmation_pro_haas_karinthi
```

Pour la vraie implémentation, installer Ollama depuis https://ollama.com/download, puis télécharger le modèle :

```bash
ollama pull qwen3.5:9b
```

## Lancement

Les commandes sont données pour un terminal Bash (Linux, macOS, Git Bash). Sous Windows PowerShell,
remplacer `./gradlew` par `.\gradlew`, et définir une variable d'environnement avec `$env:NOM="valeur"`
avant la commande (exemples ci-dessous).

### Avec le modèle (Ollama)

Vérifier qu'Ollama tourne (icône dans la barre des tâches, ou `ollama ps`), puis :

```bash
./gradlew bootRun
```

Ouvrir http://localhost:8080. La première description prend une quinzaine de secondes, le temps de charger
le modèle en mémoire ; les suivantes 1 à 3 secondes. Pour précharger le modèle avant une démonstration :
`ollama run qwen3.5:9b ""`.

L'ordre de lancement n'a pas d'importance : si Ollama est arrêté, la page l'annonce, et il suffit de le
démarrer puis de recommencer, sans relancer l'application.

### Avec la fausse implémentation (sans modèle)

La fausse implémentation n'appelle aucun modèle et renvoie une phrase fixe. L'application fonctionne
alors de bout en bout, page et synthèse vocale comprises, sur un poste où Ollama n'est pas installé.

```bash
./gradlew bootRun --args='--spring.profiles.active=faux'
```

Le changement d'agent se fait sans modifier une ligne de Java. Trois façons équivalentes :

| Méthode | Commande |
|---|---|
| Profil Spring | `./gradlew bootRun --args='--spring.profiles.active=faux'` |
| Propriété | `./gradlew bootRun --args='--descripteur.fournisseur=faux'` |
| Variable d'environnement (Bash) | `DESCRIPTEUR_FOURNISSEUR=faux ./gradlew bootRun` |
| Variable d'environnement (PowerShell) | `$env:DESCRIPTEUR_FOURNISSEUR="faux"; .\gradlew bootRun` |

Pour tester l'interface dans des conditions réalistes, la fausse implémentation peut simuler
un délai ou une panne (dans `src/main/resources/application-faux.properties`) :

```properties
descripteur.faux.delai=PT2S
descripteur.faux.echec=AGENT_INDISPONIBLE   # ou DELAI_DEPASSE, REPONSE_VIDE, INCONNUE
```

## Configuration

Dans `src/main/resources/application.properties` :

| Propriété | Variable d'environnement | Défaut | Rôle |
|---|---|---|---|
| `descripteur.fournisseur` | `DESCRIPTEUR_FOURNISSEUR` | `ollama` | agent utilisé : `ollama` ou `faux` |
| `descripteur.ollama.url` | `OLLAMA_URL` | `http://localhost:11434` | adresse du serveur Ollama |
| `descripteur.ollama.modele` | `OLLAMA_MODELE` | `qwen3.5:9b` | modèle (vision et outils) |
| `descripteur.ollama.timeout` | | `PT2M` | délai maximal d'une description |
| `descripteur.ollama.temperature` | | `0.1` | température (basse : réponses plus factuelles) |
| `descripteur.ollama.contexte` | | `8192` | taille du contexte en jetons (4096 par défaut dans Ollama, trop peu avec une image et un outil) |
| `descripteur.fuseau-horaire` | | `Europe/Paris` | fuseau de l'outil de date et d'heure |
| `spring.servlet.multipart.max-file-size` | | `10MB` | taille maximale d'une image |

Pour essayer un autre modèle, il doit gérer **les images et les outils** : `ollama show <modèle>` doit
afficher les capacités `vision` et `tools` (ce n'est pas le cas de `qwen2.5vl` ni de `llama3.2-vision`).
Préférer une variante sans raisonnement préalable, plus rapide (`qwen3-vl:8b-instruct` plutôt que `qwen3-vl:8b`) :

```bash
ollama pull qwen3-vl:8b-instruct
OLLAMA_MODELE=qwen3-vl:8b-instruct ./gradlew bootRun
```

Sous PowerShell :

```powershell
ollama pull qwen3-vl:8b-instruct
$env:OLLAMA_MODELE="qwen3-vl:8b-instruct"; .\gradlew bootRun
```

La comparaison des modèles essayés est dans [docs/evaluation-consigne.md](docs/evaluation-consigne.md).

Une variable définie avec `$env:` reste active jusqu'à la fermeture du terminal PowerShell.

## Tests et qualité du code

Une seule commande lance toutes les vérifications :

```bash
./gradlew check
```

Elle échoue au moindre écart. Elle exécute :

| Outil | Ce qu'il vérifie | Règle | Rapport |
|---|---|---|---|
| **JUnit 5** | le comportement (86 tests) | tous les tests passent | `build/reports/tests/test/index.html` |
| **JaCoCo** | la couverture des tests | au moins 85 % des lignes et 75 % des branches | `build/reports/jacoco/test/html/index.html` |
| **SpotBugs** | les bugs probables, dans le code compilé | aucun avertissement (effort maximal) | `build/reports/spotbugs/main.html` |
| **Checkstyle** | les conventions d'écriture (nommage, Javadoc, accolades…) | aucune infraction | `build/reports/checkstyle/main.html` |
| **Spotless** | la mise en forme (palantir-java-format) | code formaté | message dans la console |

Couverture actuelle : 94 % des lignes, 84 % des branches.

Les règles de Checkstyle sont propres au projet et justifiées dans
[config/checkstyle/checkstyle.xml](config/checkstyle/checkstyle.xml).

**Avant chaque commit**, mettre le code en forme automatiquement, puis vérifier :

```bash
./gradlew spotlessApply check
```

### Ce que couvrent les tests

Les tests ne demandent pas Ollama :

- les types du contrat (`Image`, `Description`, l'exception) : invariants, copie défensive, exception vérifiée ;
- la validation de l'image (type annoncé et signature réelle) et de la question (longueur, question vide) ;
- le contrôleur et la traduction des erreurs, avec un agent simulé (`@WebMvcTest`) ;
- l'outil de date (horloge fixe, changement de jour selon le fuseau) ;
- la boucle de l'agent : outil exécuté et résultat renvoyé au modèle, outil inconnu, arrêt au bout de 4 tours ;
- l'agent LangChain4J, avec un modèle simulé, et face à un faux serveur Ollama local
  (réponse normale, appel d'outil au format d'Ollama, connexion refusée, délai dépassé, modèle absent) ;
- le choix de l'implémentation selon la propriété `descripteur.fournisseur`.

La consigne envoyée au modèle a été réglée sur un jeu d'images dont le contenu est connu :
voir [docs/evaluation-consigne.md](docs/evaluation-consigne.md).

## Architecture

```
Navigateur                        Serveur Spring Boot
index.html, app.js    -- POST -->  DescriptionControleur
voix.js (Web Speech)  <-- JSON --  ValidateurImage, ValidateurQuestion, GestionnaireErreurs
                                          |
                                          | ne connaît que l'interface
                                          v
                                   DescripteurImage
                                    |            |
                 descripteur.fournisseur=ollama  descripteur.fournisseur=faux
                                    v            v
          DescripteurLangChain4j            FauxDescripteur
          (boucle d'agent)                  (réponse fixe)
          + OutilsAgent (date et heure)
          + OllamaConfiguration
                    |
                    v
             Ollama (local), qwen3.5:9b
```

- Le contrôleur reçoit l'agent par son constructeur et ne connaît que l'interface `DescripteurImage` :
  il ignore Ollama et LangChain4J.
- `DescripteurLangChain4j` est un **adaptateur** : il traduit les types de l'application (`Image`,
  `Description`) en messages LangChain4J, et les erreurs de la bibliothèque en `DescriptionImpossibleException`.
  Il ne dépend d'aucun fournisseur : ajouter un fournisseur (par exemple OpenRouter) revient à écrire une
  nouvelle classe de configuration, sans modifier le contrôleur ni l'adaptateur.
- C'est un **agent** : `DescripteurLangChain4j` présente au modèle les outils de `OutilsAgent` (méthodes
  annotées `@Tool`). Si le modèle demande un outil, l'agent l'exécute et lui renvoie le résultat, puis le
  modèle reprend ; au bout de 4 allers-retours sans réponse, la demande échoue proprement. L'ajout de l'outil
  n'a modifié ni l'interface `DescripteurImage`, ni le contrôleur, ni la page.
- Le fournisseur est choisi par la propriété `descripteur.fournisseur`.

### Organisation du code

```
src/main/java/fr/troisil/audiodescription/
├── agent/                       le contrat : DescripteurImage, Image, Description, DescriptionImpossibleException
│   ├── faux/                    FauxDescripteur
│   └── langchain4j/             DescripteurLangChain4j, Consigne, OutilsAgent
│       └── ollama/              OllamaConfiguration
└── web/                         DescriptionControleur, ValidateurImage, ValidateurQuestion, GestionnaireErreurs
src/main/resources/static/       index.html, style.css, app.js, voix.js
```

### API

`POST /api/descriptions` (multipart) : champ `image` (obligatoire) et champ `question` (facultatif).

- Succès (200) : `{"texte": "..."}`
- Échec : `{"code": "...", "message": "..."}`, avec un message destiné à l'utilisateur
  (400 fichier refusé ou question trop longue, 413 fichier trop lourd, 502, 503 ou 504 agent en échec).
