from pydantic import BaseModel, Field
from typing import List


class HistorialCompraSchema(BaseModel):
    producto_nombre: str = Field(..., example="Café Americano", description="Nombre del producto comprado")
    categoria: str = Field(..., example="Bebidas Calientes", description="Categoría del producto")
    fecha: str = Field(..., example="2026-05-20", description="Fecha de la compra")


class ProductoDisponibleSchema(BaseModel):
    id: str = Field(..., example="prod_123", description="ID único del producto en Supabase")
    nombre: str = Field(..., example="Muffin de Arándanos", description="Nombre comercial")
    precio: float = Field(..., example=35.00, description="Precio de venta al público")
    stock: int = Field(..., example=15, description="Cantidad disponible en el inventario")
    categoria: str = Field(..., example="Repostería", description="Categoría para armar los combos")


#Esquema para la ia
class RecomendacionRequest(BaseModel):
    usuario_id: str = Field(..., example="usr_987", description="UUID del estudiante en la base de datos")
    historial: List[HistorialCompraSchema] = Field(
        default=[], 
        description="Lista de las últimas compras del alumno extraídas de Supabase"
    )
    productos_disponibles: List[ProductoDisponibleSchema] = Field(
        ..., 
        description="Lista del inventario actual de la cafetería con stock disponible"
    )


#Esquema de respouesta
class RecomendacionResponse(BaseModel):
    usuario_id: str
    recomendacion: str = Field(..., description="Texto corto sugerido por Gemini listo para mostrar en la interfaz")