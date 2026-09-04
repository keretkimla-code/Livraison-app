import enum
import uuid
from datetime import datetime

from sqlalchemy import (
    Column, String, Float, Boolean, DateTime, ForeignKey, Enum, Integer, Text
)
from sqlalchemy.orm import relationship

from app.database import Base


def gen_id(prefix: str) -> str:
    return f"{prefix}-{uuid.uuid4().hex[:8].upper()}"


class UserRole(str, enum.Enum):
    client = "client"
    courier = "courier"
    admin = "admin"


class CourierStatus(str, enum.Enum):
    not_submitted = "not_submitted"
    pending = "pending"
    validated = "validated"
    rejected = "rejected"


class VehicleType(str, enum.Enum):
    moto = "moto"
    tricycle = "tricycle"
    vehicule = "vehicule"


class ParcelType(str, enum.Enum):
    document = "document"
    colis_leger = "colis_leger"
    colis_lourd = "colis_lourd"
    repas = "repas"
    courses = "courses"


class OrderStatus(str, enum.Enum):
    pending = "pending"                    # créée, en recherche de livreur
    accepted = "accepted"                   # livreur assigné
    heading_to_pickup = "heading_to_pickup"
    at_pickup = "at_pickup"
    heading_to_dropoff = "heading_to_dropoff"
    at_dropoff = "at_dropoff"
    delivered = "delivered"                 # code/photo confirmés
    paid = "paid"
    cancelled = "cancelled"


class DisputeStatus(str, enum.Enum):
    open = "open"
    in_review = "in_review"
    resolved = "resolved"


class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, default=lambda: gen_id("USR"))
    # Pour les rôles client/livreur : numéro de téléphone (auth par OTP).
    # Pour le rôle admin : on y stocke l'email de connexion (voir password_hash).
    phone = Column(String, unique=True, index=True, nullable=False)
    full_name = Column(String, nullable=True)
    role = Column(Enum(UserRole), nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Utilisé uniquement pour les comptes admin (back-office web), qui se
    # connectent par email + mot de passe plutôt que par OTP téléphone.
    password_hash = Column(String, nullable=True)

    courier_profile = relationship("CourierProfile", back_populates="user", uselist=False)


class CourierProfile(Base):
    __tablename__ = "courier_profiles"

    id = Column(String, primary_key=True, default=lambda: gen_id("CRR"))
    user_id = Column(String, ForeignKey("users.id"), unique=True, nullable=False)

    vehicle_type = Column(Enum(VehicleType), default=VehicleType.moto)
    plate_number = Column(String, nullable=True)
    id_document_uploaded = Column(Boolean, default=False)
    vehicle_photo_uploaded = Column(Boolean, default=False)
    status = Column(Enum(CourierStatus), default=CourierStatus.not_submitted)

    is_available = Column(Boolean, default=False)
    current_lat = Column(Float, nullable=True)
    current_lng = Column(Float, nullable=True)
    location_updated_at = Column(DateTime, nullable=True)

    rating_avg = Column(Float, default=5.0)
    rating_count = Column(Integer, default=0)
    total_earnings = Column(Integer, default=0)

    user = relationship("User", back_populates="courier_profile")


class Order(Base):
    __tablename__ = "orders"

    id = Column(String, primary_key=True, default=lambda: gen_id("CMD"))
    client_id = Column(String, ForeignKey("users.id"), nullable=False)
    courier_id = Column(String, ForeignKey("users.id"), nullable=True)

    pickup_address = Column(String, nullable=False)
    pickup_lat = Column(Float, nullable=False)
    pickup_lng = Column(Float, nullable=False)
    dropoff_address = Column(String, nullable=False)
    dropoff_lat = Column(Float, nullable=False)
    dropoff_lng = Column(Float, nullable=False)

    parcel_type = Column(Enum(ParcelType), default=ParcelType.colis_leger)
    distance_km = Column(Float, nullable=False)
    price = Column(Integer, nullable=False)

    status = Column(Enum(OrderStatus), default=OrderStatus.pending)
    delivery_code = Column(String, nullable=True)
    payment_method = Column(String, nullable=True)

    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class Rating(Base):
    __tablename__ = "ratings"

    id = Column(String, primary_key=True, default=lambda: gen_id("RTG"))
    order_id = Column(String, ForeignKey("orders.id"), unique=True, nullable=False)
    rating = Column(Integer, nullable=False)
    comment = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)


class ChatMessage(Base):
    __tablename__ = "chat_messages"

    id = Column(String, primary_key=True, default=lambda: gen_id("MSG"))
    order_id = Column(String, ForeignKey("orders.id"), nullable=False)
    sender_role = Column(Enum(UserRole), nullable=False)
    text = Column(Text, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)


class ZoneTarifaire(Base):
    """
    Correspond à la table `zones_tarifaires` du schéma directeur.
    Gérée depuis le back-office admin (section 3.3 / 4 du schéma).

    NOTE (limite connue) : sans PostGIS, on ne fait pas de géofencing par
    polygone. La zone marquée `is_default=True` sert de grille tarifaire
    active pour tout le pays/toutes les commandes tant qu'un vrai découpage
    géographique par zone n'est pas implémenté (V1).
    """

    __tablename__ = "zones_tarifaires"

    id = Column(String, primary_key=True, default=lambda: gen_id("ZNE"))
    nom_zone = Column(String, nullable=False, unique=True)
    tarif_base = Column(Integer, nullable=False)
    tarif_km = Column(Integer, nullable=False)
    heure_pointe_multiplicateur = Column(Float, default=1.0)
    is_default = Column(Boolean, default=False)
    actif = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class Dispute(Base):
    """Litiges / support client, gérés depuis le back-office admin (section 3.3)."""

    __tablename__ = "disputes"

    id = Column(String, primary_key=True, default=lambda: gen_id("LIT"))
    order_id = Column(String, ForeignKey("orders.id"), nullable=False)
    reported_by_id = Column(String, ForeignKey("users.id"), nullable=False)
    reason = Column(String, nullable=False)
    description = Column(Text, nullable=True)
    status = Column(Enum(DisputeStatus), default=DisputeStatus.open)
    resolution_note = Column(Text, nullable=True)
    resolved_by_id = Column(String, ForeignKey("users.id"), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    resolved_at = Column(DateTime, nullable=True)


class GpsLog(Base):
    """
    Historique append-only des positions livreurs (section 7 du schéma :
    « logs de géolocalisation conservés pour litiges/sécurité »).
    Distinct de CourierProfile.current_lat/current_lng, qui ne garde que la
    dernière position connue pour le matching temps réel.
    """

    __tablename__ = "gps_logs"

    id = Column(String, primary_key=True, default=lambda: gen_id("GPS"))
    courier_id = Column(String, ForeignKey("users.id"), nullable=False, index=True)
    order_id = Column(String, ForeignKey("orders.id"), nullable=True, index=True)
    lat = Column(Float, nullable=False)
    lng = Column(Float, nullable=False)
    timestamp = Column(DateTime, default=datetime.utcnow, index=True)
