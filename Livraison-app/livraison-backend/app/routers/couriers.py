from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import User, CourierProfile, CourierStatus
from app.schemas import (
    CourierProfileUpdate, CourierProfileOut, AvailabilityUpdate, LocationUpdate,
)
from app.security import get_current_user, require_role

router = APIRouter(prefix="/couriers", tags=["couriers"])


def _get_or_create_profile(db: Session, user: User) -> CourierProfile:
    profile = db.query(CourierProfile).filter(CourierProfile.user_id == user.id).first()
    if profile is None:
        profile = CourierProfile(user_id=user.id)
        db.add(profile)
        db.commit()
        db.refresh(profile)
    return profile


@router.post("/profile", response_model=CourierProfileOut)
def submit_profile(
    payload: CourierProfileUpdate,
    db: Session = Depends(get_db),
    user: User = Depends(require_role("courier")),
):
    """
    Soumission du dossier d'inscription (pièce d'identité + véhicule).
    En bêta, le statut passe automatiquement à 'validated' — en
    production, un administrateur doit valider manuellement via le
    back-office avant de faire passer le statut à 'validated'.
    """
    profile = _get_or_create_profile(db, user)
    profile.vehicle_type = payload.vehicle_type
    profile.plate_number = payload.plate_number
    profile.id_document_uploaded = payload.id_document_uploaded
    profile.vehicle_photo_uploaded = payload.vehicle_photo_uploaded

    if payload.id_document_uploaded and payload.vehicle_photo_uploaded:
        profile.status = CourierStatus.pending
        # TODO (V1) : ne pas auto-valider — attendre une action admin.
        profile.status = CourierStatus.validated
    db.commit()
    db.refresh(profile)
    return profile


@router.get("/me", response_model=CourierProfileOut)
def get_my_profile(
    db: Session = Depends(get_db),
    user: User = Depends(require_role("courier")),
):
    profile = _get_or_create_profile(db, user)
    return profile


@router.patch("/me/availability", response_model=CourierProfileOut)
def update_availability(
    payload: AvailabilityUpdate,
    db: Session = Depends(get_db),
    user: User = Depends(require_role("courier")),
):
    profile = _get_or_create_profile(db, user)
    if profile.status != CourierStatus.validated:
        raise HTTPException(
            status_code=403,
            detail="Ton dossier doit être validé avant de pouvoir passer disponible.",
        )
    profile.is_available = payload.is_available
    db.commit()
    db.refresh(profile)
    return profile


@router.patch("/me/location", response_model=CourierProfileOut)
def update_location(
    payload: LocationUpdate,
    db: Session = Depends(get_db),
    user: User = Depends(require_role("courier")),
):
    profile = _get_or_create_profile(db, user)
    profile.current_lat = payload.lat
    profile.current_lng = payload.lng
    profile.location_updated_at = datetime.utcnow()
    db.commit()
    db.refresh(profile)
    return profile
