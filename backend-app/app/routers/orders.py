import random
from typing import List

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.config import DEFAULT_SEARCH_RADIUS_KM, BASE_FARE, FARE_PER_KM, PARCEL_SURCHARGE
from app.database import get_db
from app.models import (
    User, Order, OrderStatus, CourierProfile, CourierStatus, Rating,
    ZoneTarifaire, Dispute,
)
from app.schemas import (
    OrderEstimateRequest, OrderEstimateResponse, OrderCreateRequest, OrderOut,
    OrderStatusUpdate, ConfirmDeliveryRequest, PayOrderRequest, RateOrderRequest,
    NearbyCourierOut, DisputeCreateRequest, DisputeOut,
)
from app.security import get_current_user, require_role
from app.utils.geo import haversine_km
from app.routers.ws import manager

router = APIRouter(prefix="/orders", tags=["orders"])


def _compute_price(db: Session, distance_km: float, parcel_type: str) -> int:
    """
    Calcule le prix d'une course. Si une zone tarifaire active est marquée
    comme zone par défaut dans le back-office admin (table zones_tarifaires,
    section 4/5 du schéma directeur), ses tarifs remplacent les valeurs
    codées en dur dans config.py. Sinon, on retombe sur BASE_FARE/FARE_PER_KM.

    Limite connue : pas de découpage géographique réel par polygone (ça
    demanderait PostGIS) — voir la note dans le modèle ZoneTarifaire.
    """
    base_fare, fare_per_km, multiplicateur = BASE_FARE, FARE_PER_KM, 1.0
    default_zone = (
        db.query(ZoneTarifaire)
        .filter(ZoneTarifaire.is_default.is_(True), ZoneTarifaire.actif.is_(True))
        .first()
    )
    if default_zone is not None:
        base_fare = default_zone.tarif_base
        fare_per_km = default_zone.tarif_km
        multiplicateur = default_zone.heure_pointe_multiplicateur or 1.0

    surcharge = PARCEL_SURCHARGE.get(parcel_type, 0)
    return round((base_fare + distance_km * fare_per_km) * multiplicateur + surcharge)


def _order_or_404(db: Session, order_id: str) -> Order:
    order = db.query(Order).filter(Order.id == order_id).first()
    if order is None:
        raise HTTPException(status_code=404, detail="Commande introuvable")
    return order


def _ensure_participant(order: Order, user: User) -> None:
    if user.id not in (order.client_id, order.courier_id):
        raise HTTPException(status_code=403, detail="Tu n'es pas rattaché à cette commande")


# --- Estimation (sans écriture en base) ---

@router.post("/estimate", response_model=OrderEstimateResponse)
def estimate_order(payload: OrderEstimateRequest, db: Session = Depends(get_db)):
    distance = haversine_km(
        payload.pickup_lat, payload.pickup_lng, payload.dropoff_lat, payload.dropoff_lng
    )
    price = _compute_price(db, distance, payload.parcel_type.value)
    return OrderEstimateResponse(distance_km=round(distance, 2), price=price)


# --- Création (client) ---

@router.post("", response_model=OrderOut)
def create_order(
    payload: OrderCreateRequest,
    db: Session = Depends(get_db),
    user: User = Depends(require_role("client")),
):
    distance = haversine_km(
        payload.pickup_lat, payload.pickup_lng, payload.dropoff_lat, payload.dropoff_lng
    )
    price = _compute_price(db, distance, payload.parcel_type.value)

    order = Order(
        client_id=user.id,
        pickup_address=payload.pickup_address,
        pickup_lat=payload.pickup_lat,
        pickup_lng=payload.pickup_lng,
        dropoff_address=payload.dropoff_address,
        dropoff_lat=payload.dropoff_lat,
        dropoff_lng=payload.dropoff_lng,
        parcel_type=payload.parcel_type,
        distance_km=round(distance, 2),
        price=price,
        status=OrderStatus.pending,
    )
    db.add(order)
    db.commit()
    db.refresh(order)
    return order


# --- Découverte (livreur) ---

@router.get("/nearby", response_model=List[OrderOut])
def list_nearby_orders(
    radius_km: float = DEFAULT_SEARCH_RADIUS_KM,
    db: Session = Depends(get_db),
    user: User = Depends(require_role("courier")),
):
    profile = db.query(CourierProfile).filter(CourierProfile.user_id == user.id).first()
    if profile is None or not profile.is_available:
        raise HTTPException(status_code=400, detail="Passe disponible pour voir les demandes proches")
    if profile.current_lat is None or profile.current_lng is None:
        raise HTTPException(status_code=400, detail="Position GPS non renseignée")

    pending_orders = db.query(Order).filter(Order.status == OrderStatus.pending).all()
    nearby = [
        o for o in pending_orders
        if haversine_km(profile.current_lat, profile.current_lng, o.pickup_lat, o.pickup_lng) <= radius_km
    ]
    nearby.sort(
        key=lambda o: haversine_km(profile.current_lat, profile.current_lng, o.pickup_lat, o.pickup_lng)
    )
    return nearby


@router.get("/{order_id}/candidates", response_model=List[NearbyCourierOut])
def list_candidate_couriers(
    order_id: str,
    radius_km: float = DEFAULT_SEARCH_RADIUS_KM,
    db: Session = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """Livreurs disponibles à proximité du point de collecte (usage interne/admin)."""
    order = _order_or_404(db, order_id)
    couriers = (
        db.query(CourierProfile)
        .filter(
            CourierProfile.status == CourierStatus.validated,
            CourierProfile.is_available.is_(True),
            CourierProfile.current_lat.isnot(None),
        )
        .all()
    )
    results = []
    for c in couriers:
        d = haversine_km(order.pickup_lat, order.pickup_lng, c.current_lat, c.current_lng)
        if d <= radius_km:
            results.append(
                NearbyCourierOut(
                    courier_id=c.user_id, distance_km=round(d, 2),
                    rating_avg=c.rating_avg, vehicle_type=c.vehicle_type,
                )
            )
    results.sort(key=lambda r: r.distance_km)
    return results


# --- Cycle de vie d'une commande ---

@router.get("/history", response_model=List[OrderOut])
def order_history(
    db: Session = Depends(get_db),
    user: User = Depends(get_current_user),
):
    if user.role.value == "client":
        return db.query(Order).filter(Order.client_id == user.id).order_by(Order.created_at.desc()).all()
    return db.query(Order).filter(Order.courier_id == user.id).order_by(Order.created_at.desc()).all()


@router.get("/{order_id}", response_model=OrderOut)
def get_order(order_id: str, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    order = _order_or_404(db, order_id)
    _ensure_participant(order, user)
    return order


@router.post("/{order_id}/accept", response_model=OrderOut)
async def accept_order(
    order_id: str,
    db: Session = Depends(get_db),
    user: User = Depends(require_role("courier")),
):
    order = _order_or_404(db, order_id)
    if order.status != OrderStatus.pending:
        raise HTTPException(status_code=409, detail="Cette commande n'est plus disponible")

    order.courier_id = user.id
    order.status = OrderStatus.heading_to_pickup
    order.delivery_code = f"{random.randint(1000, 9999)}"
    db.commit()
    db.refresh(order)

    await manager.broadcast(order_id, {"event": "status_update", "status": order.status.value})
    return order


@router.post("/{order_id}/decline")
def decline_order(
    order_id: str,
    db: Session = Depends(get_db),
    user: User = Depends(require_role("courier")),
):
    # Cette route existe pour la journalisation côté livreur (analytics futurs) —
    # la commande reste 'pending' et visible pour les autres livreurs.
    _order_or_404(db, order_id)
    return {"message": "Commande refusée, elle reste proposée aux autres livreurs."}


@router.patch("/{order_id}/status", response_model=OrderOut)
async def update_status(
    order_id: str,
    payload: OrderStatusUpdate,
    db: Session = Depends(get_db),
    user: User = Depends(require_role("courier")),
):
    order = _order_or_404(db, order_id)
    if order.courier_id != user.id:
        raise HTTPException(status_code=403, detail="Tu n'es pas le livreur assigné à cette commande")

    allowed_transitions = {
        OrderStatus.heading_to_pickup: {OrderStatus.at_pickup},
        OrderStatus.at_pickup: {OrderStatus.heading_to_dropoff},
        OrderStatus.heading_to_dropoff: {OrderStatus.at_dropoff},
    }
    if payload.status not in allowed_transitions.get(order.status, set()):
        raise HTTPException(
            status_code=409,
            detail=f"Transition {order.status.value} -> {payload.status.value} non autorisée",
        )

    order.status = payload.status
    db.commit()
    db.refresh(order)

    await manager.broadcast(order_id, {"event": "status_update", "status": order.status.value})
    return order


@router.post("/{order_id}/confirm-delivery", response_model=OrderOut)
async def confirm_delivery(
    order_id: str,
    payload: ConfirmDeliveryRequest,
    db: Session = Depends(get_db),
    user: User = Depends(require_role("courier")),
):
    order = _order_or_404(db, order_id)
    if order.courier_id != user.id:
        raise HTTPException(status_code=403, detail="Tu n'es pas le livreur assigné à cette commande")
    if order.status != OrderStatus.at_dropoff:
        raise HTTPException(status_code=409, detail="Le livreur doit être arrivé au point de livraison")
    if payload.code != order.delivery_code:
        raise HTTPException(status_code=400, detail="Code de confirmation incorrect")

    order.status = OrderStatus.delivered
    db.commit()
    db.refresh(order)

    await manager.broadcast(order_id, {"event": "status_update", "status": order.status.value})
    return order


@router.post("/{order_id}/pay", response_model=OrderOut)
async def pay_order(
    order_id: str,
    payload: PayOrderRequest,
    db: Session = Depends(get_db),
    user: User = Depends(require_role("client")),
):
    order = _order_or_404(db, order_id)
    if order.client_id != user.id:
        raise HTTPException(status_code=403, detail="Cette commande ne t'appartient pas")
    if order.status != OrderStatus.delivered:
        raise HTTPException(status_code=409, detail="La commande doit être livrée avant paiement")

    # TODO (V1) : appeler ici l'API Airtel Money / Moov Money pour un
    # paiement mobile réel, et valider via webhook avant de marquer 'paid'.
    order.payment_method = payload.method
    order.status = OrderStatus.paid
    db.commit()
    db.refresh(order)

    if order.courier_id:
        courier = db.query(CourierProfile).filter(CourierProfile.user_id == order.courier_id).first()
        if courier:
            courier.total_earnings += order.price
            db.commit()

    await manager.broadcast(order_id, {"event": "status_update", "status": order.status.value})
    return order


@router.post("/{order_id}/rate")
def rate_order(
    order_id: str,
    payload: RateOrderRequest,
    db: Session = Depends(get_db),
    user: User = Depends(require_role("client")),
):
    order = _order_or_404(db, order_id)
    if order.client_id != user.id:
        raise HTTPException(status_code=403, detail="Cette commande ne t'appartient pas")
    if db.query(Rating).filter(Rating.order_id == order_id).first():
        raise HTTPException(status_code=409, detail="Cette commande a déjà été notée")

    rating = Rating(order_id=order_id, rating=payload.rating, comment=payload.comment)
    db.add(rating)

    if order.courier_id:
        courier = db.query(CourierProfile).filter(CourierProfile.user_id == order.courier_id).first()
        if courier:
            total_points = courier.rating_avg * courier.rating_count + payload.rating
            courier.rating_count += 1
            courier.rating_avg = round(total_points / courier.rating_count, 2)

    db.commit()
    return {"message": "Merci pour ta note !"}


# --- Litiges (support client — section 3.3 du schéma directeur) ---

@router.post("/{order_id}/dispute", response_model=DisputeOut)
def open_dispute(
    order_id: str,
    payload: DisputeCreateRequest,
    db: Session = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """Le client ou le livreur signale un problème sur une commande ;
    le litige apparaît ensuite dans le back-office admin pour traitement."""
    order = _order_or_404(db, order_id)
    _ensure_participant(order, user)

    dispute = Dispute(
        order_id=order_id,
        reported_by_id=user.id,
        reason=payload.reason,
        description=payload.description,
    )
    db.add(dispute)
    db.commit()
    db.refresh(dispute)
    return dispute


@router.get("/{order_id}/disputes", response_model=List[DisputeOut])
def list_order_disputes(
    order_id: str,
    db: Session = Depends(get_db),
    user: User = Depends(get_current_user),
):
    order = _order_or_404(db, order_id)
    _ensure_participant(order, user)
    return db.query(Dispute).filter(Dispute.order_id == order_id).order_by(Dispute.created_at.desc()).all()
