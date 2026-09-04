from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import ADMIN_EMAIL, ADMIN_PASSWORD
from app.database import Base, SessionLocal, engine
from app.models import User, UserRole
from app.routers import auth, users, couriers, orders, ws, chat, geocode, admin
from app.security import hash_password

# Crée les tables si elles n'existent pas encore.
# En production, préfère un outil de migration (ex. Alembic) à ce
# create_all() automatique, pour gérer proprement les évolutions de schéma.
Base.metadata.create_all(bind=engine)


def bootstrap_admin() -> None:
    """
    Crée un compte admin par défaut (ADMIN_EMAIL/ADMIN_PASSWORD dans .env)
    au premier démarrage, si aucun compte admin n'existe déjà. Permet
    d'accéder au back-office (backoffice/) sans étape d'installation
    manuelle. Change le mot de passe par défaut avant tout déploiement
    partagé — il n'y a pas encore d'écran de changement de mot de passe
    (TODO V1).
    """
    db = SessionLocal()
    try:
        existing_admin = db.query(User).filter(User.role == UserRole.admin).first()
        if existing_admin is None:
            db.add(User(
                phone=ADMIN_EMAIL,
                full_name="Administrateur",
                role=UserRole.admin,
                password_hash=hash_password(ADMIN_PASSWORD),
            ))
            db.commit()
    finally:
        db.close()


bootstrap_admin()

app = FastAPI(
    title="Livraison API",
    description=(
        "Backend central du projet de livraison type Yango/Uber (Tchad). "
        "Sert les applications Client (Kotlin) et Livreur (Flutter)."
    ),
    version="0.1.0-beta",
)

# CORS ouvert pour simplifier les tests depuis les apps mobiles et un futur
# back-office web. À restreindre à des origines précises en production.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(users.router)
app.include_router(couriers.router)
app.include_router(orders.router)
app.include_router(chat.router)
app.include_router(geocode.router)
app.include_router(ws.router)
app.include_router(admin.router)


@app.get("/")
def root():
    return {
        "service": "Livraison API",
        "status": "ok",
        "docs": "/docs",
    }


@app.get("/health")
def health():
    return {"status": "ok"}
