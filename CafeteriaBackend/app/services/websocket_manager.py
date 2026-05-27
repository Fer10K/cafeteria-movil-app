from fastapi import WebSocket
from typing import List

class BaristaConnectionManager:
    def __init__(self):
        # Lista para almacenar las conexiones WebSocket activas de los baristas
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        """Acepta la conexión del barista y la guarda en la lista."""
        await websocket.accept()
        self.active_connections.append(websocket)
        print(f"☕ Barista conectado. Total conectados: {len(self.active_connections)}")

    def disconnect(self, websocket: WebSocket):
        """Elimina al barista de la lista cuando cierra la app o pierde internet."""
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)
            print(f"Barista desconectado. Total conectados: {len(self.active_connections)}")

    async def broadcast_nuevo_pedido(self, pedido_data: dict):
        """Envía los datos del nuevo pedido a TODOS los baristas conectados."""
        mensaje = {
            "evento": "NUEVO_PEDIDO",
            "data": pedido_data
        }
        for connection in self.active_connections:
            try:
                await connection.send_json(mensaje)
            except Exception:
                # Si una conexión falló por alguna razón, nos aseguramos de no romper el ciclo
                pass

barista_manager = BaristaConnectionManager()