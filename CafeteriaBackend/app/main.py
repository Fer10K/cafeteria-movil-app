from fastapi import FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import traceback

from app.config import supabase
from app.services.ai_service import AIService
from app.services.gamification_service import GamificationService
from app.services.auth_service import AuthService
from app.services.product_service import ProductoService
from app.services.pedido_service import PedidoService
from app.services.get_pedidos_service import BaristaService


from app.schemas.ai_schema import RecomendacionRequest, RecomendacionResponse
from app.schemas.gamification_schema import ProcesarCompraRequest, ProcesarCompraResponse, PosicionLeaderboard
from app.schemas.register_schema import RegistroUsuarioResponse, RegistroUsuarioInput
from app.schemas.login_schema import LoginUsuarioInput, LoginUsuarioResponse
from app.schemas.product_schema import ProductoResponse
from app.schemas.pedido_schema import PedidoCreateRequest, PedidoResponse, PedidoStatusResponse
from app.schemas.get_pedidos_schema import PedidoBaristaResponse
from typing import List



# services
ai_service = AIService()
gamification_service = GamificationService()
auth_service = AuthService()
producto_service = ProductoService()
pedido_service = PedidoService()
barista_service = BaristaService()

# 3. Inicializar la aplicación FastAPI
app = FastAPI(
    title="Cafeteria Universitaria API",
    description="Backend de utilidad para el control de gamificación e Inteligencia Artificial",
    version="1.0.0"
)

@app.middleware("http")
async def catch_exceptions_middleware(request: Request, call_next):
    try:
        return await call_next(request)
    except Exception as e:
        # Esto imprimirá el error con lujo de detalle en la consola de Docker
        print("💥 ERROR CRÍTICO EN EL BACKEND:")
        traceback.print_exc()
        
        # Y esto te mandará el verdadero error a Android para que lo veas en el Logcat
        return JSONResponse(
            status_code=500,
            content={"detail": f"Error interno: {str(e)}", "traceback": traceback.format_exc()}
        )

#  Configurar CORS (Crucial para permitir conexiones externas)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 5. Endpoint Health Check
@app.get("/", tags =["Health"])
def read_root():
    return {
        "status": "online",
        "message": "Backend de la Cafeteria Universitario operando con éxito",
        "database_connected": supabase is not None
    }

@app.post("/ai/recomendar", response_model=RecomendacionResponse, tags=["Inteligencia Artificial"])
async def generar_sugerencia_menu(payload: RecomendacionRequest):
    """
    Endpoint que recibe el historial del alumno y el inventario de la cafetería
    para devolver una recomendación personalizada generada por Gemini 1.5 Flash.
    """
    try:
        payload_dict = payload.model_dump()
        
        resultado_ia = await ai_service.obtener_recomendacion_contextual(payload_dict)
        
        return RecomendacionResponse(
            usuario_id=payload.usuario_id,
            recomendacion=resultado_ia
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error interno en el módulo de IA: {str(e)}")


@app.post("/gamificacion/procesar-compra", response_model=ProcesarCompraResponse, tags=["Gamificación"])
async def procesar_compra_estudiante(payload: ProcesarCompraRequest):
    """
    Endpoint que se ejecuta tras una venta. Calcula la XP ganada,
    actualiza los niveles y valida si se desbloquearon logros en Supabase.
    """
    try:
        resultado = await gamification_service.procesar_transaccion(payload)
        return resultado
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


#Endpoint para el registro
@app.post("/auth/registro", response_model=RegistroUsuarioResponse, tags=["Autenticación"])
async def registrar_estudiante(payload: RegistroUsuarioInput):
    """
    Endpoint para el registro de nuevos alumnos. Crea las credenciales en 
    Supabase Auth, genera el perfil público e inicializa su gamificación.
    """
    try:
        resultado = await auth_service.registrar_estudiante(payload)
        return resultado
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


#Endpoit logear usuario
@app.post("/auth/login", response_model=LoginUsuarioResponse, tags=["Autenticación"])
async def login_estudiante(payload: LoginUsuarioInput):
    """
    Endpoint para que los alumnos inicien sesión. Valida las credenciales 
    y retorna el UUID junto con los datos públicos del estudiante.
    """
    try:
        resultado = await auth_service.logear_estudiante(payload)
        return resultado
    except Exception as e:
        raise HTTPException(
            status_code=401,
            detail=str(e)
        )

#Obtener productos
@app.get("/productos", response_model=List[ProductoResponse], tags=["Catálogo"])
def listar_productos():
    """
    Retorna todos los productos disponibles en la cafetería 
    junto con el nombre de su categoría correspondiente.
    """
    try:

        return producto_service.obtener_todos_los_productos()
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/pedidos", response_model=PedidoResponse, status_code=status.HTTP_201_CREATED, tags=["Pedidos"])
async def crear_pedido(payload: PedidoCreateRequest):
    """
    Endpoint final conectado a Supabase. Recibe el payload nativo del carrito
    desde Android, impacta la base de datos y confirma la transacción.
    """
    try:
        resultado = await pedido_service.procesar_pedido_real(payload)
        return resultado
    except Exception as e:
        raise HTTPException(
            status_code=400, 
            detail=f"Error al procesar/insertar el pedido: {str(e)}"
        )


@app.get("/pedidos/{pedido_id}/status", response_model=PedidoStatusResponse, tags=["Pedidos"])
async def obtener_estatus_pedido(pedido_id: str):
    """
    Endpoint de Polling para Android. Consulta cada X segundos el estado
    actual del pedido en Supabase para verificar si el barista ya confirmó el pago.
    """
    try:
        resultado = await pedido_service.verificar_estado_pedido(pedido_id)
        return resultado
    except Exception as e:
        raise HTTPException(
            status_code=404, 
            detail=f"Error al consultar el estado del pedido: {str(e)}"
        )


@app.get("/gamificacion/perfil/{usuario_id}", response_model=ProcesarCompraResponse, tags=["Gamificación"])
async def obtener_perfil_gamificacion_estudiante(usuario_id: str):
    """
    Endpoint autónomo que consulta Supabase para extraer los puntos acumulados,
    el nivel de un estudiante específico.
    """
    try:
        res = await gamification_service.obtener_perfile_individual(usuario_id)
        return res
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Error al leer Supabase: {str(e)}")
    


@app.get("/gamificacion/leaderboard/{usuario_id}", response_model=list[PosicionLeaderboard])
async def obtener_leaderboard(usuario_id: str):
    """
    Endpoint autónomo que consulta Supabase para extraer los puntos acumulados,
    el nivel y de todos los usuarios exepto el especifico
    """
    try:
        res = await gamification_service.obtener_perfiles_general(usuario_id)
        
        return res

    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={"detail": f"Error interno: {str(e)}", "traceback": traceback.format_exc()}
        )


@app.get("/pedidos/usuario/{usuario_id}/historial-nombres", response_model=List[str], tags=["Pedidos"])
async def obtener_pedidos_usuario(usuario_id: str):
    """
    Obtiene los IDs de la tabla 'pedidos' filtrando por usuario_id, 
    busca los nombres en 'pedido_items' y regresa una lista plana de strings.
    """
    try:
        # 1. Obtener los pedido_id del usuario desde la tabla 'pedidos'
        res_pedidos = supabase.table("pedidos") \
            .select("pedido_id") \
            .eq("usuario_id", usuario_id) \
            .execute()
        
        datos_pedidos = res_pedidos.data if res_pedidos.data else []
        if not datos_pedidos:
            return []
            
        # Extraemos todos los UUIDs de los pedidos en una lista nativa de Python
        lista_pedido_ids = [p["pedido_id"] for p in datos_pedidos]
        
        # 2. Consultar 'pedido_items' para traer los nombres vinculados a esos IDs
        res_items = supabase.table("pedido_items") \
            .select("nombre_producto") \
            .in_("pedido_id", lista_pedido_ids) \
            .execute()
            
        datos_items = res_items.data if res_items.data else []
        
        # 3. Filtrar y limpiar para regresar únicamente la lista de nombres (máximo 10)
        nombres_productos = [item["nombre_producto"] for item in datos_items if item.get("nombre_producto")]
        
        # Retornamos los últimos 10 de forma simplificada
        return nombres_productos[:10]
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error al procesar el historial relacional de pedidos: {str(e)}"
        )



#=========================================================================================

@app.get("/barista/pedidos", response_model=List[PedidoBaristaResponse], status_code=status.HTTP_200_OK,summary="Obtener comandas activas para el Barista",description="Retorna una lista con todos los pedidos cuyo estado sea 'PROCESANDO' (en preparación) o 'LISTO' (esperando entrega) incluyendo el desglose de productos y extras.")
async def obtener_pedidos_barista():
    try:
        return await barista_service.obtener_comandas_activas()
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error interno en el servidor al procesar la solicitud: {str(e)}"
        )


@app.patch("/barista/pedidos/{pedido_id}/estado",status_code=status.HTTP_200_OK,summary="Actualizar el estado de una comanda",description="Cambia el estado de un pedido (ej. de PROCESANDO a LISTO).")
async def actualizar_estado_pedido(pedido_id: str, nuevo_estado: str):
    try:
        exito = await barista_service.cambiar_estado_pedido(pedido_id, nuevo_estado)
        
        if not exito:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="No se encontró el pedido o no se pudo actualizar."
            )
            
        return {"status": "success", "message": f"Pedido actualizado a {nuevo_estado} con éxito."}
        
    except HTTPException as he:
        raise he
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error en el servidor: {str(e)}"
        )

@app.get("/barista/pedidos/entregados", response_model=List[PedidoBaristaResponse],status_code=status.HTTP_200_OK,summary="Obtener historial de pedidos entregados",description="Retorna una lista con el desglose de todas las comandas que ya fueron finalizadas y entregadas con éxito.")
async def obtener_historial_entregados():
    try:
        return await barista_service.obtener_pedidos_entregados()
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error al recuperar el historial de entregas: {str(e)}"
        )

@app.get("/barista/pedidos/cancelados", response_model=List[PedidoBaristaResponse],status_code=status.HTTP_200_OK,summary="Obtener historial de pedidos entregados",description="Retorna una lista con el desglose de todas las comandas que ya fueron finalizadas y entregadas con éxito.")
async def obtener_historial_cancelados():
    try:
        return await barista_service.obtener_pedidos_cancelados()
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error al recuperar el historial de entregas: {str(e)}"
        )