import os
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./livraison.db")
SECRET_KEY = os.getenv("SECRET_KEY", "dev-secret-key-a-changer-en-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "1440"))

# Rayon de recherche par défaut pour le matching livreur <-> commande (km)
DEFAULT_SEARCH_RADIUS_KM = 5.0

# Tarification par défaut (utilisée tant qu'aucune zone tarifaire active
# n'est configurée par l'admin dans le back-office — voir zones_tarifaires
# et app/routers/admin.py). Une fois une zone marquée `is_default`, ses
# valeurs remplacent BASE_FARE/FARE_PER_KM pour le calcul du prix.
BASE_FARE = 500
FARE_PER_KM = 250
PARCEL_SURCHARGE = {
    "document": 0,
    "colis_leger": 0,
    "colis_lourd": 1000,
    "repas": 300,
    "courses": 200,
}

# Commission plateforme prélevée sur chaque course (section 9 du schéma
# directeur : 15-20%). Utilisée pour les rapports financiers du back-office.
COMMISSION_RATE = float(os.getenv("COMMISSION_RATE", "0.18"))

# En bêta, un dossier livreur complet (pièce d'identité + véhicule) était
# auto-validé pour simplifier les tests sans back-office. Maintenant que
# l'admin peut valider manuellement (section 3.3 du schéma), on désactive
# l'auto-validation par défaut. Remets à "true" uniquement pour des tests
# locaux rapides sans avoir à te connecter au back-office.
AUTO_VALIDATE_COURIERS = os.getenv("AUTO_VALIDATE_COURIERS", "false").lower() == "true"

# Compte admin créé automatiquement au démarrage si aucun compte admin
# n'existe encore en base. À changer immédiatement après la première
# connexion (aucune UI de changement de mot de passe pour l'instant —
# TODO V1). Voir app/main.py:bootstrap_admin().
ADMIN_EMAIL = os.getenv("ADMIN_EMAIL", "admin@livraison.td")
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD", "admin1234")
