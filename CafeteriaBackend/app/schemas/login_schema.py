from pydantic import BaseModel, EmailStr, Field

class LoginUsuarioInput(BaseModel):
    correo: EmailStr = Field(..., example="fernando@univerisdad.edu.mx", description="correo del usuario")
    password: str = Field(...,min_length=6, example="password123")

class LoginUsuarioResponse(BaseModel):
    usuario_id: str
    nombre_completo: str
    correo: str
    mensaje: str
    rol: str