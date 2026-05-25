from datetime import datetime
from app.config import supabase
from app.schemas.pedido_schema import PedidoCreateRequest, PedidoResponse, PedidoStatusResponse

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
        # 1. Calcular el monto total sumando productos y extras de forma dinámica
        total_calculado = 0.0
        for item in pedido.items:
            costo_extras = sum(extra.precio_adicional for extra in item.extras)
            total_calculado += (item.precio_unitario_base + costo_extras) * item.cantidad

        # Cambiar el estado según el método de pago
        estado_final = "PROCESANDO" if pedido.metodo_pago == "NFC" else pedido.estado

        # 2. Insertar en la tabla principal: 'pedidos'
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

        # 3. Insertar los productos en la tabla: 'pedido_items'
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

            # 4. Insertar los extras vinculados en la tabla: 'pedido_item_extras'
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

        # 5. Formatear fecha UTC legible a un string estándar para el front
        fecha_raw = nuevo_pedido["fecha_creacion"].replace("+00:00", "")
        fecha_legible = datetime.fromisoformat(fecha_raw).strftime("%Y-%m-%d %H:%M:%S")

        # 6. Retornar la respuesta exacta que Kotlin espera recibir
        return PedidoResponse(
            pedido_id=str(id_del_pedido_creado),
            usuario_id=str(nuevo_pedido["usuario_id"]),
            estado=nuevo_pedido["estado"],
            monto_total=float(nuevo_pedido["monto_total"]),
            fecha_creacion=fecha_legible,
            mensaje="¡Pedido registrado y procesado exitosamente en base de datos!"
        )