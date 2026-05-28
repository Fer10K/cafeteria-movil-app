import os
import traceback
from typing import List
from fastapi import FastAPI, HTTPException, Request, status, WebSocket, WebSocketDisconnect, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

# Servicios actualizados
from app.services.ai_service import AIService
from app.services.gamification_service import GamificationService
from app.services.auth_service import AuthService
from app.services.product_service import ProductoService
from app.services.pedido_service import PedidoService
from app.services.get_pedidos_service import BaristaService
from app.services.websocket_manager import barista_manager
from app.supertopsecretutils import enviar_correo_pedido_listo

# Esquemas Pydantic
from app.schemas.ai_schema import RecomendacionRequest, RecomendacionResponse
from app.schemas.gamification_schema import ProcesarCompraRequest, ProcesarCompraResponse, PosicionLeaderboard
from app.schemas.register_schema import RegistroUsuarioResponse, RegistroUsuarioInput
from app.schemas.login_schema import LoginUsuarioInput, LoginUsuarioResponse
from app.schemas.product_schema import ProductoResponse
from app.schemas.pedido_schema import PedidoCreateRequest, PedidoResponse, PedidoStatusResponse
from app.schemas.get_pedidos_schema import PedidoBaristaResponse

# Instanciar servicios
ai_service = AIService()
gamification_service = GamificationService()
auth_service = AuthService()
producto_service = ProductoService()
pedido_service = PedidoService()
barista_service = BaristaService()

# Inicializar la aplicación FastAPI
app = FastAPI(
    title="Cafeteria Universitaria API",
    description="Backend optimizado para el control de gamificación, pedidos e Inteligencia Artificial",
    version="1.0.0"
)

# Middleware para capturar excepciones globales y enviarlas a Android Logcat
@app.middleware("http")
async def catch_exceptions_middleware(request: Request, call_next):
    try:
        return await call_next(request)
    except Exception as e:
        print("ERROR CRÍTICO EN EL BACKEND:")
        traceback.print_exc()
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={"detail": f"Error interno: {str(e)}", "traceback": traceback.format_exc()}
        )

# Configurar CORS para permitir conexiones externas (Parrot OS, Render, Emuladores)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==========================================
# ENDPOINTS: WEB SOCKET BARISTAS:
# ==========================================
@app.websocket("/ws/baristas")
async def websocket_baristas(websocket: WebSocket):
    await barista_manager.connect(websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        barista_manager.disconnect(websocket)



@app.get("/", tags=["Health"])
def read_root():
    return {
        "status": "online",
        "message": "Backend de la Cafeteria Universitaria operando con éxito",
        "environment_configured": os.getenv("DATABASE_URL") is not None
    }


# ==========================================
# ENDPOINTS: INTELIGENCIA ARTIFICIAL
# ==========================================
@app.post("/ai/recomendar", response_model=RecomendacionResponse, tags=["Inteligencia Artificial"])
async def generar_sugerencia_menu(payload: RecomendacionRequest):
    """
    Endpoint que recibe el historial del alumno y el inventario de la cafetería
    para devolver una recomendación personalizada generada por Gemini.
    """
    try:
        payload_dict = payload.model_dump()
        resultado_ia = await ai_service.obtener_recomendacion_contextual(payload_dict)
        return RecomendacionResponse(
            usuario_id=payload.usuario_id,
            recomendacion=resultado_ia
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error en el módulo de IA: {str(e)}")


# ==========================================
# ENDPOINTS: GAMIFICACIÓN
# ==========================================
@app.post("/gamificacion/procesar-compra", response_model=ProcesarCompraResponse, tags=["Gamificación"])
async def procesar_compra_estudiante(payload: ProcesarCompraRequest):
    """Calcula la XP ganada, subidas de nivel e impacta los logros tras una compra."""
    resultado = await gamification_service.procesar_transaccion(payload)
    return resultado


@app.get("/gamificacion/perfil/{usuario_id}", response_model=ProcesarCompraResponse, tags=["Gamificación"])
async def obtener_perfil_gamificacion_estudiante(usuario_id: str):
    """Consulta los puntos acumulados, nivel y medallas de un estudiante específico."""
    res = await gamification_service.obtener_perfile_individual(usuario_id)
    return res


@app.get("/gamificacion/leaderboard/{usuario_id}", response_model=List[PosicionLeaderboard], tags=["Gamificación"])
async def obtener_leaderboard(usuario_id: str):
    """Extrae el top de posiciones global omitiendo al alumno consultante."""
    res = await gamification_service.obtener_perfiles_general(usuario_id)
    return res


# ==========================================
# ENDPOINTS: AUTENTICACIÓN
# ==========================================
@app.post("/auth/registro", response_model=RegistroUsuarioResponse, tags=["Autenticación"])
async def registrar_estudiante(payload: RegistroUsuarioInput):
    """Registra nuevos alumnos directo en PostgreSQL encriptando la contraseña."""
    resultado = await auth_service.registrar_estudiante(payload)
    return resultado


@app.post("/auth/login", response_model=LoginUsuarioResponse, tags=["Autenticación"])
async def login_estudiante(payload: LoginUsuarioInput):
    """Valida credenciales SHA-256 de los estudiantes para el acceso móvil."""
    try:
        resultado = await auth_service.logear_estudiante(payload)
        return resultado
    except HTTPException as he:
        raise he


# ==========================================
# ENDPOINTS: CATÁLOGO DE PRODUCTOS
# ==========================================
@app.get("/productos", response_model=List[ProductoResponse], tags=["Catálogo"])
async def listar_productos():
    """Retorna el menú disponible estructurado con sus grupos de opciones y extras."""
    resultado = await producto_service.obtener_todos_los_productos()
    return resultado


# ==========================================
# ENDPOINTS: FLUJO DE PEDIDOS ALUMNOS
# ==========================================
@app.post("/pedidos", response_model=PedidoResponse, status_code=status.HTTP_201_CREATED, tags=["Pedidos"])
async def crear_pedido(payload: PedidoCreateRequest):
    """Recibe la orden del carrito, calcula totales y la guarda de forma atómica."""
    resultado = await pedido_service.procesar_pedido_real(payload)
    return resultado


@app.get("/pedidos/{pedido_id}/status", response_model=PedidoStatusResponse, tags=["Pedidos"])
async def obtener_estatus_pedido(pedido_id: str):
    """Endpoint de Polling para Android. Consulta el estado en tiempo real."""
    resultado = await pedido_service.verificar_estado_pedido(pedido_id)
    return resultado


@app.get("/pedidos/usuario/{usuario_id}/historial-nombres", response_model=List[str], tags=["Pedidos"])
async def obtener_pedidos_usuario(usuario_id: str):
    """Recupera una lista plana con los nombres de los últimos 10 productos comprados."""
    resultado = await pedido_service.obtener_historial_nombres_usuario(usuario_id)
    return resultado


# ==========================================
# ENDPOINTS: CONSOLA DEL BARISTA
# ==========================================
@app.get("/barista/pedidos", response_model=List[PedidoBaristaResponse], tags=["Barista"])
async def obtener_pedidos_barista():
    """Retorna las comandas activas con estados 'PROCESANDO' o 'LISTO'."""
    return await barista_service.obtener_comandas_activas()


@app.patch("/barista/pedidos/{pedido_id}/estado", tags=["Barista"])
async def actualizar_estado_pedido(pedido_id: str, nuevo_estado: str, background_tasks: BackgroundTasks):
    """Modifica el estado de avance de una orden de la barra."""

    if nuevo_estado == "LISTO":
        background_tasks.add_task(enviar_correo_pedido_listo, pedido_id)

    exito = await barista_service.cambiar_estado_pedido(pedido_id, nuevo_estado)

    if not exito:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No se encontró el pedido o no se pudo actualizar."
        )
    return {"status": "success", "message": f"Pedido actualizado a {nuevo_estado} con éxito."}


@app.get("/barista/pedidos/entregados", response_model=List[PedidoBaristaResponse], tags=["Barista"])
async def obtener_historial_entregados():
    """Historial de órdenes entregadas con éxito."""
    return await barista_service.obtener_pedidos_entregados()


@app.get("/barista/pedidos/cancelados", response_model=List[PedidoBaristaResponse], tags=["Barista"])
async def obtener_historial_cancelados():
    """Historial de órdenes canceladas."""
    return await barista_service.obtener_pedidos_cancelados()


@app.get("/barista/pedidos/porconfirmar", response_model=List[PedidoBaristaResponse], tags=["Barista"])
async def obtener_historial_porconfirmar():
    """Historial de órdenes pendientes de confirmación de pago."""
    return await barista_service.obtener_pedidos_porconfirmar()