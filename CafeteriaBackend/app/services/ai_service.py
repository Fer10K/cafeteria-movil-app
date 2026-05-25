import google.generativeai as genai
from app.config import settings

class AIService:
    def __init__(self):
        genai.configure(api_key=settings.gemini_api_key)
        self.model = genai.GenerativeModel("gemini-2.5-flash")

    async def obtener_recomendacion_contextual(self, payload: dict) -> str:
        contexto = payload.get("tipo_contexto", "inicio")
        historial = payload.get("historial", [])
        menu = payload.get("productos_disponibles", [])
        
        # Base de instrucciones comunes
        prompt_base = (
            "Eres el asistente inteligente de la cafetería universitaria.\n"
            "Instrucciones estrictas:\n"
            "1. Tu respuesta debe ser muy corta, puedes insertar una curiosidad o frace graciosa sobre el producto de sugerencia (máximo 1 o 2 líneas).\n"
            "2. Usa un tono juvenil, universitario y motivador.\n"
            "3. No inventes productos que no estén explícitamente en la lista de disponibles.\n\n"
        )
        
        # CASO 1: PANTALLA DE INICIO (Historial habitual)
        if contexto == "inicio":
            prompt = prompt_base + (
                f"Contexto: El alumno abrió la app. Analiza sus últimas compras: {historial}.\n"
                f"Sugiérele algo que le guste o una alternativa basada en el menú de hoy: {menu}."
            )
            
        # CASO 2: PANTALLA DEL CARRITO (Venta cruzada / Maridaje)
        elif contexto == "carrito":
            prompt = prompt_base + (
                f"Contexto: El alumno está en la caja/carrito. Actualmente lleva estos productos en su cesta: {historial}.\n"
                f"Sugiérele un único producto adicional del menú de hoy: {menu} que combine perfectamente con lo que ya lleva "
                "(ej. si lleva comida, sugiere bebida; si lleva café, una repostería), incentivando a cerrar la compra."
            )
            
        #CASO 3: PANTALLA DE GAMIFICACIÓN (Estrategia de Competencia)
        elif contexto == "gamificacion":
            xp_actual = payload.get("puntos_usuario_actual", 0)
            xp_siguiente = payload.get("puntos_siguiente_usuario", 0)
            rival = payload.get("nombre_siguiente_usuario", "el líder")
            
            puntos_necesarios = max(0, xp_siguiente - xp_actual)
            
            prompt = prompt_base + (
                f"Contexto: Pantalla de niveles. El alumno tiene {xp_actual} XP. El usuario de arriba es {rival} con {xp_siguiente} XP.\n"
                f"Le faltan exactamente {puntos_necesarios} XP para alcanzarlo.\n"
                f"Mapeando el menú disponible: {menu}, recomiéndale un producto económico o intermedio de la lista que le otorgue "
                f"puntos para acortar camino. Ojo: Si la diferencia de puntos es gigante, no le exijas gastar demasiado dinero, "
                f"en su lugar anímalo a pedir algo casual para seguir sumando racha."
            )
        else:
            prompt = prompt_base + f"Recomienda algo casual del menú: {menu}"

        try:
            response = await self.model.generate_content_async(prompt)
            return response.text.strip()
        except Exception as e:
            print(f"Error al conectar con Gemini API: {e}", flush=True)
            return "¡Hola! Un café siempre viene bien para iniciar el día. ¡Echa un vistazo a los especiales del menú!"