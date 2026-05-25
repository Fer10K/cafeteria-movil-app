from pydantic import BaseModel
from typing import Optional, List

class OpcionExtraResponse(BaseModel):
    id: int
    nombre: str
    precio_adicional: float
    disponible: bool

    class Config:
        from_attributes = True


class GrupoOpcionesResponse(BaseModel):
    id: int
    nombre: str
    min_seleccion: int
    max_seleccion: int
    opciones: List[OpcionExtraResponse]

    class Config:
        from_attributes = True

class ProductoResponse(BaseModel):
    id: int
    categoria_id: Optional[int]
    categoria_nombre: Optional[str]
    nombre: str
    descripcion: Optional[str]
    precio: float
    imagen_url: Optional[str]
    disponible: bool

    grupos_opciones: List[GrupoOpcionesResponse]

    class Config:
        from_attributes = True