from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import supabase
from app.services.ai_service import AIService
from app.services.gamification_service import GamificationService
from app.services.auth_service import AuthService
from app.services.product_service import ProductoService
from app.services.pedido_service import PedidoService

from app.schemas.ai_schema import RecomendacionRequest, RecomendacionResponse
from app.schemas.gamification_schema import ProcesarCompraRequest, ProcesarCompraResponse
from app.schemas.register_schema import RegistroUsuarioResponse, RegistroUsuarioInput
from app.schemas.login_schema import LoginUsuarioInput, LoginUsuarioResponse
from app.schemas.product_schema import ProductoResponse
from app.schemas.pedido_schema import PedidoCreateRequest, PedidoResponse, PedidoStatusResponse
from typing import List



# services
ai_service = AIService()
gamification_service = GamificationService()
auth_service = AuthService()
producto_service = ProductoService()
pedido_service = PedidoService()

# 3. Inicializar la aplicación FastAPI
app = FastAPI(
    title="Cafeteria Universitaria API",
    description="Backend de utilidad para el control de gamificación e Inteligencia Artificial",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # 💡 Esto permite que tu celular físico pueda leer el JSON completo
    allow_credentials=True,
    allow_methods=["*"], # Permite GET, POST, etc.
    allow_headers=["*"],
)

# 4. Configurar CORS (Crucial para permitir conexiones externas)
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
        historial_dict = [item.model_dump() for item in payload.historial]
        productos_dict = [item.model_dump() for item in payload.productos_disponibles]
        
        resultado_ia = await ai_service.obtener_recomendacion_menu(
            historial_usuario=historial_dict,
            productos_disponibles=productos_dict
        )
    
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