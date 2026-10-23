# Cafetería Móvil App 🍵

Sistema completo para la cafetería universitaria: app Android nativa para alumnos (pedir, pagar y seguir pedidos con gamificación) y API REST en FastAPI para la operación de los baristas en tiempo real.

## Características

### Para el alumno (Android)
- **Catálogo por categorías**: Café, Fríos y Repostería, con personalización de productos (jarabes, tipo de leche, etc.).
- **Carrito y pago**: pago NFC o efectivo en caja.
- **Seguimiento en tiempo real** del estado del pedido: Pendiente de pago → Procesando → Listo → Entregado.
- **Gamificación**: gana 1 XP por peso gastado, sube de nivel, desbloquea logros y compite en una tabla de posiciones global.
- **Recomendaciones con IA**: banner inteligente con Gemini en el inicio (historial), el carrito (maridaje) y la gamificación (estrategia).
- **Notificación por correo** cuando el pedido está listo.

### Para el barista (Android)
- **Tablero Kanban** con columnas: Por Confirmar, En Proceso y Listos.
- **Alertas en tiempo real** por WebSocket cuando entra un pedido nuevo.
- Históricos de entregados y cancelados.

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| App móvil | Kotlin 2.2, Jetpack Compose, Material 3, Retrofit, OkHttp (WebSocket), Coil |
| Backend | Python 3.11, FastAPI, SQLAlchemy, Uvicorn |
| Base de datos | PostgreSQL |
| IA | Google Gemini (`gemini-2.5-flash`) |
| Despliegue | Docker, Render |
| Tiempo real | FastAPI WebSocket |

## Estructura del proyecto

```
cafeteria-movil-app/
├── CafeteriaAndroidStudio/   # App Android (Kotlin + Jetpack Compose)
│   └── app/src/main/java/com/example/cafeteriaapp/
│       ├── data/             # Retrofit, sesión local, repositorio de carrito
│       ├── domain/           # Modelos y repositorios
│       └── ui/               # Pantallas, ViewModels y tema
└── CafeteriaBackend/         # API Python (FastAPI)
    └── app/
        ├── main.py           # Endpoints y middleware
        ├── config.py         # Configuración (.env)
        ├── tablas.sql        # Esquema de la base de datos (migraciones)
        ├── schemas/          # Modelos Pydantic
        └── services/         # Lógica: pedidos, IA, gamificación, auth, WebSocket
```

## Requisitos

- Python 3.11+ (o Docker)
- Android Studio (para la app móvil)
- Una base de datos PostgreSQL con el esquema de `CafeteriaBackend/app/tablas.sql`
- API key de Google Gemini

> **Nota:** el servicio desplegado en Render ha expirado. Las migraciones (esquema SQL) están incluidas en el repositorio, así que para ejecutar el backend en local solo necesitas tu propia instancia de PostgreSQL.

## Instalación y ejecución

### 1. Backend

**Con Docker (recomendado):**
```bash
cd CafeteriaBackend
docker compose up --build
```

**En local:**
```bash
cd CafeteriaBackend
python -m venv .venv
# Windows: .venv\Scripts\activate — Linux/Mac: source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

La documentación interactiva queda en `http://localhost:8000/docs` (Swagger).

Antes de arrancar, ejecuta el esquema SQL (`app/tablas.sql`) en tu base de datos PostgreSQL.

### 2. App Android

1. Abre `CafeteriaAndroidStudio/` en Android Studio y sincroniza Gradle.
2. Configura la URL del backend en `data/remote/RetrofitClient.kt`:
   - Emulador local: `http://10.0.2.2:8000/`
3. Ejecuta la app en un emulador o dispositivo (minSdk 24).

### 3. Variables de entorno

Crea un archivo `CafeteriaBackend/.env`:

```env
DATABASE_URL=postgresql://usuario:contraseña@host:5432/base
SMTP_USER=xxx
SMTP_PASSWORD=xxx
GEMINI_API_KEY=xxx
```

| Variable | Descripción | Obligatoria |
|---|---|---|
| `DATABASE_URL` | Cadena de conexión a PostgreSQL | Sí |
| `GEMINI_API_KEY` | API key de Google Gemini | Sí |
| `SMTP_USER` | Correo remitente (notificación "pedido listo") | No |
| `SMTP_PASSWORD` | Contraseña de app del correo | No |

## API

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Healthcheck |
| POST | `/auth/registro` · `/auth/login` | Autenticación |
| GET | `/productos` | Catálogo con opciones y extras |
| POST | `/pedidos` | Crear pedido |
| GET | `/pedidos/{id}/status` | Estado del pedido |
| POST/GET | `/gamificacion/*` | XP, nivel, logros y leaderboard |
| POST | `/ai/recomendar` | Recomendaciones con Gemini |
| GET/PATCH/WS | `/barista/pedidos` · `/ws/baristas` | Tablero barista y alertas |

## Uso de Inteligencia Artificial (sección obligatoria)

Para el desarrollo de esta app nos apoyamos en **Google Gemini** y **Cursor IDE**:

- **Diseño visual:** estructura de tarjetas en Jetpack Compose, gradientes de la pantalla de perfil y barras de nivel de XP.
- **Lógica del barista:** diseño del loop de consultas con corrutinas (`delay(4000)`) para la columna de pedidos entrantes.
- **Modelos de datos:** generación de Data Classes de Kotlin a partir de los JSON de FastAPI.
- **Revisión humana:** el equipo corrigió imports y parámetros obsoletos de Material 3, adaptó variables con `@SerializedName` y optimizó el polling para que solo refrescara la columna «Por Confirmar».

Conversación de referencia: [ver chat en Gemini](https://gemini.google.com/u/1/app/0d2ef00190c29096).

## Autor

Desarrollado por [Fer10K](https://github.com/Fer10K) — proyecto académico.
