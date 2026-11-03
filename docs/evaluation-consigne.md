# Évaluation de la consigne envoyée au modèle

Modèle : `qwen2.5vl:7b` (Ollama, en local sur une RTX 4080), température 0,1. Voir aussi la mise à jour du 3 novembre en fin de document (outil de date, passage à `qwen3.5:9b`).
Date : 15 octobre 2026.

## Protocole

Six images dont nous connaissons le contenu, et onze cas (avec et sans question) :

| Image | Contenu réel | Questions posées |
|---|---|---|
| `lampe.jpg` | lampe en forme de cœur à paillettes roses, socle doré | — |
| `panneau_attention.jpg` | panneau « danger » avec le panonceau « RANDONNEURS », route de campagne | « Que dit le panneau ? » |
| `piège.jpg` | pomme verte avec un papier où est écrit « iPod » (ce n'est **pas** un iPod) | — |
| `situation.jpg` | dans une rue, un homme se retourne pour regarder une femme en rouge ; la femme qui l'accompagne a l'air choquée | « Que fait l'homme ? » |
| `stade_raf.jpg` | stade vide, sièges jaunes et rouges, banderoles « AVEYRON », « Occitanie » | « Quel est le nom de ce stade ? » |
| `yaourt.jpg` | opercule : dessin de brebis, Nutri-Score **B**, « À consommer jusqu'au : 18/08/24 », logo AB | « Quelle est la date de péremption ? », « Quel est le Nutri-Score ? » |

Les images ne sont pas versionnées (dossier `image_test/`, ignoré par Git).

## Versions essayées

| Version | Changement | Résultat |
|---|---|---|
| v0 | consigne initiale | réponses aux questions suivies de **toute la description** ; texte du stade **non lu** (« des inscriptions sont présentes ») ; conversation inventée dans la scène de rue |
| v1 | réponses aux questions en 1 ou 2 phrases ; texte à **citer entre guillemets** ; doute exprimé plutôt qu'affirmé ; température 0,1 | **nette amélioration** : « Le panneau dit "RANDONNEURS". », « La date de péremption est le 18/08/24. » ; « Occitanie », « AVEYRON » cités ; plus d'invention dans la scène de rue |
| v2 | v1 + règles supplémentaires (regard de chaque personne, ne pas parler du texte absent) | **moins bonne** : texte inventé (« 2015 ») dans la scène de rue, texte du stade de nouveau non lu |
| v3 | v1 + « ne répète jamais une information » | texte du stade non lu |
| **retenue** | **v1**, avec une phrase de repli plus simple pour les questions sans réponse | voir ci-dessous |

## Constats

1. **Une consigne plus longue n'est pas meilleure.** Avec un modèle de 7 milliards de paramètres,
   ajouter des règles a dégradé les réponses (v2).
2. **Les réponses varient d'un essai à l'autre**, même à température 0,1 et à consigne identique :
   le texte du stade est cité une fois sur deux, la brebis est reconnue une fois sur deux.
   Comparer deux consignes sur un seul essai ne suffit donc pas ; il faut répéter les essais.
3. **Ce que la consigne a corrigé** : la longueur des réponses aux questions, la citation du texte,
   les relations inventées entre les personnes.
4. **Ce qu'elle ne corrige pas (limites du modèle)** :
   - le Nutri-Score est toujours lu « A » au lieu de « B » (la lettre mise en évidence n'est pas reconnue) ;
   - le regard de l'homme dans la scène de rue (il se retourne vers la femme en rouge) n'est jamais compris ;
   - l'animal du yaourt est souvent décrit comme une vache.
5. **Questions sans réponse** : le modèle reconnaît qu'il ne peut pas donner le nom du stade,
   au lieu d'en inventer un.

## Pistes (octobre)

- Essayer un modèle plus grand ou différent (`gemma3:12b`, `llama3.2-vision:11b`) sur les mêmes cas :
  le choix du modèle se fait dans `application.properties`, sans modifier le code.
- Répéter chaque cas plusieurs fois pour mesurer un taux de réussite plutôt qu'un résultat unique.

---

# Mise à jour du 3 novembre : un outil pour l'agent, et un nouveau modèle

## Pourquoi changer de modèle

L'agent dispose désormais d'un outil, `dateEtHeureActuelles` : un modèle ne connaît pas la date du jour,
et sans outil il la devinerait. L'outil permet de répondre à « Ce yaourt est-il périmé ? ».

`qwen2.5vl:7b` **ne sait pas appeler d'outil** (capacités annoncées par Ollama : `completion`, `vision`).
Trois modèles de vision avec outils tenant dans 12 Go ont été comparés :

| Modèle | Constat | Retenu |
|---|---|---|
| `qwen3-vl:8b` | c'est en fait la variante « thinking » : elle raisonne avant chaque réponse, même avec `think: false` ; 10 à 57 s par réponse, deux réponses vides et une coupée (contexte de 4096 jetons dépassé) | non |
| `qwen3-vl:8b-instruct` | variante sans raisonnement | non essayé |
| **`qwen3.5:9b`** | `think: false` coupe bien le raisonnement ; **1 à 3 s** par réponse ; l'image ne coûte que 565 jetons ; 5,9 Go en mémoire vidéo | **oui** |

Le contexte est porté à 8192 jetons (propriété `descripteur.ollama.contexte`) : avec celui d'Ollama
par défaut (4096), l'image, la consigne, la description de l'outil et la réponse ne tenaient pas toujours.

## Résultats avec `qwen3.5:9b`

Treize cas : les onze d'octobre, plus « Ce yaourt est-il périmé ? » et « Quel jour sommes-nous ? »
(posée sur l'image du panneau). Les cas sensibles à l'outil ont été passés deux fois.

| Cas | Résultat |
|---|---|
| « Quel est le Nutri-Score ? » | **« Le Nutri-Score est B. »** (2 sur 2) — `qwen2.5vl` répondait toujours A |
| « Quelle est la date de péremption ? » | « 18/08/24 » (2 sur 2) |
| « Ce yaourt est-il périmé ? » | l'outil est appelé, puis « périmé, car la date indiquée est le 18 août 2024 et nous sommes en novembre 2026 » (2 sur 2) |
| « Quel jour sommes-nous ? » | l'outil est appelé une fois sur deux ; l'autre fois, le modèle dit ne pas pouvoir répondre (sans inventer de date) |
| Panneau, pomme « iPod », lampe | justes ; « RANDONNEURS » et « iPod » cités |
| Scène de rue | plus d'invention de conversation ; l'homme « regarde par-dessus son épaule » |
| Stade, description générale | **texte inventé** : « AVIGNON », « GDF SUEZ » au lieu de « AVEYRON », « Occitanie » |

## Réglage de la consigne pour l'outil

- Première version de la règle (« si la réponse dépend du jour, par exemple d'une date de péremption,
  appelle l'outil ») : le modèle appelait l'outil **trop souvent**, et à « Quel est le Nutri-Score ? »
  il répondait sur la date de péremption.
- Règle retenue : appeler l'outil **uniquement** quand la question porte sur le jour ou l'heure présents,
  ou demande si une date est passée ; et « réponds exactement à la question posée ». L'outil n'est
  plus appelé hors de propos, et le Nutri-Score est de nouveau bien lu.

## Constats

1. Le choix du modèle a plus d'effet que la consigne : `qwen3.5:9b` lit correctement le Nutri-Score,
   ce qu'aucune consigne n'avait obtenu de `qwen2.5vl`.
2. Le nom d'un modèle ne garantit pas son comportement : `qwen3-vl:8b` est une variante qui raisonne
   toujours ; il faut vérifier (capacités annoncées, temps de réponse, jetons consommés).
3. Une règle de consigne sur un outil se règle comme le reste : trop large, l'outil est appelé à tort ;
   trop stricte, il ne l'est plus. Le changement de modèle s'est fait sans modifier le code de l'agent
   ni le contrôleur : seule la propriété `descripteur.ollama.modele` a changé.
