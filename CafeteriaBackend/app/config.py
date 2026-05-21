from pydantic_settings import BaseSettings
from supabase import create_client, Client

class Settings(BaseSettings):
    supabase_url: str
    supabase_key: str
    gemini_api_key: str

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

try:
    settings = Settings()
except Exception as e:
    print(f"❌ Error al cargar las variables de entorno: {e}")
    raise e

supabase: Client = create_client(settings.supabase_url, settings.supabase_key)