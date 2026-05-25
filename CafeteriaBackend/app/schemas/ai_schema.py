from pydantic import BaseModel, Field
from typing import List, Optional

class HistorialCompraSchema(BaseModel):
    producto_nombre: str = Field(..., example="Café Americano")

class ProductoDisponibleSchema(BaseModel):
    id: str
    nombre: str
    precio: float
    categoria: str

# 🔥 NUEVO: Esquema unificado para soportar los 3 flujos sin choques
class RecomendacionRequest(BaseModel):
    usuario_id: str
    tipo_contexto: str = Field(..., example="inicio", description="Valores válidos: 'inicio', 'carrito', 'gamificacion'")
    historial: List[HistorialCompraSchema] = Field(default=[])
    productos_disponibles: List[ProductoDisponibleSchema] = Field(default=[])
    
    # Campos adicionales opcionales para la pantalla de Gamificación
    puntos_usuario_actual: Optional[int] = Field(default=None, description="XP acumulada por el alumno")
    puntos_siguiente_usuario: Optional[int] = Field(default=None, description="XP del rival a vencer en el leaderboard")
    nombre_siguiente_usuario: Optional[str] = Field(default=None, description="Nombre del alumno que va arriba")

class RecomendacionResponse(BaseModel):
    usuario_id: str
    recomendacion: str