from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.config import supabase
from app.services.ai_service import AIService
from app.services.gamification_service import GamificationService
from app.services.auth_service import RegisterService
from app.schemas.ai_schema import RecomendacionRequest, RecomendacionResponse
from app.schemas.gamification_schema import ProcesarCompraRequest, ProcesarCompraResponse
from app.schemas.register_schema import RegistroUsuarioResponse, RegistroUsuarioInput


# services
ai_service = AIService()
gamification_service = GamificationService()
auth_service = RegisterService()

# 3. Inicializar la aplicación FastAPI
app = FastAPI(
    title="Cafeteria Universitaria API",
    description="Backend de utilidad para el control de gamificación e Inteligencia Artificial",
    version="1.0.0"
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