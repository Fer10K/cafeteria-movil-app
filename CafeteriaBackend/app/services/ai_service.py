import google.generativeai as genai
from app.config import settings

class AIService:
    def __init__(self):
        genai.configure(api_key=settings.gemini_api_key)
        self.model = genai.GenerativeModel("gemini-2.5-flash")

    async def obtener_recomendacion_menu(self, historial_usuario: list, productos_disponibles: list) -> str:
        """
        Analiza el historial de consumo de un estudiante y el inventario disponible
        en la cafetería para recomendarle el combo ideal.
        """
        
        prompt = f"""
        Eres el asistente inteligente de la cafetería universitaria. Tu objetivo es recomendar de manera amigable,
        inclusiva y muy breve un producto o combo ideal para un estudiante basado en su historial y el menú de hoy.

        Historial de compras recientes del estudiante:
        {historial_usuario}

        Productos disponibles hoy en la cafetería (con precio y stock):
        {productos_disponibles}

        Instrucciones estrictas:
        1. Tu respuesta debe ser corta (máximo 3 líneas) porque se mostrará en una pantalla móvil.
        2. Usa un tono juvenil y motivador.
        3. Si notas que consume mucho café, sugiérele alguna alternativa saludable disponible o el combo del día.
        4. No inventes productos que no estén en la lista de disponibles.
        """

        try:
            response = await self.model.generate_content_async(prompt)
            return response.text.strip()
        except Exception as e:
            print(f"❌ Error al conectar con Gemini API: {e}")
            return "¡Hola! Hoy te recomendamos revisar los especiales del día en nuestra barra principal. ¡Buen provecho!"
