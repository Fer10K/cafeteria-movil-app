from pydantic import BaseModel, EmailStr, Field

class RegistroUsuarioInput(BaseModel):
    correo: EmailStr = Field(..., example="fernando@universidad.edu.mx", description="Correo institucional del alumno")
    password: str = Field(..., min_length=6, example="password123", description="Contraseña segura (mínimo 6 caracteres)")
    nombre_completo: str = Field(..., example="Fernando", description="Nombre del estudiante")

class RegistroUsuarioResponse(BaseModel):
    usuario_id: str
    mensaje: str