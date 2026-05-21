from typing import List, Dict, Any
from supabase import Client
from app.main import supabase
from app.schemas.gamification_schema import ProcesarCompraRequest, LogroDesbloqueadoSchema

class GamificationService:
    def __init__(self):
        self.db: Client = supabase

    def _calcular_xp_ganada(self, monto: float) -> int:
        """Regla de utilidad: 1 punto de XP por cada peso gastado (redondeado hacia abajo)."""
        return int(monto)

    async def procesar_transaccion(self, datos: ProcesarCompraRequest) -> Dict[str, Any]:
        """
        Calcula la nueva XP, valida si el usuario sube de nivel y verifica
        si desbloqueó logros en Supabase.
        """

        usuario_id = datos.usuario_id
        xp_ganada = self._calcular_xp_ganada(datos.monto_total)
        subio_de_nivel = False
        logros_nuevos: List[LogroDesbloqueadoSchema] = []

        # Consultar el estado
        try:
            res_perfil = self.db.table("perfiles_gamificacion").select("xp_total, nivel").eq("usuario_id", usuario_id).single().execute()
            xp_actual = res_perfil.data["xp_total"]
            nivel_actual = res_perfil.data["nivel"]
        except Exception as e:
            if "0 rows" in str(e) or "PGRST116" in str(e):
                nuevo_perfil = {
                    "usuario_id": usuario_id,
                    "xp_total": 0,
                    "nivel": 1
                }
                self.db.table("perfiles_gamificacion").insert(nuevo_perfil).execute()
                xp_actual = 0
                nivel_actual = 1
            else:
                raise e
        
        if not res_perfil.data:
            raise Exception("Perfil de usuario no encontrado en la base de datos.")

        xp_actual = res_perfil.data["xp_total"]
        nivel_actual = res_perfil.data["nivel"]

        # Sumar la nueva XP
        xp_final = xp_actual + xp_ganada

        # Mientras la XP final supere el umbral del nivel actual, incrementa el nivel
        while True:
            xp_necesaria_siguiente = nivel_actual * 500
            if xp_final >= xp_necesaria_siguiente:
                nivel_actual += 1
                subio_de_nivel = True
            else:
                break

        # 4. Actualizar los nuevos valores del alumno en Supabase
        self.db.table("perfiles_gamificacion").update({
            "xp_total": xp_final,
            "nivel": nivel_actual
        }).eq("usuario_id", usuario_id).execute()

        # 5. Sistema de Logros de utilidad (Simulación/Verificación rápida)
        # Aquí puedes consultar tu tabla 'logros' y meter lógica condicional.
        # Por ejemplo, si es su primera compra del día, otorgamos una medalla:
        if datos.es_primer_compra_dia:
            res_logro = self.db.table("logros").select("*").eq("codigo", "primer_cafe").single().execute()
            if res_logro.data:
                # Registramos que el usuario ahora tiene este logro
                self.db.table("usuario_logros").insert({
                    "usuario_id": usuario_id,
                    "logro_id": res_logro.data["id"]
                }).execute()
                
                logros_nuevos.append(
                    LogroDesbloqueadoSchema(
                        id=res_logro.data["id"],
                        nombre=res_logro.data["nombre"],
                        descripcion=res_logro.data["descripcion"],
                        icono_url=res_logro.data.get("icono_url")
                    )
                )

        # Retornamos un diccionario que calce con nuestro ProcesarCompraResponse
        return {
            "usuario_id": usuario_id,
            "xp_ganada": xp_ganada,
            "xp_actual": xp_final,
            "nivel_actual": nivel_actual,
            "subio_de_nivel": subio_de_nivel,
            "logros_nuevos": logros_nuevos
        }