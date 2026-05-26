from datetime import datetime
from app.config import supabase
from app.schemas.pedido_schema import PedidoCreateRequest, PedidoResponse, PedidoStatusResponse

from app.services.gamification_service import GamificationService 
from app.schemas.gamification_schema import ProcesarCompraRequest

gamification_service = GamificationService()

class PedidoService:

    async def verificar_estado_pedido(self, pedido_id: str) -> PedidoStatusResponse:

        resultado = supabase.table("pedidos") \
            .select("pedido_id, estado") \
            .eq("pedido_id", pedido_id) \
            .execute()
    
        if not resultado.data:
            raise Exception(f"No se encontró ningún pedido con el ID: {pedido_id}")
        
        pedido_actual = resultado.data[0]
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


    async def procesar_pedido_real(self, pedido: PedidoCreateRequest) -> PedidoResponse:
        total_calculado = 0.0
        cantidad_productos_total = 0

        for item in pedido.items:
            costo_extras = sum(extra.precio_adicional for extra in item.extras)
            total_calculado += (item.precio_unitario_base + costo_extras) * item.cantidad

        estado_final = "PROCESANDO"

        # Insertar en la tabla principal: 'pedidos'
        pedido_data = {
            "usuario_id": pedido.usuario_id,
            "metodo_pago": pedido.metodo_pago,
            "estado": estado_final,
            "monto_total": round(total_calculado, 2)
        }
        
        res_pedido = supabase.table("pedidos").insert(pedido_data).execute()
        if not res_pedido.data:
            raise Exception("No se pudo registrar el pedido en la tabla 'pedidos' de Supabase.")
            
        nuevo_pedido = res_pedido.data[0]
        id_del_pedido_creado = nuevo_pedido["pedido_id"]

        # Insertar los productos en la tabla: 'pedido_items'
        for item in pedido.items:
            item_data = {
                "pedido_id": id_del_pedido_creado,
                "producto_id": item.producto_id,
                "nombre_producto": item.nombre_producto,
                "cantidad": item.cantidad,
                "precio_unitario_base": item.precio_unitario_base
            }
            res_item = supabase.table("pedido_items").insert(item_data).execute()
            
            if not res_item.data:
                raise Exception(f"Error al insertar el producto {item.nombre_producto} en Supabase.")
                
            id_del_item_creado = res_item.data[0]["id"]

            # Insertar los extras vinculados en la tabla: 'pedido_item_extras'
            if item.extras:
                extras_data_list = []
                for extra in item.extras:
                    extras_data_list.append({
                        "pedido_item_id": id_del_item_creado,
                        "extra_id": extra.id,          # Mapea al 'extra_id' de tu tabla
                        "nombre_extra": extra.nombre,  # Mapea al 'nombre_extra' de tu tabla
                        "precio_adicional": extra.precio_adicional
                    })
                supabase.table("pedido_item_extras").insert(extras_data_list).execute()


        #INSERSION DEL MODULO DE GAMIFICACION
        gamificacion_data = None
        if pedido.metodo_pago:
            try:
                # Armamos el request que tu GamificationService espera
                payload_gamificacion = ProcesarCompraRequest(
                    usuario_id=pedido.usuario_id,
                    monto_total=total_calculado,
                    cantidad_productos=cantidad_productos_total,
                    es_primer_compra_dia=False
                )
                # Ejecutamos tu método asíncrono
                gamificacion_data = await gamification_service.procesar_transaccion(payload_gamificacion)
            except Exception as e:
                print(f"[GAMIFICACIÓN ERROR] No se pudieron otorgar puntos: {str(e)}")


        # 5. Formatear fecha UTC legible a un string estándar para el front
        fecha_raw = nuevo_pedido["fecha_creacion"].replace("+00:00", "")
        fecha_legible = datetime.fromisoformat(fecha_raw).strftime("%Y-%m-%d %H:%M:%S")

        msg_final = "¡Pedido registrado!"
        if gamificacion_data:
            msg_final = f"¡Pago Exitoso! Ganaste +{gamificacion_data['xp_ganada']} XP. Nivel actual: {gamificacion_data['nivel_actual']}"

        # 6. Retornar la respuesta exacta que Kotlin espera recibir
        return PedidoResponse(
            pedido_id=str(id_del_pedido_creado),
            usuario_id=str(nuevo_pedido["usuario_id"]),
            estado=nuevo_pedido["estado"],
            monto_total=float(nuevo_pedido["monto_total"]),
            fecha_creacion=fecha_legible,
            mensaje=msg_final
        )