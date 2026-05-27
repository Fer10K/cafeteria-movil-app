import os
import psycopg2
from psycopg2.extras import RealDictCursor
from typing import List, Dict, Any
from fastapi import HTTPException, status
from app.schemas.gamification_schema import ProcesarCompraRequest, LogroDesbloqueadoSchema

class GamificationService:
    def __init__(self):
        # Ya no requerimos inicializar la variable interna de Supabase
        pass

    def _calcular_xp_ganada(self, monto: float) -> int:
        """Regla de utilidad: 1 punto de XP por cada peso gastado (redondeado hacia abajo)."""
        return int(monto)

    async def procesar_transaccion(self, datos: ProcesarCompraRequest) -> Dict[str, Any]:
        """
        Calcula la nueva XP, valida si el usuario sube de nivel y verifica
        si desbloqueó logros en PostgreSQL de Render.
        """
        usuario_id = datos.usuario_id
        xp_ganada = self._calcular_xp_ganada(datos.monto_total)
        subio_de_nivel = False
        logros_nuevos: List[LogroDesbloqueadoSchema] = []

        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            # 1. Consultar el perfil actual del alumno
            query_perfil = """
                SELECT xp_total, nivel 
                FROM public.perfiles_gamificacion 
                WHERE usuario_id = %s;
            """
            cursor.execute(query_perfil, (usuario_id,))
            perfil = cursor.fetchone()

            # Si por alguna razón extraña no tiene perfil, se lo creamos en caliente
            if not perfil:
                query_insert_perfil = """
                    INSERT INTO public.perfiles_gamificacion (usuario_id, xp_total, nivel)
                    VALUES (%s, 0, 1)
                    RETURNING xp_total, nivel;
                """
                cursor.execute(query_insert_perfil, (usuario_id,))
                perfil = cursor.fetchone()

            xp_actual = perfil["xp_total"]
            nivel_actual = perfil["nivel"]

            # 2. Calcular la XP final y procesar subida de niveles
            xp_final = xp_actual + xp_ganada

            while True:
                xp_necesaria_siguiente = nivel_actual * 500
                if xp_final >= xp_necesaria_siguiente:
                    nivel_actual += 1
                    subio_de_nivel = True
                else:
                    break

            # 3. Actualizar los nuevos valores del alumno
            query_update = """
                UPDATE public.perfiles_gamificacion 
                SET xp_total = %s, nivel = %s 
                WHERE usuario_id = %s;
            """
            cursor.execute(query_update, (xp_final, nivel_actual, usuario_id))

            # 4. Sistema de Logros (Si es su primera compra del día)
            if datos.es_primer_compra_dia:
                query_logro = "SELECT id, nombre, descripcion, icono_url FROM public.logros WHERE codigo = %s;"
                cursor.execute(query_logro, ("primer_cafe",))
                logro_maestro = cursor.fetchone()

                if logro_maestro:
                    # Verificar primero que no se le haya otorgado ya (evitar duplicados por integridad)
                    query_check_logro = "SELECT 1 FROM public.usuario_logros WHERE usuario_id = %s AND logro_id = %s;"
                    cursor.execute(query_check_logro, (usuario_id, logro_maestro["id"]))
                    ya_lo_tiene = cursor.fetchone()

                    if not ya_lo_tiene:
                        query_otorgar_logro = """
                            INSERT INTO public.usuario_logros (usuario_id, logro_id)
                            VALUES (%s, %s);
                        """
                        cursor.execute(query_otorgar_logro, (usuario_id, logro_maestro["id"]))

                        logros_nuevos.append(
                            LogroDesbloqueadoSchema(
                                id=str(logro_maestro["id"]),
                                nombre=logro_maestro["nombre"],
                                descripcion=logro_maestro["descripcion"],
                                icono_url=logro_maestro.get("icono_url")
                            )
                        )

            # Confirmamos todas las operaciones atómicamente
            conn.commit()
            cursor.close()

            return {
                "usuario_id": usuario_id,
                "xp_ganada": xp_ganada,
                "xp_actual": xp_final,
                "nivel_actual": nivel_actual,
                "subio_de_nivel": subio_de_nivel,
                "logros_nuevos": logros_nuevos
            }

        except Exception as e:
            if conn: conn.rollback()
            print(f"Error en procesar_transaccion gamificación: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Error al procesar la gamificación: {str(e)}"
            )
        finally:
            if conn: conn.close()

    async def obtener_perfile_individual(self, usuario_id: str) -> dict:
        """Obtiene los niveles, XP y lista de logros desbloqueados de un alumno específico."""
        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            # 1. Traer datos de gamificación básica
            query_perfil = "SELECT xp_total, nivel FROM public.perfiles_gamificacion WHERE usuario_id = %s;"
            cursor.execute(query_perfil, (usuario_id,))
            perfil = cursor.fetchone()

            xp_total = perfil["xp_total"] if perfil else 0
            nivel = perfil["nivel"] if perfil else 1

            # 2. Traer los logros vinculados haciendo un INNER JOIN limpio
            query_logros = """
                SELECT l.id, l.nombre, l.descripcion, l.icono_url 
                FROM public.usuario_logros ul
                INNER JOIN public.logros l ON ul.logro_id = l.id
                WHERE ul.usuario_id = %s;
            """
            cursor.execute(query_logros, (usuario_id,))
            logros_data = cursor.fetchall()

            cursor.close()

            logros_list = []
            for logro in logros_data:
                logros_list.append({
                    "id": str(logro["id"]),
                    "nombre": logro["nombre"],
                    "descripcion": logro["descripcion"],
                    "icono_url": logro.get("icono_url")
                })

            return {
                "usuario_id": usuario_id,
                "xp_ganada": 0,
                "xp_actual": xp_total,
                "nivel_actual": nivel,
                "subio_de_nivel": False,
                "logros_nuevos": logros_list
            }

        except Exception as e:
            print(f"Error al obtener perfil individual: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error al obtener los datos de perfil."
            )
        finally:
            if conn: conn.close()

    async def obtener_perfiles_general(self, usuario_id: str) -> list:
        """Retorna el Leaderboard global ordenado por XP, excluyendo al usuario actual."""
        conn = None
        try:
            conn = psycopg2.connect(os.getenv("DATABASE_URL"))
            cursor = conn.cursor(cursor_factory=RealDictCursor)

            # Hacemos un JOIN clásico para extraer el nombre de la tabla usuarios de forma directa
            query_leaderboard = """
                SELECT pg.usuario_id, pg.xp_total, pg.nivel, pg.avatar_url, u.nombre
                FROM public.perfiles_gamificacion pg
                INNER JOIN public.usuarios u ON pg.usuario_id = u.id
                WHERE pg.usuario_id != %s
                ORDER BY pg.xp_total DESC;
            """
            cursor.execute(query_leaderboard, (usuario_id,))
            leaderboard_completo = cursor.fetchall()

            cursor.close()

            lista_filtrada = []
            for perfil in leaderboard_completo:
                lista_filtrada.append({
                    "usuario_id": str(perfil["usuario_id"]),
                    "nombre": perfil["nombre"] or "Alumno Anónimo",
                    "xp_total": perfil["xp_total"],
                    "nivel": perfil["nivel"],                        
                    "avatar_url": perfil["avatar_url"] or "https://vuestrasubase.supabase.co/storage/v1/object/public/avatares/default.png"
                })
                
            return lista_filtrada

        except Exception as e:
            print(f"Error al generar leaderboard general: {str(e)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error al cargar la tabla de posiciones."
            )
        finally:
            if conn: conn.close()