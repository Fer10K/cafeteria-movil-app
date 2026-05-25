import os
from supabase import create_client
from app.schemas.product_schema import ProductoResponse, OpcionExtraResponse, GrupoOpcionesResponse
from typing import List

class ProductoService:
    def obtener_todos_los_productos(self) -> List[ProductoResponse]:
        try:
            url: str = os.environ.get("SUPABASE_URL")
            key: str = os.environ.get("SUPABASE_KEY")
            cliente_local = create_client(url, key)
            
            respuesta = cliente_local.table("productos")\
                .select("""
                    id, nombre, descripcion, precio, imagen_url, disponible, categoria_id,
                    categorias(nombre),
                    grupos_opciones!producto_grupo_opciones(
                        id, nombre, min_seleccion, max_seleccion,
                        opciones_extras(id, nombre, precio_adicional, disponible)
                    )
                """)\
                .eq("disponible", True)\
                .execute()

            print(f"🔥 [BACKEND] Consulta avanzada exitosa. Mapeando {len(respuesta.data)} productos...", flush=True)

            productos_formateados = []
            for item in respuesta.data:
                categoria_info = item.get("categorias", {})
                nombre_cat = categoria_info.get("nombre") if isinstance(categoria_info, dict) else None

                lista_grupos = []
                grupos_raw = item.get("grupos_opciones", [])
                
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
                        categoria_nombre=nombre_cat,
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
            raise Exception("No se pudo procesar el catálogo con opciones.")
