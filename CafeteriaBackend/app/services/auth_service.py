import os
import hashlib
import psycopg2
from psycopg2.extras import RealDictCursor
from fastapi import HTTPException, status
from app.schemas.register_schema import RegistroUsuarioResponse, RegistroUsuarioInput
from app.schemas.login_schema import LoginUsuarioInput, LoginUsuarioResponse


class AuthService:
    def __init__(self):
        pass

    async def logear_estudiante(self, datos: LoginUsuarioInput) -> LoginUsuarioResponse:
        conn = None
        try:
            print(f"Intentando validar credenciales para el correo: {datos.correo}")
            
            # 1. Encriptar la contraseña recibida para poder compararla con el hash de la BD
            password_encriptada = hashlib.sha256(datos.password.encode()).hexdigest()
            
            # 2. Conectarse a PostgreSQL en Render
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # 3. Consultar si existe el usuario con esa combinación exacta de correo y contraseña
            query = """
                SELECT id, nombre, correo, rol 
                FROM public.usuarios 
                WHERE correo = %s AND contrasena = %s;
            """
            cursor.execute(query, (datos.correo, password_encriptada))
            usuario = cursor.fetchone()
            
            cursor.close()
            
            # 4. Validar si se encontró o no el registro
            if not usuario:
                print("Credenciales inválidas o usuario no encontrado.")
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Correo o contraseña incorrectos."
                )
                
            uuid_confirmado = str(usuario['id'])
            nombre_usuario = usuario['nombre']
            rol_usuario = usuario['rol']
            
            print(f"Login exitoso. Usuario: {nombre_usuario} [{rol_usuario}]")
            
            # 5. Retornar la respuesta estructurada tal como la espera tu LoginUsuarioResponse
            return LoginUsuarioResponse(
                usuario_id=uuid_confirmado,
                nombre_completo=nombre_usuario,
                correo=datos.correo,
                mensaje="Inicio de sesión exitoso.",
                rol=rol_usuario
            )

        except HTTPException as he:
            # Re-lanzar las excepciones controladas de FastAPI (como el 401)
            raise he
        except Exception as e:
            print(f"Error crítico en el proceso de login: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Error interno del servidor al procesar el inicio de sesión: {str(e)}"
            )
        finally:
            if conn:
                conn.close()

    async def registrar_estudiante(self, datos: RegistroUsuarioInput) -> RegistroUsuarioResponse:
        conn = None
        try:
            print("Iniciando registro directo en PostgreSQL de Render")
            
            # 1. Encriptar la contraseña antes de guardarla por seguridad
            password_encriptada = hashlib.sha256(datos.password.encode()).hexdigest()
            
            # 2. Establecer conexión con Render
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)
            
            # 3. Insertar el usuario. La BD genera el UUID y el rol 'cliente' por defecto
            query_usuario = """
                INSERT INTO public.usuarios (nombre, correo, contrasena)
                VALUES (%s, %s, %s)
                RETURNING id;
            """
            cursor.execute(query_usuario, (datos.nombre_completo, datos.correo, password_encriptada))
            resultado_usuario = cursor.fetchone()
            
            if not resultado_usuario:
                raise Exception("La base de datos no retornó el ID del usuario creado.")
                
            uuid_oficial = str(resultado_usuario['id'])
            print(f"Usuario creado con éxito en Render. UUID: {uuid_oficial}")
            
            # 4. Inicializar su perfil de gamificación (Obligatorio por la llave foránea)
            print("Inicializando su perfil de gamificación...")
            query_gamificacion = """
                INSERT INTO public.perfiles_gamificacion (usuario_id, xp_total, nivel)
                VALUES (%s, 0, 1);
            """
            cursor.execute(query_gamificacion, (uuid_oficial,))
            
            # 5. Confirmar la transacción completa en la base de datos
            conn.commit()
            cursor.close()
            
            # 6. Retornamos la respuesta exacta que tu RegistroUsuarioResponse espera
            return RegistroUsuarioResponse(
                usuario_id=uuid_oficial,
                mensaje="Estudiante registrado con éxito en la Base de Datos de Render."
            )

        except psycopg2.IntegrityError as e:
            if conn: conn.rollback()
            print(f"Error de integridad (posible correo duplicado): {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="El correo electrónico ya se encuentra registrado."
            )
        except Exception as e:
            if conn: conn.rollback()
            print(f"Error en el proceso de registro: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Error interno al registrar: {str(e)}"
            )
        finally:
            if conn:
                conn.close()