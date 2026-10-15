# Évaluation de la consigne envoyée au modèle

Modèle : `qwen2.5vl:7b` (Ollama, en local sur une RTX 4080), température 0,1.
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

## Pistes

- Essayer un modèle plus grand ou différent (`gemma3:12b`, `llama3.2-vision:11b`) sur les mêmes cas :
  le choix du modèle se fait dans `application.properties`, sans modifier le code.
- Répéter chaque cas plusieurs fois pour mesurer un taux de réussite plutôt qu'un résultat unique.
