from pydantic import BaseModel
from typing import List

# Subdetalle: Los extras que lleva un producto específico
class ExtraResponse(BaseModel):
    nombre_extra: str

# Detalle: El producto solicitado con sus respectivos extras anidados
class ItemPedidoResponse(BaseModel):
    pedido_item_id: int
    nombre_producto: str
    cantidad: int
    extras: List[ExtraResponse]

# Maestro: El pedido completo que consume la interfaz del Barista
class PedidoBaristaResponse(BaseModel):
    pedido_id: str
    usuario_id: str
    nombre_usuario: str
    estado: str
    monto_total: float
    productos: List[ItemPedidoResponse]