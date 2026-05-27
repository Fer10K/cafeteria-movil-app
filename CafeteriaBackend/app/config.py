import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # La API Key de Gemini que requiere tu AIService
    gemini_api_key: str
    
    # Tu nueva cadena de conexión a PostgreSQL de Render
    database_url: str

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        # Permite que si hay variables extra en el .env (o si faltan temporalmente en desarrollo), 
        # no tumbe el inicio del servidor de forma tan estricta
        extra = "ignore"

try:
    settings = Settings()
    
    # CORRECCIÓN DE PREVENCIÓN: Fix clásico de compatibilidad para el prefijo de la URL
    if settings.database_url and settings.database_url.startswith("postgres://"):
        settings.database_url = settings.database_url.replace("postgres://", "postgresql://", 1)
        # Seteamos de vuelta en las variables de entorno del sistema para que psycopg2 lo lea sin problemas
        os.environ["DATABASE_URL"] = settings.database_url

except Exception as e:
    print(f"❌ Error crítico al cargar las variables de entorno en config.py: {e}")
    raise e