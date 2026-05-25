from pydantic import BaseModel
from typing import List

# =====================================================================
# 1. ESQUEMAS DE SOPORTE (Basados en OpcionExtraResponse e ItemPedidoRequest)
# =====================================================================

class ExtraPedidoRequest(BaseModel):
    # Usamos id y nombre a secas, tal cual viene en OpcionExtraResponse
    id: int
    nombre: str
    precio_adicional: float

class ItemPedidoRequest(BaseModel):
    producto_id: int
    nombre_producto: str
    cantidad: int
    precio_unitario_base: float
    extras: List[ExtraPedidoRequest]


# =====================================================================
# 2. ESQUEMA DE ENTRADA PRINCIPAL (PedidoCreateRequest)
# =====================================================================

class PedidoCreateRequest(BaseModel):
    usuario_id: str
    metodo_pago: str  # "EFECTIVO" o "NFC"
    estado: str = "PENDIENTE_PAGO"
    items: List[ItemPedidoRequest]


# =====================================================================
# 3. ESQUEMA DE SALIDA (PedidoResponse)
# =====================================================================

class PedidoResponse(BaseModel):
    pedido_id: str
    usuario_id: str
    estado: str
    monto_total: float
    fecha_creacion: str
    mensaje: str