from datetime import datetime
from app.config import supabase
from app.schemas.pedido_schema import PedidoCreateRequest, PedidoResponse

class PedidoService:
    async def procesar_pedido_real(self, pedido: PedidoCreateRequest) -> PedidoResponse:
        # 1. Calcular el monto total de forma segura en el backend
        total_calculado = 0.0
        for item in pedido.items:
            costo_extras = sum(extra.precio_adicional for extra in item.extras)
            total_calculado += (item.precio_unitario_base + costo_extras) * item.cantidad

        # Cambiar estado inicial si se procesa con NFC de forma inmediata
        estado_final = "PROCESANDO" if pedido.metodo_pago == "NFC" else pedido.estado

        # 2. Insertar el registro principal en la tabla 'pedidos'
        pedido_data = {
            "usuario_id": pedido.usuario_id,
            "metodo_pago": pedido.metodo_pago,
            "estado": estado_final,
            "monto_total": round(total_calculado, 2)
        }
        
        res_pedido = supabase.table("pedidos").insert(pedido_data).execute()
        
        if not res_pedido.data:
            raise Exception("No se pudo registrar el pedido principal en Supabase.")
            
        nuevo_pedido = res_pedido.data[0]
        id_del_pedido_creado = nuevo_pedido["pedido_id"]

        # 3. Insertar los productos correspondientes (pedido_items)
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
                raise Exception(f"Error al insertar el producto {item.nombre_producto} en la orden.")
                
            id_del_item_creado = res_item.data[0]["id"]

            # 4. Si el producto tiene extras, los guardamos apuntando a este item
            if item.extras:
                extras_data_list = []
                for extra in item.extras:
                    extras_data_list.append({
                        "pedido_item_id": id_del_item_creado,
                        "extra_id": extra.id,
                        "nombre_extra": extra.nombre,
                        "precio_adicional": extra.precio_adicional
                    })
                supabase.table("pedido_item_extras").insert(extras_data_list).execute()

        # 5. Formatear y construir la respuesta exacta para el frente de Kotlin
        # Ajustamos el string de la fecha para que use un formato ISO estándar legible por Gson
        fecha_legible = datetime.fromisoformat(nuevo_pedido["fecha_creacion"].replace("+00:00", "")).strftime("%Y-%m-%d %H:%M:%S")

        return PedidoResponse(
            pedido_id=str(id_del_pedido_creado),
            usuario_id=str(nuevo_pedido["usuario_id"]),
            estado=nuevo_pedido["estado"],
            monto_total=float(nuevo_pedido["monto_total"]),
            fecha_creacion=fecha_legible,
            mensaje="¡Pedido registrado y procesado exitosamente en base de datos!"
        )