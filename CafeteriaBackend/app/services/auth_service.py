from fastapi import HTTPException, status
from supabase import Client
from app.main import supabase
from app.schemas.register_schema import RegistroUsuarioResponse, RegistroUsuarioInput
from app.schemas.login_schema import LoginUsuarioInput, LoginUsuarioResponse


class AuthService:
    def __init__(self):
        self.db: Client = supabase

    async def logear_estudiante(self, datos: LoginUsuarioInput) -> LoginUsuarioResponse:
        try:
            print("Validar las credenciales contra Supabase Auth")
            auth_response = self.db.auth.sign_in_with_password({
                "email": datos.correo,
                "password": datos.password
            })
            
            if not auth_response.user:
                raise Exception("Credenciales incorrectas o usuario no encontrado.")
                
            uuid_confirmado = auth_response.user.id

            print("Buscar el nombre completo en la tabla")
            resultado_perfil = self.db.table("usuarios")\
                .select("nombre, rol")\
                .eq("id", uuid_confirmado)\
                .single()\
                .execute()

            nombre_usuario = resultado_perfil.data.get("nombre", "Estudiante")
            rol=resultado_perfil.data.get("rol")

            print(rol)

            return LoginUsuarioResponse(
                usuario_id=uuid_confirmado,
                nombre_completo=nombre_usuario,
                correo=datos.correo,
                mensaje="Inicio de sesión exitoso.",
                rol=rol
            )

        except Exception as e:
            raise Exception(f"Correo o contraseña incorrectos: {e}")
        

    async def registrar_estudiante(self, datos: RegistroUsuarioInput) -> RegistroUsuarioResponse:
        try:
            print("Crear el usuario en Supabase Auth")
            auth_response = supabase.auth.admin.create_user({
            "email": datos.correo,
            "password": datos.password,
            "email_confirm": True,
            "user_metadata": {"nombre": datos.nombre_completo}
        })

            print("Validamos que se haya generado el usuario en Auth")
            if not auth_response.user:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="No se pudo crear las credenciales de autenticación.",
                )

            uuid_oficial = auth_response.user.id
            print(f"✅ Usuario creado en Auth con UUID: {uuid_oficial}")
            print("Insertar los datos del alumno en tu tabla de usuarios públicos")

            print(
                "Insertar los datos del alumno en tu tabla de usuarios públicos usando el UUID oficial"
            )

            # Ahora la inserción manual no fallará jamás por la clave foránea
            supabase.table("usuarios").upsert({
            "id": uuid_oficial,
            "nombre": datos.nombre_completo,
            "correo": datos.correo
            }).execute()
            

            print("Inicializarsu perfil de gamificación")
            supabase.table("perfiles_gamificacion").upsert(
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