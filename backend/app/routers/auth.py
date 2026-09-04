import random
from datetime import datetime, timedelta
from typing import Dict, Tuple

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import User, UserRole
from app.schemas import SendOtpRequest, VerifyOtpRequest, TokenResponse
from app.security import create_access_token

router = APIRouter(prefix="/auth", tags=["auth"])

# Stockage en mémoire des codes OTP envoyés (bêta uniquement).
# À remplacer en production par Redis (avec expiration) + un vrai
# fournisseur SMS (API opérateur Airtel/Moov, ou Twilio).
_otp_store: Dict[str, Tuple[str, datetime]] = {}
OTP_TTL_MINUTES = 5
UNIVERSAL_TEST_CODE = "0000"  # pratique pour développer sans vrai SMS


@router.post("/send-otp")
def send_otp(payload: SendOtpRequest):
    code = f"{random.randint(1000, 9999)}"
    _otp_store[payload.phone] = (code, datetime.utcnow() + timedelta(minutes=OTP_TTL_MINUTES))

    # En bêta : le code n'est pas réellement envoyé par SMS, il est
    # simplement retourné dans la réponse pour faciliter les tests.
    return {
        "message": "Code envoyé (simulation bêta — aucun SMS réel).",
        "debug_code": code,
        "hint": f"Le code universel de test est {UNIVERSAL_TEST_CODE}",
    }


@router.post("/verify-otp", response_model=TokenResponse)
def verify_otp(payload: VerifyOtpRequest, db: Session = Depends(get_db)):
    stored = _otp_store.get(payload.phone)

    valid = payload.code == UNIVERSAL_TEST_CODE
    if not valid and stored:
        code, expires_at = stored
        if datetime.utcnow() <= expires_at and payload.code == code:
            valid = True

    if not valid:
        raise HTTPException(status_code=400, detail="Code OTP invalide ou expiré")

    # Le rôle admin ne se crée jamais via l'OTP client/livreur : les comptes
    # admin sont créés au bootstrap (voir app/main.py) ou par un autre admin.
    # Sans ce garde-fou, n'importe qui pourrait s'auto-promouvoir admin en
    # passant role="admin" dans le corps de la requête.
    if payload.role == UserRole.admin:
        raise HTTPException(status_code=403, detail="Ce rôle ne peut pas être créé par ce canal")

    user = db.query(User).filter(User.phone == payload.phone).first()
    if user is None:
        user = User(phone=payload.phone, full_name=payload.full_name, role=payload.role)
        db.add(user)
        db.commit()
        db.refresh(user)
    elif payload.full_name and not user.full_name:
        user.full_name = payload.full_name
        db.commit()

    token = create_access_token(user_id=user.id, role=user.role.value)
    return TokenResponse(access_token=token, user_id=user.id, role=user.role)
