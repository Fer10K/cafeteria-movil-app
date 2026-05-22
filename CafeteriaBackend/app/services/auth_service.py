from fastapi import APIRouter, HTTPException, status
from supabase import create_client, Client
from app.schemas import RegistroUsuarioResponse, RegistroUsuarioInput
import os

router = APIRouter(prefix="/auth", tags=["Autenticación"])

# Inicialización del cliente de Supabase
supabase: Client = create_client(
    os.getenv("SUPABASE_URL", ""),
    os.getenv("SUPABASE_KEY", "")
)

@router.post("/registro", response_model=RegistroUsuarioResponse, status_code=status.HTTP_201_CREATED)
async def registrar_estudiante(datos: RegistroUsuarioInput):
    try:
        print("Crear el usuario en Supabase Auth")
        auth_response = supabase.auth.sign_up({
            "email": datos.correo,
            "password": datos.password
        })
        
        print("Validamos que se haya generado el usuario en Auth")
        if not auth_response.user:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST, 
                detail="No se pudo crear las credenciales de autenticación."
            )
            
        uuid_oficial = auth_response.user.id

        print("Insertar los datos del alumno en tu tabla de usuarios públicos usando el UUID oficial")
        supabase.table("usuarios").insert({
            "id": uuid_oficial,
            "nombre": datos.nombre_completo,
            "correo": datos.correo
        }).execute()

        print("# Inicializarsu perfil de gamificación")
        supabase.table("perfiles_gamificacion").insert({
            "usuario_id": uuid_oficial,
            "xp_actual": 0,
            "nivel_actual": 1
        }).execute()

        return RegistroUsuarioResponse(
            usuario_id=uuid_oficial,
            mensaje="Estudiante registrado con éxito en Auth y Base de Datos Pública."
        )

    except Exception as e:
        # Si algo falla, es buena práctica registrar el error
        print(f"❌ Error en el proceso de registro: {e}", flush=True)
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Error al registrar: {str(e)}"
        )