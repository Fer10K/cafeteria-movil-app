import os
import psycopg2
from psycopg2.extras import RealDictCursor
from datetime import datetime
from typing import List
from fastapi import HTTPException, status
from app.schemas.pedido_schema import PedidoCreateRequest, PedidoResponse, PedidoStatusResponse
from app.services.gamification_service import GamificationService 
from app.schemas.gamification_schema import ProcesarCompraRequest

gamification_service = GamificationService()

class PedidoService:

    async def verificar_estado_pedido(self, pedido_id: str) -> PedidoStatusResponse:
        """Consulta en PostgreSQL el estado actual de un pedido para el polling del alumno."""
        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            query = "SELECT pedido_id, estado FROM public.pedidos WHERE pedido_id = %s;"
            cursor.execute(query, (pedido_id,))
            pedido_actual = cursor.fetchone()

            cursor.close()

            if not pedido_actual:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail=f"No se encontró ningún pedido con el ID: {pedido_id}"
                )
            
            estado = pedido_actual["estado"]
        
            if estado == "PENDIENTE_PAGO":
                mensaje = "El pedido sigue en espera de pago en caja."
            elif estado == "PROCESANDO":
                mensaje = "¡Pago confirmado! El barista está preparando tu orden."
            else:
                mensaje = f"El pedido se encuentra en estado: {estado}"
            
            return PedidoStatusResponse(
                pedido_id=str(pedido_actual["pedido_id"]),
                estado=estado,
                mensaje=mensaje
            )

        except HTTPException as he:
            raise he
        except Exception as e:
            print(f"Error al verificar estado del pedido: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error interno al consultar el estado del pedido."
            )
        finally:
            if conn: conn.close()


    async def procesar_pedido_real(self, pedido: PedidoCreateRequest) -> PedidoResponse:
        """Calcula totales, guarda el pedido de forma atómica e impacta la gamificación."""
        total_calculado = 0.0

        for item in pedido.items:
            costo_extras = sum(extra.precio_adicional for extra in item.extras)
            total_calculado += (item.precio_unitario_base + costo_extras) * item.cantidad

        estado_final = "PROCESANDO"

        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            # 1. Insertar en la tabla principal: 'pedidos'
            query_pedido = """
                INSERT INTO public.pedidos (usuario_id, metodo_pago, estado, monto_total)
                VALUES (%s, %s, %s, %s)
                RETURNING pedido_id, usuario_id, estado, monto_total, fecha_creacion;
            """
            cursor.execute(query_pedido, (
                pedido.usuario_id, 
                pedido.metodo_pago, 
                estado_final, 
                round(total_calculado, 2)
            ))
            nuevo_pedido = cursor.fetchone()
            
            if not nuevo_pedido:
                raise Exception("La base de datos no retornó los datos del pedido creado.")
                
            id_del_pedido_creado = str(nuevo_pedido["pedido_id"])

            # 2. Insertar los productos en la tabla: 'pedido_items'
            for item in pedido.items:
                query_item = """
                    INSERT INTO public.pedido_items (pedido_id, producto_id, nombre_producto, cantidad, precio_unitario_base)
                    VALUES (%s, %s, %s, %s, %s)
                    RETURNING id;
                """
                cursor.execute(query_item, (
                    id_del_pedido_creado,
                    item.producto_id,
                    item.nombre_producto,
                    item.cantidad,
                    item.precio_unitario_base
                ))
                resultado_item = cursor.fetchone()
                
                if not resultado_item:
                    raise Exception(f"Error al insertar el producto {item.nombre_producto}.")
                    
                id_del_item_creado = resultado_item["id"]

                # 3. Insertar los extras vinculados en la tabla: 'pedido_item_extras'
                if item.extras:
                    for extra in item.extras:
                        query_extra = """
                            INSERT INTO public.pedido_item_extras (pedido_item_id, extra_id, nombre_extra, precio_adicional)
                            VALUES (%s, %s, %s, %s);
                        """
                        cursor.execute(query_extra, (
                            id_del_item_creado,
                            extra.id,
                            extra.nombre,
                            extra.precio_adicional
                        ))

            # Si todo el guardado fue exitoso, confirmamos la transacción relacional
            conn.commit()
            cursor.close()

        except Exception as e:
            # En caso de cualquier falla (llaves foráneas, desconexiones), deshacemos todo lo afectado en este intento
            if conn: conn.rollback()
            print(f"Error crítico guardando la transacción del pedido: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"No se pudo registrar la comanda: {str(e)}"
            )
        finally:
            if conn: conn.close()


        # 4. IMPACTO EN MÓDULO DE GAMIFICACIÓN (Se ejecuta por separado del commit principal 
        # para que si falla la lógica de puntos, no interfiera ni le tire abajo la compra al alumno)
        gamificacion_data = None
        if pedido.metodo_pago:
            try:
                payload_gamificacion = ProcesarCompraRequest(
                    usuario_id=pedido.usuario_id,
                    monto_total=total_calculado,
                    cantidad_productos=0,  # Conservado de tu lógica original
                    es_primer_compra_dia=False
                )
                gamificacion_data = await gamification_service.procesar_transaccion(payload_gamificacion)
            except Exception as e:
                print(f"[GAMIFICACIÓN ERROR] No se pudieron otorgar puntos de XP: {str(e)}")


        # 5. Formatear la fecha que retorna PostgreSQL de forma nativa (objeto datetime)
        fecha_bd = nuevo_pedido["fecha_creacion"]
        if isinstance(fecha_bd, datetime):
            fecha_legible = fecha_bd.strftime("%Y-%m-%d %H:%M:%S")
        else:
            # Fallback por si viniera como string mapeado de forma cruda
            fecha_raw = str(fecha_bd).replace("+00:00", "")
            fecha_legible = datetime.fromisoformat(fecha_raw).strftime("%Y-%m-%d %H:%M:%S")

        msg_final = "¡Pedido registrado!"
        if gamificacion_data:
            msg_final = f"¡Pago Exitoso! Ganaste +{gamificacion_data['xp_ganada']} XP. Nivel actual: {gamificacion_data['nivel_actual']}"

        # 6. Retornar la respuesta exacta que el frontend de Android espera recibir
        return PedidoResponse(
            pedido_id=id_del_pedido_creado,
            usuario_id=str(nuevo_pedido["usuario_id"]),
            estado=nuevo_pedido["estado"],
            monto_total=float(nuevo_pedido["monto_total"]),
            fecha_creacion=fecha_legible,
            mensaje=msg_final
        )

    async def obtener_historial_nombres_usuario(self, usuario_id: str) -> List[str]:
        """
        Consulta en PostgreSQL los nombres de los productos que el usuario ha comprado,
        haciendo un JOIN entre pedidos y pedido_items, limitado a los últimos 10.
        """
        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            # Consulta directa y eficiente con JOIN y LIMIT nativo
            query = """
                SELECT pi.nombre_producto 
                FROM public.pedido_items pi
                INNER JOIN public.pedidos p ON pi.pedido_id = p.pedido_id
                WHERE p.usuario_id = %s AND pi.nombre_producto IS NOT NULL
                ORDER BY p.fecha_creacion DESC
                LIMIT 10;
            """
            cursor.execute(query, (usuario_id,))
            resultados = cursor.fetchall()
            cursor.close()

            # Extraemos los strings en una lista plana
            nombres_productos = [item["nombre_producto"] for item in resultados]
            return nombres_productos

        except Exception as e:
            print(f"Error al procesar el historial relacional de pedidos: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Error al recuperar el historial de consumos: {str(e)}"
            )
        finally:
            if conn:
                conn.close()