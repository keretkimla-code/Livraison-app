# Livraison Client — v0.2 (Kotlin) — connectée au backend FastAPI

Application cliente Android pour le projet de livraison type Yango/Uber
(Tchad). **Cette version est connectée au vrai backend** (`livraison-backend`,
FastAPI) — ce n'est plus une simulation en mémoire.

## ⚠️ Avant de lancer : configure l'adresse du backend

Ouvre `app/src/main/java/com/livraison/client/network/RetrofitClient.kt`
et adapte `baseUrl` :

| Contexte de test | `baseUrl` à utiliser |
|---|---|
| Émulateur Android | `http://10.0.2.2:8000/` (déjà configuré par défaut) |
| Téléphone physique (même Wi-Fi) | `http://<IP locale de ton PC>:8000/`, backend lancé avec `--host 0.0.0.0` |
| Backend déployé (ex. URL publique GitHub Codespaces) | son URL HTTPS, avec le `/` final |

Lance le backend **avant** de démarrer l'app :
```bash
cd livraison-backend
venv/bin/uvicorn app.main:app --host 0.0.0.0 --reload
```

## Ce qui est réellement connecté à l'API

- **Inscription/connexion** : vrai code OTP côté serveur (code universel
  de test `0000`), token JWT
- **Estimation de prix** : `POST /orders/estimate`, calculée sur la vraie
  distance entre les deux adresses
- **Recherche d'adresse** : `GET /geocode/search`, avec suggestions en
  temps réel pendant la saisie (proxy Nominatim/OpenStreetMap côté
  serveur, restreint au Tchad) — il faut choisir une suggestion dans la
  liste pour que l'adresse ait des coordonnées GPS valides
- **Création de commande** : `POST /orders`
- **Suivi de commande** : `GET /orders/{id}` interrogé toutes les 3
  secondes tant que la commande n'est pas arrivée à destination
- **Chat avec le livreur** : `GET/POST /orders/{id}/messages`, rafraîchi
  toutes les 3 secondes
- **Paiement** : `POST /orders/{id}/pay`
- **Historique** : `GET /orders/history`
- **Notation** : `POST /orders/{id}/rate`

Le **code de livraison** à donner au livreur (généré par le serveur) est
maintenant affiché directement dans l'app Client, sur l'écran de suivi —
c'est bien lui qui doit le communiquer au livreur, comme dans le vrai
parcours.

## Ce qui reste simulé côté app (documenté dans le code)

- **Position du livreur sur la carte** : déduite du statut de la
  commande, pas d'un flux GPS temps réel → prévu via WebSocket en V1
  (le backend expose déjà `/ws/orders/{id}` pour ça)

## Comment lancer le projet

1. Ouvre le dossier `LivraisonClient` dans Android Studio
2. Laisse Gradle se synchroniser (il proposera de générer le wrapper si
   besoin)
3. Vérifie/adapte `baseUrl` (voir plus haut)
4. Lance sur un émulateur ou un téléphone (minSdk 24)

## Parcours de test recommandé

1. Démarre le backend
2. Inscris-toi comme client (téléphone + `0000`)
3. Crée une commande (adresses + type de colis) → estimation → confirmation
4. Dans l'app Livreur (v0.2, connectée au même backend), passe
   disponible : la commande doit apparaître sous 8 secondes
5. Accepte côté livreur, suis la progression côté client
6. Une fois arrivé, donne le code affiché dans l'app Client au livreur
   pour qu'il confirme la livraison
7. Paye, puis note le livreur depuis l'historique
