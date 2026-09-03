from datetime import datetime
from typing import Optional, List

from pydantic import BaseModel, Field

from app.models import UserRole, CourierStatus, VehicleType, ParcelType, OrderStatus, DisputeStatus


# --- Auth ---

class SendOtpRequest(BaseModel):
    phone: str = Field(..., min_length=8)


class VerifyOtpRequest(BaseModel):
    phone: str
    code: str
    role: UserRole
    full_name: Optional[str] = None


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    role: UserRole


# --- Users ---

class UserOut(BaseModel):
    id: str
    phone: str
    full_name: Optional[str]
    role: UserRole
    created_at: datetime

    class Config:
        from_attributes = True


# --- Couriers ---

class CourierProfileUpdate(BaseModel):
    vehicle_type: VehicleType
    plate_number: str
    id_document_uploaded: bool = False
    vehicle_photo_uploaded: bool = False


class CourierProfileOut(BaseModel):
    id: str
    vehicle_type: VehicleType
    plate_number: Optional[str]
    id_document_uploaded: bool
    vehicle_photo_uploaded: bool
    status: CourierStatus
    is_available: bool
    current_lat: Optional[float]
    current_lng: Optional[float]
    rating_avg: float
    rating_count: int
    total_earnings: int

    class Config:
        from_attributes = True


class AvailabilityUpdate(BaseModel):
    is_available: bool


class LocationUpdate(BaseModel):
    lat: float
    lng: float


# --- Orders ---

class OrderEstimateRequest(BaseModel):
    pickup_lat: float
    pickup_lng: float
    dropoff_lat: float
    dropoff_lng: float
    parcel_type: ParcelType = ParcelType.colis_leger


class OrderEstimateResponse(BaseModel):
    distance_km: float
    price: int


class OrderCreateRequest(BaseModel):
    pickup_address: str
    pickup_lat: float
    pickup_lng: float
    dropoff_address: str
    dropoff_lat: float
    dropoff_lng: float
    parcel_type: ParcelType = ParcelType.colis_leger


class OrderStatusUpdate(BaseModel):
    status: OrderStatus


class ConfirmDeliveryRequest(BaseModel):
    code: str


class PayOrderRequest(BaseModel):
    method: str


class RateOrderRequest(BaseModel):
    rating: int = Field(..., ge=1, le=5)
    comment: Optional[str] = None


class OrderOut(BaseModel):
    id: str
    client_id: str
    courier_id: Optional[str]
    pickup_address: str
    pickup_lat: float
    pickup_lng: float
    dropoff_address: str
    dropoff_lat: float
    dropoff_lng: float
    parcel_type: ParcelType
    distance_km: float
    price: int
    status: OrderStatus
    payment_method: Optional[str]
    # Exposé aux deux participants pour faciliter les tests bêta en solo.
    # En production, le code devrait rester visible côté client uniquement
    # (c'est le client qui le communique de vive voix au livreur à la
    # livraison) — à restreindre lors du passage en V1.
    delivery_code: Optional[str]
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class NearbyCourierOut(BaseModel):
    courier_id: str
    distance_km: float
    rating_avg: float
    vehicle_type: VehicleType


class ChatMessageIn(BaseModel):
    text: str


class ChatMessageOut(BaseModel):
    id: str
    order_id: str
    sender_role: UserRole
    text: str
    created_at: datetime

    class Config:
        from_attributes = True


# --- Admin / Back-office ---

class AdminLoginRequest(BaseModel):
    email: str
    password: str


class ZoneTarifaireIn(BaseModel):
    nom_zone: str
    tarif_base: int = Field(..., ge=0)
    tarif_km: int = Field(..., ge=0)
    heure_pointe_multiplicateur: float = Field(1.0, ge=1.0)
    is_default: bool = False
    actif: bool = True


class ZoneTarifaireOut(BaseModel):
    id: str
    nom_zone: str
    tarif_base: int
    tarif_km: int
    heure_pointe_multiplicateur: float
    is_default: bool
    actif: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class CourierAdminOut(BaseModel):
    id: str
    user_id: str
    full_name: Optional[str]
    phone: str
    vehicle_type: VehicleType
    plate_number: Optional[str]
    id_document_uploaded: bool
    vehicle_photo_uploaded: bool
    status: CourierStatus
    is_available: bool
    rating_avg: float
    rating_count: int
    total_earnings: int


class CourierValidationRequest(BaseModel):
    note: Optional[str] = None


class DisputeCreateRequest(BaseModel):
    reason: str
    description: Optional[str] = None


class DisputeResolveRequest(BaseModel):
    resolution_note: str


class DisputeOut(BaseModel):
    id: str
    order_id: str
    reported_by_id: str
    reason: str
    description: Optional[str]
    status: DisputeStatus
    resolution_note: Optional[str]
    created_at: datetime
    resolved_at: Optional[datetime]

    class Config:
        from_attributes = True


class GpsLogOut(BaseModel):
    id: str
    courier_id: str
    order_id: Optional[str]
    lat: float
    lng: float
    timestamp: datetime

    class Config:
        from_attributes = True


class DashboardStats(BaseModel):
    orders_total: int
    orders_by_status: dict
    revenue_total: int
    commission_total: int
    couriers_validated: int
    couriers_pending: int
    clients_total: int
    disputes_open: int
