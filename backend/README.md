# Livraison API — Backend (bêta)

Backend central du projet de livraison type Yango/Uber (Tchad), en
**Python + FastAPI**. Il sert les deux applications déjà développées :
**LivraisonClient** (Kotlin) et **LivraisonLivreur** (Flutter).

Ce backend a été **testé de bout en bout** (inscription, matching
géographique, cycle de vie complet d'une commande, paiement, notation) —
voir la section "Tester rapidement" ci-dessous.

## Ce que couvre cette version bêta

- **Authentification par OTP** simulée (aucun vrai SMS envoyé — code
  universel de test `0000`), avec token JWT
- **Inscription livreur** avec dossier (pièce d'identité + véhicule) —
  auto-validé en bêta ; en V1, un admin doit valider manuellement
- **Statut disponible/indisponible** + position GPS du livreur
- **Estimation de prix** et **matching géographique réel** (distance
  Haversine calculée en Python — pas besoin de PostGIS pour développer)
- **Cycle de vie complet d'une commande** : création → recherche de
  livreurs proches → acceptation → collecte → livraison → confirmation
  par code → paiement → notation
- **WebSocket temps réel** (`/ws/orders/{order_id}`) qui diffuse les
  changements de statut aux apps connectées
- Base de données **SQLite** par défaut (fichier local, zéro configuration)

## Ce qu'il reste à faire pour la V1 (production)

1. **PostgreSQL + PostGIS** à la place de SQLite (change juste
   `DATABASE_URL` dans `.env` — le code utilise SQLAlchemy, la migration
   est presque transparente ; pour de gros volumes, remplacer le calcul
   Haversine en Python par des requêtes spatiales natives PostGIS)
2. **Vrai envoi de SMS** pour l'OTP (API Airtel/Moov ou un fournisseur
   comme Twilio) — voir `app/routers/auth.py`
3. **Validation manuelle des livreurs** par un admin, au lieu de
   l'auto-validation actuelle — voir le `TODO` dans `app/routers/couriers.py`
4. **Intégration Mobile Money réelle** (Airtel Money / Moov Money) pour
   le paiement — voir le `TODO` dans `app/routers/orders.py`
5. **Migrations de schéma** avec Alembic, au lieu du `create_all()`
   automatique
6. **Redis** pour le stockage des OTP et le Pub/Sub WebSocket, si le
   backend est déployé sur plusieurs instances
7. **HTTPS obligatoire** + restreindre les origines CORS en production
8. Brancher les deux apps mobiles sur cette API (remplacer les données
   simulées dans `AppViewModel.kt` et `app_state.dart`)

## Installer et lancer en local

```bash
python3 -m venv venv
venv/bin/pip install -r requirements.txt
cp .env.example .env
venv/bin/uvicorn app.main:app --reload
```

L'API est alors disponible sur `http://127.0.0.1:8000`, avec une
documentation interactive auto-générée sur `http://127.0.0.1:8000/docs`
(Swagger UI — pratique pour tester chaque endpoint à la main).

## Tester rapidement le parcours complet

```bash
# 1. Le client s'inscrit/se connecte (code de test : 0000)
curl -X POST localhost:8000/auth/verify-otp -H "Content-Type: application/json" \
  -d '{"phone": "+23566000001", "code": "0000", "role": "client", "full_name": "Ton nom"}'

# 2. Le livreur s'inscrit/se connecte
curl -X POST localhost:8000/auth/verify-otp -H "Content-Type: application/json" \
  -d '{"phone": "+23566000002", "code": "0000", "role": "courier", "full_name": "Nom du livreur"}'

# (utilise les access_token retournés comme "Authorization: Bearer <token>"
#  pour la suite — voir /docs pour la liste complète des routes)
```

Toutes les routes sont documentées et testables directement dans
`/docs`.

## Structure du projet

```
app/
├── main.py                → point d'entrée FastAPI, CORS, routes
├── config.py               → variables d'environnement, tarification
├── database.py              → connexion SQLAlchemy
├── models.py                 → tables : User, CourierProfile, Order, Rating, ChatMessage
├── schemas.py                 → schémas Pydantic (requêtes/réponses)
├── security.py                 → JWT, dépendances d'authentification
├── utils/geo.py                 → distance Haversine (matching géographique)
└── routers/
    ├── auth.py                    → inscription/connexion par OTP
    ├── users.py                    → profil utilisateur
    ├── couriers.py                  → dossier, disponibilité, position
    ├── orders.py                     → cœur métier : cycle de vie des commandes
    └── ws.py                          → WebSocket temps réel
```

## Modèle de tarification (à ajuster)

Défini dans `app/config.py` :
- Prise en charge : 500 FCFA
- Prix au km : 250 FCFA
- Suppléments par type de colis (colis lourd, repas, courses...)

## Endpoints principaux

| Méthode | Route | Description |
|---|---|---|
| POST | `/auth/send-otp` | Envoyer un code OTP (simulé) |
| POST | `/auth/verify-otp` | Vérifier le code et obtenir un token |
| GET | `/users/me` | Profil de l'utilisateur connecté |
| POST | `/couriers/profile` | Soumettre le dossier livreur |
| PATCH | `/couriers/me/availability` | Basculer disponible/indisponible |
| PATCH | `/couriers/me/location` | Mettre à jour la position GPS |
| POST | `/orders/estimate` | Estimer le prix d'une course |
| POST | `/orders` | Créer une commande (client) |
| GET | `/orders/nearby` | Commandes proches (livreur) |
| POST | `/orders/{id}/accept` | Accepter une commande |
| PATCH | `/orders/{id}/status` | Faire progresser le statut |
| POST | `/orders/{id}/confirm-delivery` | Confirmer avec le code |
| POST | `/orders/{id}/pay` | Payer la commande |
| POST | `/orders/{id}/rate` | Noter le livreur |
| GET | `/orders/history` | Historique des commandes |
| WS | `/ws/orders/{id}` | Suivi temps réel |
