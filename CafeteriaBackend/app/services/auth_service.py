from fastapi import HTTPException, status
from supabase import Client
from app.main import supabase
from app.schemas.register_schema import RegistroUsuarioResponse, RegistroUsuarioInput


class RegisterService:
    def __init__(self):
        self.db: Client = supabase

    async def registrar_estudiante(self, datos: RegistroUsuarioInput) -> RegistroUsuarioResponse:
        try:
            print("Crear el usuario en Supabase Auth")
            auth_response = self.db.auth.sign_up(
                {"email": datos.correo, "password": datos.password}
            )

            print("Validamos que se haya generado el usuario en Auth")
            if not auth_response.user:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="No se pudo crear las credenciales de autenticación.",
                )

            uuid_oficial = auth_response.user.id

            print(
                "Insertar los datos del alumno en tu tabla de usuarios públicos usando el UUID oficial"
            )
            supabase.table("usuarios").upsert(
                {
                    "id": uuid_oficial,
                    "nombre": datos.nombre_completo,
                    "correo": datos.correo,
                }
            ).execute()

            print("Inicializarsu perfil de gamificación")
            supabase.table("perfiles_gamificacion").insert(
                {"usuario_id": uuid_oficial, "xp_total": 0, "nivel": 1}
            ).execute()

            return RegistroUsuarioResponse(
                usuario_id=uuid_oficial,
                mensaje="Estudiante registrado con éxito en Auth y Base de Datos Pública.",
            )

        except Exception as e:
            print(f"Error en el proceso de registro: {e}", flush=True)
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Error al registrar: {str(e)}",
        )