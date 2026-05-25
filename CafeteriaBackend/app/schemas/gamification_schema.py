from pydantic import BaseModel, Field
from typing import List, Optional

# 1. Representación de un logro/medalla desbloqueada
class LogroDesbloqueadoSchema(BaseModel):
    id: str = Field(..., example="logro_001", description="ID único del logro en Supabase")
    nombre: str = Field(..., example="Primer Café del Día", description="Nombre comercial del logro")
    descripcion: str = Field(..., example="Has comprado tu primer café antes de las 9:00 AM", description="Detalle de la hazaña")
    icono_url: Optional[str] = Field(None, example="https://supabase.com/storage/.../cafe.png", description="Ruta de la medalla para pintar en Android")

# 2. Esquema Request Body
class ProcesarCompraRequest(BaseModel):
    usuario_id: str = Field(..., example="usr_987", description="UUID del estudiante en la base de datos")
    monto_total: float = Field(..., example=125.50, description="Total gastado en la transacción para calcular la XP")
    cantidad_productos: int = Field(..., example=3, description="Número de productos comprados en este ticket")
    es_primer_compra_dia: bool = Field(default=False, description="Bandera para lógica de logros diarios")

# 3. Esquema Response Body
class ProcesarCompraResponse(BaseModel):
    usuario_id: str
    xp_ganada: int = Field(..., example=125, description="XP obtenida en esta transacción (ej: 1 XP por cada peso)")
    xp_actual: int = Field(..., example=2450, description="XP total acumulada del usuario tras la compra")
    nivel_actual: int = Field(..., example=5, description="Nivel actual del alumno")
    subio_de_nivel: bool = Field(..., example=False, description="Indica a Android si debe disparar la animación de Level Up 🎉")
    logros_nuevos: List[LogroDesbloqueadoSchema] = Field(
        default=[], 
        description="Lista de medallas que el usuario desbloqueó estrictamente con esta compra"
    )

    #Esquema obtener PErfiles

class PosicionLeaderboard(BaseModel):
    usuario_id: str
    nombre: str
    xp_total: int
    nivel: int
    avatar_url: str

    class Config:
        from_attributes = True