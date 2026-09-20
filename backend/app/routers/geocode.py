from typing import List

import httpx
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel

router = APIRouter(prefix="/geocode", tags=["geocode"])

NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"
USER_AGENT = "LivraisonAppTchad/0.1"


class GeocodeResult(BaseModel):
    display_name: str
    lat: float
    lng: float


# Liste locale de quartiers de N'Djamena — utilisée en priorité car le
# service externe (Nominatim/OSM) bloque parfois les requêtes venant de
# serveurs cloud comme Render. Complète cette liste au besoin.
NDJAMENA_NEIGHBORHOODS = {
    "diguel": (12.0980, 15.0270),
    "abena": (12.1550, 15.0700),
    "atrone": (12.1200, 15.0800),
    "gassi": (12.1450, 15.0450),
    "walia": (12.1600, 15.0600),
    "moursal": (12.1150, 15.0650),
    "chagoua": (12.0900, 15.0600),
    "farcha": (12.1750, 15.0350),
    "klemat": (12.1300, 15.0500),
    "sabangali": (12.1250, 15.0550),
    "ridina": (12.1050, 15.0750),
    "paris congo": (12.1400, 15.0350),
    "dembe": (12.1000, 15.0500),
    "goudji": (12.1550, 15.0900),
    "kabalaye": (12.1200, 15.0400),
    "amriguebe": (12.1650, 15.0500),
    "n'djari": (12.0850, 15.0900),
    "toukra": (12.0700, 15.1100),
    "sara": (12.1350, 15.0800),
    "lamadji": (12.1450, 15.0250),
}


def _search_local(q: str) -> List[GeocodeResult]:
    q_lower = q.strip().lower()
    matches = []
    for name, (lat, lng) in NDJAMENA_NEIGHBORHOODS.items():
        if q_lower in name or name in q_lower:
            matches.append(
                GeocodeResult(
                    display_name=f"{name.title()}, N'Djamena, Tchad",
                    lat=lat,
                    lng=lng,
                )
            )
    return matches


@router.get("/search", response_model=List[GeocodeResult])
async def search_address(q: str = Query(..., min_length=2)):
    """
    Recherche d'adresses : essaie d'abord la liste locale de quartiers
    de N'Djamena (fiable, instantanée), puis se rabat sur Nominatim/OSM
    si aucun résultat local n'est trouvé.
    """
    local_results = _search_local(q)
    if local_results:
        return local_results

    params = {
        "q": q,
        "format": "json",
        "limit": 5,
        "countrycodes": "td",
        "addressdetails": 0,
    }
    headers = {"User-Agent": USER_AGENT}

    try:
        async with httpx.AsyncClient(timeout=8.0) as client:
            response = await client.get(NOMINATIM_URL, params=params, headers=headers)
            response.raise_for_status()
            data = response.json()
    except httpx.HTTPError:
        # Le service externe échoue et aucun résultat local trouvé :
        # on renvoie une liste vide plutôt qu'une erreur 502 bloquante.
        return []

    return [
        GeocodeResult(
            display_name=item["display_name"],
            lat=float(item["lat"]),
            lng=float(item["lon"]),
        )
        for item in data
    ]