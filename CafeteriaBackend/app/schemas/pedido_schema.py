from pydantic import BaseModel, Field
from typing import List

# =====================================================================
# 1. ESQUEMAS DE SOPORTE
# =====================================================================

class ExtraPedidoRequest(BaseModel):
    # Mapea 'extra_id' enviado por Android al atributo 'id' interno
    id: int = Field(validation_alias="extra_id")
    # Mapea 'nombre_extra' enviado por Android al atributo 'nombre' interno
    nombre: str = Field(validation_alias="nombre_extra")
    precio_adicional: float

class ItemPedidoRequest(BaseModel):
    producto_id: int
    nombre_producto: str
    cantidad: int
    precio_unitario_base: float
    extras: List[ExtraPedidoRequest]


# =====================================================================
# 2. ESQUEMA DE ENTRADA PRINCIPAL
# =====================================================================

class PedidoCreateRequest(BaseModel):
    usuario_id: str
    metodo_pago: str  # "EFECTIVO" o "NFC"
    estado: str = "PENDIENTE_PAGO"
    items: List[ItemPedidoRequest]


# =====================================================================
# 3. ESQUEMA DE SALIDA (Lo que Kotlin mapeará en PedidoResponse)
# =====================================================================

class PedidoResponse(BaseModel):
    pedido_id: str
    usuario_id: str
    estado: str
    monto_total: float
    fecha_creacion: str
    mensaje: str


#
# #MOLDE PARA RESPONDER AL FRONT SIN :ccccc

class PedidoStatusResponse(BaseModel):
    pedido_id: str
    estado: str
    mensaje: str
