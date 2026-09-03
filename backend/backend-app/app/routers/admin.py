from datetime import datetime
from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import (
    User, UserRole, CourierProfile, CourierStatus, Order, OrderStatus,
    ZoneTarifaire, Dispute, DisputeStatus, GpsLog,
)
from app.config import COMMISSION_RATE
from app.schemas import (
    AdminLoginRequest, TokenResponse, ZoneTarifaireIn, ZoneTarifaireOut,
    CourierAdminOut, CourierValidationRequest, OrderOut, DisputeOut,
    DisputeResolveRequest, DashboardStats, GpsLogOut,
)
from app.security import create_access_token, verify_password, require_role

router = APIRouter(prefix="/admin", tags=["admin"])


# --- Auth admin ---

@router.post("/login", response_model=TokenResponse)
def admin_login(payload: AdminLoginRequest, db: Session = Depends(get_db)):
    admin = (
        db.query(User)
        .filter(User.phone == payload.email, User.role == UserRole.admin)
        .first()
    )
    if admin is None or not admin.password_hash or not verify_password(payload.password, admin.password_hash):
        raise HTTPException(status_code=401, detail="Identifiants invalides")

    token = create_access_token(user_id=admin.id, role=admin.role.value)
    return TokenResponse(access_token=token, user_id=admin.id, role=admin.role)


# --- Tableau de bord ---

@router.get("/dashboard", response_model=DashboardStats)
def dashboard(db: Session = Depends(get_db), _admin: User = Depends(require_role("admin"))):
    orders = db.query(Order).all()
    orders_by_status = {}
    for status_value in OrderStatus:
        orders_by_status[status_value.value] = sum(1 for o in orders if o.status == status_value)

    revenue_total = sum(o.price for o in orders if o.status == OrderStatus.paid)
    commission_total = round(revenue_total * COMMISSION_RATE)

    return DashboardStats(
        orders_total=len(orders),
        orders_by_status=orders_by_status,
        revenue_total=revenue_total,
        commission_total=commission_total,
        couriers_validated=db.query(CourierProfile).filter(CourierProfile.status == CourierStatus.validated).count(),
        couriers_pending=db.query(CourierProfile).filter(CourierProfile.status == CourierStatus.pending).count(),
        clients_total=db.query(User).filter(User.role == UserRole.client).count(),
        disputes_open=db.query(Dispute).filter(Dispute.status != DisputeStatus.resolved).count(),
    )


# --- Gestion des livreurs (validation de dossier) ---

@router.get("/couriers", response_model=List[CourierAdminOut])
def list_couriers(
    status: Optional[CourierStatus] = Query(default=None),
    db: Session = Depends(get_db),
    _admin: User = Depends(require_role("admin")),
):
    query = db.query(CourierProfile, User).join(User, CourierProfile.user_id == User.id)
    if status is not None:
        query = query.filter(CourierProfile.status == status)
    rows = query.order_by(CourierProfile.status.asc()).all()
    return [
        CourierAdminOut(
            id=profile.id,
            user_id=user.id,
            full_name=user.full_name,
            phone=user.phone,
            vehicle_type=profile.vehicle_type,
            plate_number=profile.plate_number,
            id_document_uploaded=profile.id_document_uploaded,
            vehicle_photo_uploaded=profile.vehicle_photo_uploaded,
            status=profile.status,
            is_available=profile.is_available,
            rating_avg=profile.rating_avg,
            rating_count=profile.rating_count,
            total_earnings=profile.total_earnings,
        )
        for profile, user in rows
    ]


def _get_courier_profile_or_404(db: Session, user_id: str) -> CourierProfile:
    profile = db.query(CourierProfile).filter(CourierProfile.user_id == user_id).first()
    if profile is None:
        raise HTTPException(status_code=404, detail="Dossier livreur introuvable")
    return profile


@router.patch("/couriers/{user_id}/validate", response_model=CourierAdminOut)
def validate_courier(
    user_id: str,
    payload: CourierValidationRequest,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_role("admin")),
):
    profile = _get_courier_profile_or_404(db, user_id)
    if not (profile.id_document_uploaded and profile.vehicle_photo_uploaded):
        raise HTTPException(status_code=409, detail="Dossier incomplet (pièce d'identité et/ou véhicule manquants)")
    profile.status = CourierStatus.validated
    db.commit()
    db.refresh(profile)
    user = db.query(User).filter(User.id == user_id).first()
    return CourierAdminOut(
        id=profile.id, user_id=user.id, full_name=user.full_name, phone=user.phone,
        vehicle_type=profile.vehicle_type, plate_number=profile.plate_number,
        id_document_uploaded=profile.id_document_uploaded, vehicle_photo_uploaded=profile.vehicle_photo_uploaded,
        status=profile.status, is_available=profile.is_available, rating_avg=profile.rating_avg,
        rating_count=profile.rating_count, total_earnings=profile.total_earnings,
    )


@router.patch("/couriers/{user_id}/reject", response_model=CourierAdminOut)
def reject_courier(
    user_id: str,
    payload: CourierValidationRequest,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_role("admin")),
):
    profile = _get_courier_profile_or_404(db, user_id)
    profile.status = CourierStatus.rejected
    profile.is_available = False
    db.commit()
    db.refresh(profile)
    user = db.query(User).filter(User.id == user_id).first()
    return CourierAdminOut(
        id=profile.id, user_id=user.id, full_name=user.full_name, phone=user.phone,
        vehicle_type=profile.vehicle_type, plate_number=profile.plate_number,
        id_document_uploaded=profile.id_document_uploaded, vehicle_photo_uploaded=profile.vehicle_photo_uploaded,
        status=profile.status, is_available=profile.is_available, rating_avg=profile.rating_avg,
        rating_count=profile.rating_count, total_earnings=profile.total_earnings,
    )


# --- Tarifs / zones (zones_tarifaires du schéma directeur) ---

@router.get("/zones", response_model=List[ZoneTarifaireOut])
def list_zones(db: Session = Depends(get_db), _admin: User = Depends(require_role("admin"))):
    return db.query(ZoneTarifaire).order_by(ZoneTarifaire.created_at.asc()).all()


@router.post("/zones", response_model=ZoneTarifaireOut)
def create_zone(
    payload: ZoneTarifaireIn,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_role("admin")),
):
    if db.query(ZoneTarifaire).filter(ZoneTarifaire.nom_zone == payload.nom_zone).first():
        raise HTTPException(status_code=409, detail="Une zone porte déjà ce nom")

    if payload.is_default:
        db.query(ZoneTarifaire).update({ZoneTarifaire.is_default: False})

    zone = ZoneTarifaire(**payload.model_dump())
    db.add(zone)
    db.commit()
    db.refresh(zone)
    return zone


@router.patch("/zones/{zone_id}", response_model=ZoneTarifaireOut)
def update_zone(
    zone_id: str,
    payload: ZoneTarifaireIn,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_role("admin")),
):
    zone = db.query(ZoneTarifaire).filter(ZoneTarifaire.id == zone_id).first()
    if zone is None:
        raise HTTPException(status_code=404, detail="Zone introuvable")

    if payload.is_default and not zone.is_default:
        db.query(ZoneTarifaire).update({ZoneTarifaire.is_default: False})

    for field, value in payload.model_dump().items():
        setattr(zone, field, value)
    zone.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(zone)
    return zone


@router.delete("/zones/{zone_id}")
def delete_zone(
    zone_id: str,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_role("admin")),
):
    zone = db.query(ZoneTarifaire).filter(ZoneTarifaire.id == zone_id).first()
    if zone is None:
        raise HTTPException(status_code=404, detail="Zone introuvable")
    db.delete(zone)
    db.commit()
    return {"message": "Zone supprimée"}


# --- Commandes (vue support / supervision) ---

@router.get("/orders", response_model=List[OrderOut])
def list_orders(
    status: Optional[OrderStatus] = Query(default=None),
    db: Session = Depends(get_db),
    _admin: User = Depends(require_role("admin")),
):
    query = db.query(Order)
    if status is not None:
        query = query.filter(Order.status == status)
    return query.order_by(Order.created_at.desc()).limit(500).all()


# --- Litiges / support client ---

@router.get("/disputes", response_model=List[DisputeOut])
def list_disputes(
    status: Optional[DisputeStatus] = Query(default=None),
    db: Session = Depends(get_db),
    _admin: User = Depends(require_role("admin")),
):
    query = db.query(Dispute)
    if status is not None:
        query = query.filter(Dispute.status == status)
    return query.order_by(Dispute.created_at.desc()).all()


@router.patch("/disputes/{dispute_id}/resolve", response_model=DisputeOut)
def resolve_dispute(
    dispute_id: str,
    payload: DisputeResolveRequest,
    db: Session = Depends(get_db),
    admin: User = Depends(require_role("admin")),
):
    dispute = db.query(Dispute).filter(Dispute.id == dispute_id).first()
    if dispute is None:
        raise HTTPException(status_code=404, detail="Litige introuvable")
    dispute.status = DisputeStatus.resolved
    dispute.resolution_note = payload.resolution_note
    dispute.resolved_by_id = admin.id
    dispute.resolved_at = datetime.utcnow()
    db.commit()
    db.refresh(dispute)
    return dispute


# --- Logs GPS (investigation litiges / sécurité — section 7 du schéma) ---

@router.get("/gps-logs", response_model=List[GpsLogOut])
def list_gps_logs(
    courier_id: Optional[str] = Query(default=None),
    order_id: Optional[str] = Query(default=None),
    db: Session = Depends(get_db),
    _admin: User = Depends(require_role("admin")),
):
    if not courier_id and not order_id:
        raise HTTPException(status_code=400, detail="Précise courier_id ou order_id pour limiter la recherche")
    query = db.query(GpsLog)
    if courier_id:
        query = query.filter(GpsLog.courier_id == courier_id)
    if order_id:
        query = query.filter(GpsLog.order_id == order_id)
    return query.order_by(GpsLog.timestamp.asc()).limit(2000).all()
