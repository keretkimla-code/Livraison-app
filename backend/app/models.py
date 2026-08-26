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


class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, default=lambda: gen_id("USR"))
    phone = Column(String, unique=True, index=True, nullable=False)
    full_name = Column(String, nullable=True)
    role = Column(Enum(UserRole), nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

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
