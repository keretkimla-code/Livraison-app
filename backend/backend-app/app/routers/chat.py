from typing import List

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.models import User, Order, ChatMessage
from app.schemas import ChatMessageIn, ChatMessageOut
from app.security import get_current_user
from app.routers.ws import manager

router = APIRouter(prefix="/orders", tags=["chat"])


def _order_or_404(db: Session, order_id: str) -> Order:
    order = db.query(Order).filter(Order.id == order_id).first()
    if order is None:
        raise HTTPException(status_code=404, detail="Commande introuvable")
    return order


def _ensure_participant(order: Order, user: User) -> None:
    if user.id not in (order.client_id, order.courier_id):
        raise HTTPException(status_code=403, detail="Tu n'es pas rattaché à cette commande")


@router.get("/{order_id}/messages", response_model=List[ChatMessageOut])
def list_messages(
    order_id: str,
    db: Session = Depends(get_db),
    user: User = Depends(get_current_user),
):
    order = _order_or_404(db, order_id)
    _ensure_participant(order, user)
    return (
        db.query(ChatMessage)
        .filter(ChatMessage.order_id == order_id)
        .order_by(ChatMessage.created_at.asc())
        .all()
    )


@router.post("/{order_id}/messages", response_model=ChatMessageOut)
async def send_message(
    order_id: str,
    payload: ChatMessageIn,
    db: Session = Depends(get_db),
    user: User = Depends(get_current_user),
):
    order = _order_or_404(db, order_id)
    _ensure_participant(order, user)

    message = ChatMessage(order_id=order_id, sender_role=user.role, text=payload.text)
    db.add(message)
    db.commit()
    db.refresh(message)

    await manager.broadcast(
        order_id,
        {
            "event": "chat_message",
            "sender_role": message.sender_role.value,
            "text": message.text,
            "created_at": message.created_at.isoformat(),
        },
    )
    return message
