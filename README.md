# Décrire une image à qui ne la voit pas

Application web (Spring Boot + LangChain4J) qui décrit à voix haute une image déposée par l'utilisateur.

## Prérequis
- Java 21 (JDK)
- Pour la vraie implémentation : un serveur Ollama avec un modèle de vision (par défaut `qwen2.5vl`)

## Lancer avec la fausse implémentation (sans modèle)
```bash
./gradlew bootRun --args='--spring.profiles.active=faux'
```

## Lancer avec Ollama
Ollama tourne sur une VM. Deux possibilités :

1. **Tunnel SSH** (recommandé, rien à ouvrir sur la VM) :
   ```bash
   ssh -N -L 11434:localhost:11434 utilisateur@adresse-vm
   ./gradlew bootRun
   ```
2. **Adresse directe** (Ollama lancé avec `OLLAMA_HOST=0.0.0.0` sur la VM) :
   ```bash
   OLLAMA_URL=http://adresse-vm:11434 ./gradlew bootRun
   ```

Puis ouvrir http://localhost:8080.

## Installation d'Ollama sur la VM
```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama pull qwen2.5vl
```
