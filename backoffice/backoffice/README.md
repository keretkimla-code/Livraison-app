# Back-office Admin — Livraison (web)

Interface web statique (HTML/CSS/JS pur, aucun build) pour l'administration
du service, conforme à la section 3.3 du schéma directeur : tableau de
bord, validation des livreurs, gestion des tarifs par zone, supervision
des commandes, et gestion des litiges/support client.

Ce choix technique (HTML/CSS/JS simple plutôt que React) suit la
recommandation du schéma directeur : « Cohérent avec tes bases HTML/CSS ».
Rien à installer, rien à compiler.

## Lancer en local

1. Démarre d'abord le backend (voir `../backend/README.md`) :
   ```bash
   cd ../backend
   venv/bin/uvicorn app.main:app --reload
   ```
   Au premier démarrage, un compte admin par défaut est créé :
   `admin@livraison.td` / `admin1234` (variables `ADMIN_EMAIL` /
   `ADMIN_PASSWORD` dans `.env` pour le changer).

2. Ouvre `index.html` dans un navigateur (double-clic, ou sers le dossier
   avec un petit serveur statique pour éviter d'éventuelles restrictions
   navigateur sur `file://`) :
   ```bash
   cd backoffice
   python3 -m http.server 8080
   # puis ouvrir http://127.0.0.1:8080
   ```

3. Sur l'écran de connexion, vérifie l'« URL de l'API » (par défaut
   `http://127.0.0.1:8000`, à changer si le backend tourne ailleurs — ex.
   sur un VPS en production), puis connecte-toi avec le compte admin.

## Pages

- **Tableau de bord** : commandes par statut, revenu total (commandes
  payées), commission plateforme estimée, nombre de livreurs
  validés/en attente, clients inscrits, litiges ouverts.
- **Livreurs** : liste des dossiers livreurs avec filtre par statut,
  boutons Valider/Rejeter — c'est ici que se fait la validation manuelle
  qui remplace l'auto-validation de la version bêta (voir
  `AUTO_VALIDATE_COURIERS` dans le backend).
- **Tarifs & zones** : CRUD sur la table `zones_tarifaires` du schéma
  directeur (prise en charge, tarif/km, multiplicateur heures de pointe).
  La zone marquée « par défaut » fixe le tarif utilisé pour toutes les
  estimations/commandes.
- **Commandes** : vue de supervision avec filtre par statut, utile pour le
  support client.
- **Litiges** : liste des signalements ouverts par les clients/livreurs
  (`POST /orders/{id}/dispute` côté apps mobiles), avec possibilité de les
  marquer résolus et d'ajouter une note interne.

## Limites connues (transparence sur ce qui reste à faire)

- **Pas de géofencing par zone réelle.** Le découpage géographique par
  polygone (ex. « zone N'Djamena centre » vs « zone périphérie ») demande
  PostGIS, qui n'est pas encore branché (le backend utilise SQLite +
  Haversine en Python). Pour l'instant, une seule zone « par défaut »
  s'applique à toutes les commandes.
- **Pas de rôles admin différenciés** (ex. super-admin vs support). Un
  seul niveau d'accès `admin` existe pour l'instant.
- **Pas d'écran de changement de mot de passe** pour le compte admin —
  à faire avant tout déploiement partagé (changer `ADMIN_PASSWORD` dans
  `.env` en attendant).
- **Pas de rapports financiers détaillés** (export CSV, période
  personnalisée) — le tableau de bord donne des totaux globaux seulement.
- Interface non traduite au-delà du français, non testée sur mobile
  (back-office = usage desktop en priorité, conforme au schéma).
