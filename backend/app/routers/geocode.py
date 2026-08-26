from typing import List

import httpx
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel

router = APIRouter(prefix="/geocode", tags=["geocode"])

NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"

# Nominatim exige un User-Agent identifiant l'application (politique
# d'usage OSM) — personnalise avec un vrai contact avant la mise en
# production, et évite les appels trop fréquents (max ~1 req/s en usage
# gratuit ; passe à un fournisseur payant — Mapbox, LocationIQ, Google —
# si le volume grandit).
USER_AGENT = "LivraisonAppTchad/0.1 (beta - contact: barka@example.com)"


class GeocodeResult(BaseModel):
    display_name: str
    lat: float
    lng: float


@router.get("/search", response_model=List[GeocodeResult])
async def search_address(q: str = Query(..., min_length=2, description="Texte de l'adresse recherchée")):
    """
    Recherche d'adresses en texte libre, restreinte au Tchad, via
    Nominatim (OpenStreetMap). Retourne jusqu'à 5 suggestions avec leurs
    coordonnées GPS — à utiliser côté app pour l'autocomplétion des
    champs d'adresse.
    """
    params = {
        "q": q,
        "format": "json",
        "limit": 5,
        "countrycodes": "td",
        "addressdetails": 0,
    }
    headers = {"User-Agent": USER_AGENT}

    async with httpx.AsyncClient(timeout=8.0) as client:
        try:
            response = await client.get(NOMINATIM_URL, params=params, headers=headers)
            response.raise_for_status()
        except httpx.HTTPError:
            raise HTTPException(
                status_code=502,
                detail="Service de géocodage momentanément indisponible, réessaie.",
            )

    data = response.json()
    return [
        GeocodeResult(
            display_name=item["display_name"],
            lat=float(item["lat"]),
            lng=float(item["lon"]),
        )
        for item in data
    ]
