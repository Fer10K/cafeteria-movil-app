from typing import List
from supabase import Client
from app.main import supabase
from app.schemas.get_pedidos_schema import PedidoBaristaResponse, ItemPedidoResponse, ExtraResponse

class BaristaService:
    def __init__(self):
        self.db: Client = supabase

    async def obtener_comandas_activas(self) -> List[PedidoBaristaResponse]:
        """
        Consulta en Supabase los pedidos en estado PROCESANDO o LISTO 
        y construye recursivamente sus ítems y extras.
        """
        # 1. Traer pedidos que estén en cualquiera de los dos estados activos para el barista
        pedidos_query = self.db.table("pedidos")\
            .select("pedido_id, usuario_id, estado, monto_total")\
            .in_("estado", ["PROCESANDO", "LISTO"])\
            .execute()
        
        lista_pedidos_bd = pedidos_query.data
        if not lista_pedidos_bd:
            return []

        respuesta_final = []

        # 2. Construir la estructura relacional para cada pedido
        for ped in lista_pedidos_bd:
            p_id = ped.get("pedido_id")

            # 3. Consultar los productos (items) del pedido actual
            items_query = self.db.table("pedido_items")\
                .select("id, nombre_producto, cantidad")\
                .eq("pedido_id", p_id)\
                .execute()
            
            lista_productos = []

            # 4. Consultar los extras de cada producto hallado
            for item in items_query.data:
                item_id = item.get("id")

                extras_query = self.db.table("pedido_item_extras")\
                    .select("nombre_extra")\
                    .eq("pedido_item_id", item_id)\
                    .execute()

                lista_extras = [
                    ExtraResponse(nombre_extra=ext.get("nombre_extra")) 
                    for ext in extras_query.data
                ]

                # Añadir el producto con su lista de extras (vacía o llena)
                lista_productos.append(
                    ItemPedidoResponse(
                        pedido_item_id=item_id,
                        nombre_producto=item.get("nombre_producto"),
                        cantidad=item.get("cantidad"),
                        extras=lista_extras
                    )
                )

            # 5. Armar el objeto maestro consolidado
            respuesta_final.append(
                PedidoBaristaResponse(
                    pedido_id=p_id,
                    usuario_id=ped.get("usuario_id"),
                    estado=ped.get("estado"),
                    monto_total=float(ped.get("monto_total")),
                    productos=lista_productos
                )
            )

        return respuesta_final
    


    async def obtener_pedidos_entregados(self) -> List[PedidoBaristaResponse]:
        """
        Consulta en Supabase únicamente los pedidos que ya fueron entregados 
        al alumno para el historial de la barra.
        """
        try:
            # 1. Filtramos estrictamente por el estado ENTREGADO
            pedidos_query = self.db.table("pedidos")\
                .select("pedido_id, usuario_id, estado, monto_total")\
                .eq("estado", "ENTREGADO")\
                .execute()

            lista_pedidos_bd = pedidos_query.data
            if not lista_pedidos_bd:
                return []

            respuesta_final = []

            # 2. Mismo mapeo relacional recursivo (Items -> Extras)
            for ped in lista_pedidos_bd:
                p_id = ped.get("pedido_id")

                items_query = self.db.table("pedido_items")\
                    .select("id, nombre_producto, cantidad")\
                    .eq("pedido_id", p_id)\
                    .execute()

                lista_productos = []

                for item in items_query.data:
                    item_id = item.get("id")

                    extras_query = self.db.table("pedido_item_extras")\
                        .select("nombre_extra")\
                        .eq("pedido_item_id", item_id)\
                        .execute()

                    lista_extras = [
                        ExtraResponse(nombre_extra=ext.get("nombre_extra")) 
                        for ext in extras_query.data
                    ]

                    lista_productos.append(
                        ItemPedidoResponse(
                            pedido_item_id=item_id,
                            nombre_producto=item.get("nombre_producto"),
                            cantidad=item.get("cantidad"),
                            extras=lista_extras
                        )
                    )

                respuesta_final.append(
                    PedidoBaristaResponse(
                        pedido_id=p_id,
                        usuario_id=ped.get("usuario_id"),
                        estado=ped.get("estado"),
                        monto_total=float(ped.get("monto_total")),
                        productos=lista_productos
                    )
                )

            return respuesta_final

        except Exception as e:
            print(f"Error en service al obtener entregados: {str(e)}")
            raise e



    async def obtener_pedidos_cancelados(self) -> List[PedidoBaristaResponse]:
        """
        Consulta en Supabase únicamente los pedidos que ya fueron entregados 
        al alumno para el historial de la barra.
        """
        try:
            # 1. Filtramos estrictamente por el estado CANCELADO
            pedidos_query = self.db.table("pedidos")\
                .select("pedido_id, usuario_id, estado, monto_total")\
                .eq("estado", "CANCELADO")\
                .execute()

            lista_pedidos_bd = pedidos_query.data
            if not lista_pedidos_bd:
                return []

            respuesta_final = []

            # 2. Mismo mapeo relacional recursivo (Items -> Extras)
            for ped in lista_pedidos_bd:
                p_id = ped.get("pedido_id")

                items_query = self.db.table("pedido_items")\
                    .select("id, nombre_producto, cantidad")\
                    .eq("pedido_id", p_id)\
                    .execute()

                lista_productos = []

                for item in items_query.data:
                    item_id = item.get("id")

                    extras_query = self.db.table("pedido_item_extras")\
                        .select("nombre_extra")\
                        .eq("pedido_item_id", item_id)\
                        .execute()

                    lista_extras = [
                        ExtraResponse(nombre_extra=ext.get("nombre_extra")) 
                        for ext in extras_query.data
                    ]

                    lista_productos.append(
                        ItemPedidoResponse(
                            pedido_item_id=item_id,
                            nombre_producto=item.get("nombre_producto"),
                            cantidad=item.get("cantidad"),
                            extras=lista_extras
                        )
                    )

                respuesta_final.append(
                    PedidoBaristaResponse(
                        pedido_id=p_id,
                        usuario_id=ped.get("usuario_id"),
                        estado=ped.get("estado"),
                        monto_total=float(ped.get("monto_total")),
                        productos=lista_productos
                    )
                )

            return respuesta_final

        except Exception as e:
            print(f"Error en service al obtener entregados: {str(e)}")
            raise e


    
    async def cambiar_estado_pedido(self, pedido_id: str, nuevo_estado: str) -> bool:
        """
        Modifica el estado de un pedido específico en Supabase.
        Retorna True si la operación fue exitosa.
        """
        try:
            resultado = self.db.table("pedidos")\
                .update({"estado": nuevo_estado})\
                .eq("pedido_id", pedido_id)\
                .execute()
        
        # Si la actualización afectó filas, retornamos True
            return len(resultado.data) > 0
        except Exception as e:
            print(f"Error al actualizar estado en servicio: {str(e)}")
            return False