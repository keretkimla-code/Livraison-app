import os
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./livraison.db")
SECRET_KEY = os.getenv("SECRET_KEY", "dev-secret-key-a-changer-en-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "1440"))

# Rayon de recherche par défaut pour le matching livreur <-> commande (km)
DEFAULT_SEARCH_RADIUS_KM = 5.0

# Tarification (simulation — à ajuster selon ta grille réelle)
BASE_FARE = 500
FARE_PER_KM = 250
PARCEL_SURCHARGE = {
    "document": 0,
    "colis_leger": 0,
    "colis_lourd": 1000,
    "repas": 300,
    "courses": 200,
}
