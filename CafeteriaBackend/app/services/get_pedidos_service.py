import os
import psycopg2
from psycopg2.extras import RealDictCursor
from typing import List
from fastapi import HTTPException, status
from app.schemas.get_pedidos_schema import PedidoBaristaResponse, ItemPedidoResponse, ExtraResponse

class BaristaService:
    def __init__(self):
        # Eliminamos la dependencia directa con el cliente global de Supabase
        pass

    def _mapear_estructura_pedidos(self, cursor, lista_pedidos_bd: list) -> List[PedidoBaristaResponse]:
        """Método privado auxiliar para reutilizar el mapeo relacional de Pedido -> Items -> Extras"""
        respuesta_final = []

        for ped in lista_pedidos_bd:
            p_id = str(ped.get("pedido_id"))

            # 1. Consultar los productos (items) del pedido actual
            query_items = """
                SELECT id, nombre_producto, cantidad 
                FROM public.pedido_items 
                WHERE pedido_id = %s;
            """
            cursor.execute(query_items, (p_id,))
            lista_items_bd = cursor.fetchall()
            
            lista_productos = []

            # 2. Consultar los extras de cada producto hallado
            for item in lista_items_bd:
                item_id = item.get("id")

                query_extras = """
                    SELECT nombre_extra 
                    FROM public.pedido_item_extras 
                    WHERE pedido_item_id = %s;
                """
                cursor.execute(query_extras, (item_id,))
                lista_extras_bd = cursor.fetchall()

                lista_extras = [
                    ExtraResponse(nombre_extra=ext.get("nombre_extra")) 
                    for ext in lista_extras_bd
                ]

                # Añadir el producto estructurado
                lista_productos.append(
                    ItemPedidoResponse(
                        pedido_item_id=item_id,
                        nombre_producto=item.get("nombre_producto"),
                        cantidad=item.get("cantidad"),
                        extras=lista_extras
                    )
                )

            # 3. Armar el objeto maestro consolidado
            respuesta_final.append(
                PedidoBaristaResponse(
                    pedido_id=p_id,
                    usuario_id=str(ped.get("usuario_id")),
                    estado=ped.get("estado"),
                    monto_total=float(ped.get("monto_total")),
                    productos=lista_productos
                )
            )

        return respuesta_final

    async def obtener_comandas_activas(self) -> List[PedidoBaristaResponse]:
        """Consulta pedidos en estado PROCESANDO o LISTO y construye sus ítems y extras."""
        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            # En SQL puro el filtro .in_() se maneja de forma nativa con IN (%s, %s)
            query_pedidos = """
                SELECT pedido_id, usuario_id, estado, monto_total 
                FROM public.pedidos 
                WHERE estado IN ('PROCESANDO', 'LISTO')
                ORDER BY fecha_creacion ASC;
            """
            cursor.execute(query_pedidos)
            lista_pedidos_bd = cursor.fetchall()

            if not lista_pedidos_bd:
                cursor.close()
                return []

            respuesta = self._mapear_estructura_pedidos(cursor, lista_pedidos_bd)
            cursor.close()
            return respuesta

        except Exception as e:
            print(f"Error al obtener comandas activas: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno al consultar las comandas activas."
            )
        finally:
            if conn: conn.close()

    async def obtener_pedidos_entregados(self) -> List[PedidoBaristaResponse]:
        """Consulta únicamente los pedidos que ya fueron entregados al alumno."""
        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            query_pedidos = """
                SELECT pedido_id, usuario_id, estado, monto_total 
                FROM public.pedidos 
                WHERE estado = 'ENTREGADO'
                ORDER BY fecha_creacion DESC;
            """
            cursor.execute(query_pedidos)
            lista_pedidos_bd = cursor.fetchall()

            if not lista_pedidos_bd:
                cursor.close()
                return []

            respuesta = self._mapear_estructura_pedidos(cursor, lista_pedidos_bd)
            cursor.close()
            return respuesta

        except Exception as e:
            print(f"Error al obtener pedidos entregados: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno al consultar el historial de entregas."
            )
        finally:
            if conn: conn.close()

    async def obtener_pedidos_porconfirmar(self) -> List[PedidoBaristaResponse]:
        """Consulta únicamente los pedidos pendientes de pago en caja."""
        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            query_pedidos = """
                SELECT pedido_id, usuario_id, estado, monto_total 
                FROM public.pedidos 
                WHERE estado = 'PENDIENTE_PAGO'
                ORDER BY fecha_creacion DESC;
            """
            cursor.execute(query_pedidos)
            lista_pedidos_bd = cursor.fetchall()

            if not lista_pedidos_bd:
                cursor.close()
                return []

            respuesta = self._mapear_estructura_pedidos(cursor, lista_pedidos_bd)
            cursor.close()
            return respuesta

        except Exception as e:
            print(f"Error al obtener pedidos por confirmar: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno al consultar los pedidos pendientes de pago."
            )
        finally:
            if conn: conn.close()

    async def obtener_pedidos_cancelados(self) -> List[PedidoBaristaResponse]:
        """Consulta únicamente los pedidos que fueron cancelados."""
        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            query_pedidos = """
                SELECT pedido_id, usuario_id, estado, monto_total 
                FROM public.pedidos 
                WHERE estado = 'CANCELADO'
                ORDER BY fecha_creacion DESC;
            """
            cursor.execute(query_pedidos)
            lista_pedidos_bd = cursor.fetchall()

            if not lista_pedidos_bd:
                cursor.close()
                return []

            respuesta = self._mapear_estructura_pedidos(cursor, lista_pedidos_bd)
            cursor.close()
            return respuesta

        except Exception as e:
            print(f"Error al obtener pedidos cancelados: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno al consultar los pedidos cancelados."
            )
        finally:
            if conn: conn.close()

    async def cambiar_estado_pedido(self, pedido_id: str, nuevo_estado: str) -> bool:
        """Modifica el estado de un pedido específico en PostgreSQL."""
        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            query_update = """
                UPDATE public.pedidos 
                SET estado = %s 
                WHERE pedido_id = %s;
            """
            cursor.execute(query_update, (nuevo_estado, pedido_id))
            
            # rowcount devuelve cuántas filas fueron afectadas por el UPDATE
            filas_afectadas = cursor.rowcount
            
            conn.commit()
            cursor.close()
            
            return filas_afectadas > 0
        except Exception as e:
            if conn: conn.rollback()
            print(f"Error al actualizar estado del pedido {pedido_id}: {str(e)}")
            return False
        finally:
            if conn: conn.close()