import os
import json
import psycopg2
from psycopg2.extras import RealDictCursor
from fastapi import HTTPException, status
from app.schemas.product_schema import ProductoResponse, OpcionExtraResponse, GrupoOpcionesResponse
from typing import List

class ProductoService:
    def __init__(self):
        pass

    async def obtener_todos_los_productos(self) -> List[ProductoResponse]:
        """
        Consulta en PostgreSQL el catálogo completo de productos disponibles, 
        agrupando de forma nativa sus categorías, grupos de opciones y extras en un JSON.
        """
        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # Consulta avanzada usando agregación JSON nativa de Postgres
            query = """
                SELECT 
                    p.id, 
                    p.nombre, 
                    p.descripcion, 
                    p.precio, 
                    p.imagen_url, 
                    p.disponible, 
                    p.categoria_id,
                    c.nombre AS categoria_nombre,
                    COALESCE(
                        (
                            SELECT json_agg(
                                json_build_object(
                                    'id', go.id,
                                    'nombre', go.nombre,
                                    'min_seleccion', go.min_seleccion,
                                    'max_seleccion', go.max_seleccion,
                                    'opciones_extras', COALESCE(
                                        (
                                            SELECT json_agg(
                                                json_build_object(
                                                    'id', oe.id,
                                                    'nombre', oe.nombre,
                                                    'precio_adicional', oe.precio_adicional,
                                                    'disponible', oe.disponible
                                                )
                                            )
                                            FROM public.opciones_extras oe
                                            WHERE oe.grupo_id = go.id AND oe.disponible = TRUE
                                        ), '[]'::json
                                    )
                                )
                            )
                            FROM public.grupos_opciones go
                            INNER JOIN public.producto_grupo_opciones pgo ON pgo.grupo_id = go.id
                            WHERE pgo.producto_id = p.id
                        ), '[]'::json
                    ) AS grupos_opciones_json
                FROM public.productos p
                LEFT JOIN public.categorias c ON p.categoria_id = c.id
                WHERE p.disponible = TRUE
                ORDER BY p.id ASC;
            """
            
            cursor.execute(query)
            productos_bd = cursor.fetchall()
            cursor.close()

            print(f"🔥 [BACKEND] Consulta avanzada en Render exitosa. Mapeando {len(productos_bd)} productos...", flush=True)

            productos_formateados = []
            
            for item in productos_bd:
                lista_grupos = []
                
                # Psycopg2 a veces mapea el json_agg directo como lista/diccionario, 
                # pero si viene como string de texto crudo, lo decodificamos de forma segura.
                grupos_raw = item["grupos_opciones_json"]
                if isinstance(grupos_raw, str):
                    grupos_raw = json.loads(grupos_raw)

                for g in grupos_raw:
                    opciones_raw = g.get("opciones_extras", [])
                    
                    lista_opciones = [
                        OpcionExtraResponse(
                            id=o["id"],
                            nombre=o["nombre"],
                            precio_adicional=float(o["precio_adicional"]),
                            disponible=o["disponible"]
                        ) for o in opciones_raw if o.get("disponible") == True
                    ]

                    lista_grupos.append(
                        GrupoOpcionesResponse(
                            id=g["id"],
                            nombre=g["nombre"],
                            min_seleccion=g["min_seleccion"],
                            max_seleccion=g["max_seleccion"],
                            opciones=lista_opciones
                        )
                    )

                productos_formateados.append(
                    ProductoResponse(
                        id=item["id"],
                        categoria_id=item["categoria_id"],
                        categoria_nombre=item["categoria_nombre"],
                        nombre=item["nombre"],
                        descripcion=item["descripcion"],
                        precio=float(item["precio"]),
                        imagen_url=item["imagen_url"],
                        disponible=item["disponible"],
                        grupos_opciones=lista_grupos
                    )
                )
                
            return productos_formateados
            
        except Exception as e:
            print(f"❌ Error crítico en ProductoService al armar extras: {str(e)}", flush=True)
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"No se pudo procesar el catálogo con opciones: {str(e)}"
            )
        finally:
            if conn:
                conn.close()