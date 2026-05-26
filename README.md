# Cafeteria App

Este es el proyecto final para la cafetería de la escuela. Es una app nativa de Android (para los alumnos y los baristas) conectada a un backend en FastAPI y Supabase para poder pedir comida y café sin tener que hacer filas eternas entre clases.

---

##  Lo que hace la app

* **Para los alumnos:** Abres la app, eliges tu café o pan por categorías, le agregas tus extras (jarabes, tipo de leche, etc.), ves en tiempo real si tu pedido ya se está haciendo y vas ganando experiencia (XP) para subir de nivel como cliente VIP.
* **Para los baristas:** Tienen una pantalla tipo tablero Kanban con 3 columnas (*Por Confirmar*, *En Proceso* y *Listos*). La primera columna se actualiza sola cada 4 segundos (Polling de fondo) para que en cuanto pagues, les aparezca la tarjeta arriba sin que tengan que refrescar la pantalla.

---

##  Tecnologías que usamos

* **Android (Frontend):** Kotlin, Jetpack Compose, Retrofit 2 (para conectar con el back), OkHttp3 y Corrutinas para que no se trabe la interfaz.
* **Backend:** FastAPI (Python) y Uvicorn.
* **Base de datos:** Supabase y PostgreSQL.

---

## 🤖 Uso de Inteligencia Artificial (Sección Obligatoria)

Para el desarrollo de esta app y la entrega del reporte, nos apoyamos en herramientas de inteligencia artificial generativa. Aquí explicamos cómo estuvo el asunto:

### 1. ¿Qué herramientas usamos?
* **Gemini (Google AI)**
* **Cursor IDE**

### 2. ¿Para qué las usamos?
* **Diseño visual:** Nos ayudó a estructurar las tarjetas de Jetpack Compose, meterle los gradientes chidos a la pantalla de perfil y acomodar los `LinearProgressIndicator` para la barra de nivel de XP.
* **Lógica del Barista:** Como armar WebSockets iba a tomar siglos para el prototipo, usamos IA para diseñar un loop automático con Corrutinas (`delay(4000)`) para que la columna de pedidos entrantes hiciera consultas al servidor de fondo cada 4 segundos de forma eficiente.
* **Modelos de datos:** Para crear rápido las Data Classes de Kotlin (`PedidoBaristaResponse`, `ItemPedidoResponse`, etc.) basados en los JSON que nos mandaba FastAPI.

### 3. Partes del código con apoyo de IA
* **En el Front:** Las vistas `PerfilScreen.kt` y `BaristaHomeScreen.kt` (especialmente el carril dinámico por confirmar y los `SuggestionChip` para los extras).
* **En el Back:** La estructura del endpoint `@app.get("/barista/pedidos/porconfirmar")` para que filtrara rápido los datos desde Supabase.

### 4. ¿Cómo lo revisamos y validamos los humanos?
No dejamos el código al ahí se va; el equipo tuvo que meter mano e implementar ingeniería de verdad:
* **Quitamos errores raros:** La IA nos dio algunos parámetros viejos o que no existían de Material 3 (como errores con `strokeCap` o imports rotos de `ProgressIndicatorDefaults`), así que los borramos y corregimos a mano para que compilara.
* **Sincronización:** Renombramos y adaptamos las variables con `@SerializedName` para que hicieran match exacto con las tablas relacionales que creamos en Supabase.
* **Optimización de red:** Nos aseguramos de que el reloj de 4 segundos solo afectara a la columna de pedidos "Por Confirmar" para que el cel no se trabe ni gaste datos a lo menso refrescando todo el historial.

### 5. Links a los chats (Obligatorio)
Aquí están las conversaciones completas con las que estuvimos debuggeando y armando las pantallas por si el profe las quiere auditar:
* [Link al chat de desarrollo y corrección de código]*

https://gemini.google.com/u/1/app/0d2ef00190c29096

---

##  Cómo correr el proyecto

1. Clona el repositorio:
   git clone [https://github.com/tu-usuario/cafeteria-app.git](https://github.com/tu-usuario/cafeteria-app.git)
   
2. Abre la carpeta en Android Studio.

3. Dale al botón de Sync Project con Gradle Files.

4. En tu clase RetrofitClient, pon la IP de tu servidor de FastAPI (si usas el emulador, recuerda usar http://10.0.2.2:8000/).

5 .Dal5e Play y listo.
