from math import radians, sin, cos, sqrt, atan2


def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """
    Distance à vol d'oiseau entre deux points GPS, en kilomètres.

    En développement (SQLite), on calcule la distance en Python avec la
    formule de Haversine. En production avec PostgreSQL + PostGIS, cette
    fonction peut être remplacée par une requête spatiale native
    (ST_DistanceSphere) beaucoup plus rapide pour de gros volumes.
    """
    R = 6371.0  # rayon moyen de la Terre en km
    phi1, phi2 = radians(lat1), radians(lat2)
    dphi = radians(lat2 - lat1)
    dlambda = radians(lon2 - lon1)

    a = sin(dphi / 2) ** 2 + cos(phi1) * cos(phi2) * sin(dlambda / 2) ** 2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
