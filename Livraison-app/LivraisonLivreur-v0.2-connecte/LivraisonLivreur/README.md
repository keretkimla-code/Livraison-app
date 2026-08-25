# Livraison Livreur — v0.2 (Flutter / Dart) — connectée au backend FastAPI

Application coursier pour le projet de livraison type Yango/Uber (Tchad).
**Cette version est connectée au vrai backend** (`livraison-backend`,
FastAPI) — ce n'est plus une simulation en mémoire comme la v0.1.

## ⚠️ Avant de lancer : configure l'adresse du backend

Ouvre `lib/services/api_client.dart` et adapte `baseUrl` selon où tu
testes :

| Contexte de test | `baseUrl` à utiliser |
|---|---|
| Émulateur Android | `http://10.0.2.2:8000` (déjà configuré par défaut) |
| Simulateur iOS | `http://127.0.0.1:8000` |
| Téléphone physique (même Wi-Fi) | `http://<IP locale de ton PC>:8000`, backend lancé avec `uvicorn app.main:app --host 0.0.0.0` |
| Backend déployé en ligne | son URL publique en HTTPS |

Lance le backend (`livraison-backend`) **avant** de démarrer l'app :
```bash
cd livraison-backend
venv/bin/uvicorn app.main:app --host 0.0.0.0 --reload
```

## Ce qui est réellement connecté à l'API

- **Inscription** : envoi/vérification d'un vrai code OTP côté serveur
  (code universel de test `0000`), token JWT stocké en mémoire
- **Dossier livreur** : envoyé et validé via `POST /couriers/profile`
- **Disponibilité** : `PATCH /couriers/me/availability`
- **Demandes proches** : `GET /orders/nearby`, interrogé toutes les 8
  secondes tant que tu es disponible (matching géographique réel côté
  serveur, basé sur la distance à vol d'oiseau)
- **Acceptation** : `POST /orders/{id}/accept`
- **Progression du trajet** : `PATCH /orders/{id}/status` (collecte →
  livraison), déclenché automatiquement quand l'animation locale de la
  carte atteint sa destination
- **Confirmation de livraison** : `POST /orders/{id}/confirm-delivery`
  avec le vrai code généré par le serveur
- **Gains et historique** : `GET /couriers/me` et `GET /orders/history`

## Ce qui reste simulé côté app (documenté dans le code)

- **Position GPS** : générée aléatoirement autour de N'Djamena au lieu
  d'être lue depuis le capteur réel → intégrer le package `geolocator`
  en V1
- **Upload de documents** : juste un bouton coché, aucun fichier envoyé
  → intégrer `image_picker` + un endpoint d'upload en V1
- **Animation de la carte** : dessinée localement pour le retour visuel
  (les vrais changements de statut, eux, sont bien envoyés au serveur)

## Point important : le code de livraison

Normalement, c'est le **client** qui affiche ce code sur son app et le
communique de vive voix au livreur à la livraison. Comme l'app Client
n'est pas encore connectée à ce même backend, le code est temporairement
affiché directement dans l'app Livreur pour te permettre de tester le
parcours complet tout seul. Ce sera retiré une fois les deux apps
connectées simultanément.

## Comment lancer le projet

```bash
flutter create --platforms=android,ios .
flutter pub get
flutter run
```

## Parcours de test recommandé

1. Démarre le backend
2. Lance l'app, inscris-toi comme livreur (téléphone + `0000` + dossier
   véhicule)
3. Passe "Disponible"
4. Ouvre `http://<backend>/docs` dans un navigateur (ou utilise `curl`)
   pour créer un client et une commande manuellement — voir le README du
   backend pour les commandes exactes
5. La demande doit apparaître dans l'app Livreur sous 8 secondes
6. Accepte, suis le trajet simulé, confirme la livraison avec le code
   affiché
