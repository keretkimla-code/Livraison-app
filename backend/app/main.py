from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.database import Base, engine
from app.routers import auth, users, couriers, orders, ws, chat, geocode

# Crée les tables si elles n'existent pas encore.
# En production, préfère un outil de migration (ex. Alembic) à ce
# create_all() automatique, pour gérer proprement les évolutions de schéma.
Base.metadata.create_all(bind=engine)

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
