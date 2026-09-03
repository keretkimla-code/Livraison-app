from typing import Dict, List

from fastapi import APIRouter, WebSocket, WebSocketDisconnect

router = APIRouter(tags=["websocket"])


class ConnectionManager:
    """
    Gère les connexions WebSocket ouvertes par commande, pour diffuser en
    temps réel les mises à jour de statut et de position du livreur au
    client (et inversement). Implémentation en mémoire, suffisante pour
    un seul processus — à remplacer par Redis Pub/Sub si le backend est
    déployé sur plusieurs instances (V1).
    """

    def __init__(self) -> None:
        self.active_connections: Dict[str, List[WebSocket]] = {}

    async def connect(self, order_id: str, websocket: WebSocket) -> None:
        await websocket.accept()
        self.active_connections.setdefault(order_id, []).append(websocket)

    def disconnect(self, order_id: str, websocket: WebSocket) -> None:
        connections = self.active_connections.get(order_id, [])
        if websocket in connections:
            connections.remove(websocket)
        if not connections and order_id in self.active_connections:
            del self.active_connections[order_id]

    async def broadcast(self, order_id: str, message: dict) -> None:
        for connection in list(self.active_connections.get(order_id, [])):
            try:
                await connection.send_json(message)
            except Exception:
                self.disconnect(order_id, connection)


manager = ConnectionManager()


@router.websocket("/ws/orders/{order_id}")
async def order_tracking_socket(websocket: WebSocket, order_id: str):
    """
    Le client et le livreur se connectent ici pendant une course pour
    recevoir en direct : changements de statut, position GPS du livreur,
    messages de chat. Les mises à jour sont envoyées via manager.broadcast()
    depuis les routes REST correspondantes (voir orders.py).
    """
    await manager.connect(order_id, websocket)
    try:
        while True:
            # On ne fait qu'écouter la connexion ouverte ; les messages
            # entrants (ex. chat) peuvent être traités ici si besoin.
            await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(order_id, websocket)
