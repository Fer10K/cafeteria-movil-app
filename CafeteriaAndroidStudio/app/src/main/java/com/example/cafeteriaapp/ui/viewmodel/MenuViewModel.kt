package com.example.cafeteriaapp.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeteria.data.model.ProcesarCompraResponse
import com.example.cafeteria.data.model.HistorialCompraSchema
import com.example.cafeteria.data.model.ProductoDisponibleSchema
import com.example.cafeteria.data.model.RecomendacionRequest
import com.example.cafeteriaapp.data.remote.RetrofitClient
import com.example.cafeteriaapp.data.repository.CarritoRepository
import com.example.cafeteriaapp.domain.model.OpcionExtraResponse
import com.example.cafeteriaapp.domain.model.ProductoModificado
import com.example.cafeteriaapp.domain.model.ProductoResponse
import com.example.cafeteriaapp.domain.model.toNetworkPayload
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PedidoNetworkState {
    object Idle : PedidoNetworkState
    object Loading : PedidoNetworkState
    data class Success(val pedidoId: String) : PedidoNetworkState
    data class Error(val mensaje: String) : PedidoNetworkState
}
sealed interface MenuUiState {
    object Loading : MenuUiState
    data class Success(
        val todosLosProductos: List<ProductoResponse>,
        val productosPorCategoria: Map<String, List<ProductoResponse>>
    ) : MenuUiState
    data class Error(val mensaje: String) : MenuUiState
}

sealed interface GamificacionUiState {
    object Loading : GamificacionUiState
    data class Success(val data: ProcesarCompraResponse) : GamificacionUiState
    data class Error(val message: String) : GamificacionUiState
}

class MenuViewModel: ViewModel() {
    var pedidoNetworkState by mutableStateOf<PedidoNetworkState>(PedidoNetworkState.Idle)
        private set

    private val _uiState = MutableStateFlow<MenuUiState>(MenuUiState.Loading)
    val uiState: StateFlow<MenuUiState> = _uiState

    init {
        cargarCatalogo()
    }
    var productoAEditar by mutableStateOf<ProductoResponse?>(null)
        private set

    // Lista mutable reactiva para los extras que el usuario selecciona EN TIEMPO REAL
    val extrasSeleccionadosTemporalmente = mutableStateListOf<OpcionExtraResponse>()
    //Para la ia
    private val _recomendacionState = MutableStateFlow("")
    val recomendacionState: StateFlow<String> = _recomendacionState

    // Cantidad de unidades del producto actual
    var cantidadTemporal by mutableStateOf(1)
        private set

    private val _recomendacionIaState = MutableStateFlow("")
    val recomendacionIaState: StateFlow<String> = _recomendacionIaState

    private val _isCargandoIa = MutableStateFlow(false)
    val isCargandoIa: StateFlow<Boolean> = _isCargandoIa
    fun cargarSugerenciaMaridaje(usuarioId: String) {
        // Obtenemos de forma directa la lista de la cesta actual desde tu repositorio
        val itemsActuales: List<ProductoModificado> = CarritoRepository.items

        // Si la bolsa está vacía, evitamos consumir la API de Gemini y limpiamos el estado
        if (itemsActuales.isEmpty()) {
            _recomendacionIaState.value = ""
            return
        }

        viewModelScope.launch {
            _isCargandoIa.value = true
            try {
                // 1. MAPEAR CESTA ACTUAL: Extraemos solo el nombre y categoría de la base del producto modificado
                val productosEnCesta = itemsActuales.map { itemModificado ->
                    HistorialCompraSchema(
                        productoNombre = itemModificado.productoBase.nombre
                    )
                }

                // 2. MENÚ DINÁMICO DESDE EL BACK: Obtenemos el catálogo real usando tu ProductoResponse
                val resProductos = RetrofitClient.apiService.obtenerProductos()
                val menuDisponible = if (resProductos.isSuccessful && resProductos.body() != null) {
                    resProductos.body()!!.map { prod ->
                        ProductoDisponibleSchema(
                            id = prod.id.toString(),
                            nombre = prod.nombre,
                            precio = prod.precio,
                            categoria = prod.categoriaNombre
                        )
                    }
                } else {
                    emptyList()
                }

                // 3. DISPARAR PETICIÓN DE IA: Si hay menú disponible, enviamos todo unificado
                if (menuDisponible.isNotEmpty()) {
                    val payload = RecomendacionRequest(
                        usuarioId = usuarioId,
                        tipoContexto = "carrito",
                        historial = productosEnCesta,
                        productosDisponibles = menuDisponible
                    )

                    val response = RetrofitClient.apiService.obtenerRecomendacionIa(payload)
                    if (response.isSuccessful && response.body() != null) {
                        _recomendacionIaState.value = response.body()!!.recomendacion
                    } else {
                        _recomendacionIaState.value = ""
                    }
                }
            } catch (e: Exception) {
                _recomendacionIaState.value = "" // Manejo silencioso ante pérdidas de conexión
            } finally {
                _isCargandoIa.value = false
            }
        }
    }
    fun limpiarRecomendacion() {
        _recomendacionIaState.value = ""
    }

    fun cargarRecomendacionInicio(usuarioId: String) {
        viewModelScope.launch {
            try {
                // 1. Obtención en tiempo real del menú desde tu API
                val resProductos = RetrofitClient.apiService.obtenerProductos()
                val menuDelDia = if (resProductos.isSuccessful && resProductos.body() != null) {
                    resProductos.body()!!.map { prod ->
                        ProductoDisponibleSchema(
                            id = prod.id.toString(),
                            nombre = prod.nombre,
                            precio = prod.precio,
                            categoria = prod.categoriaNombre
                        )
                    }
                } else emptyList()

                // 2. Obtención en tiempo real de los últimos pedidos del alumno
                val resHistorialStrings = RetrofitClient.apiService.obtenerPedidosUsuario(usuarioId)
                val historialEstudiante = if (resHistorialStrings.isSuccessful && resHistorialStrings.body() != null) {
                    // Convertimos la lista de strings individuales al esquema que espera la IA
                    resHistorialStrings.body()!!.map { nombrePlano ->
                        HistorialCompraSchema(
                            productoNombre = nombrePlano
                        )
                    }
                } else emptyList()

                // 3. Disparamos la petición unificada al backend si el menú cargó con éxito
                if (menuDelDia.isNotEmpty()) {
                    val payload = RecomendacionRequest(
                        usuarioId = usuarioId,
                        tipoContexto = "inicio",
                        historial = historialEstudiante,
                        productosDisponibles = menuDelDia
                    )

                    val response = RetrofitClient.apiService.obtenerRecomendacionIa(payload)
                    if (response.isSuccessful && response.body() != null) {
                        _recomendacionState.value = response.body()!!.recomendacion
                    }
                }
            } catch (e: Exception) {
                _recomendacionState.value = "${e}"
            }
        }
    }


    fun cargarCatalogo() {
        viewModelScope.launch {
            _uiState.value = MenuUiState.Loading
            try {
                // 🔍 PRINT CLAVE 2: Verificar intento de petición
                println("📡 [ANDROID DEBUG] Realizando GET a /productos...")

                val response = RetrofitClient.apiService.obtenerProductos()

                // 🔍 PRINT CLAVE 3: Ver código de respuesta HTTP
                println("📡 [ANDROID DEBUG] Código HTTP recibido: ${response}")

                if (response.isSuccessful) {
                    val cuerpo = response.body()


                    if (cuerpo != null) {

                        if (cuerpo.isNotEmpty()) {
                        }

                        val agrupados = cuerpo.groupBy { it.categoriaNombre ?: "Otros" }
                        _uiState.value = MenuUiState.Success(cuerpo, agrupados)
                    } else {
                        println("📡 [ANDROID DEBUG] El cuerpo llegó completamente NULL")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = MenuUiState.Error("Error al cargar el menú de la cafetería.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = MenuUiState.Error("Sin conexión con el servidor de la cafetería.")
            }
        }
    }
    fun abrirPersonalizacion(producto: ProductoResponse) {
        productoAEditar = producto
        cantidadTemporal = 1
        extrasSeleccionadosTemporalmente.clear()

        // Opcional: Preseleccionar opciones por defecto si min_seleccion == 1
        producto.gruposOpciones?.forEach { grupo ->
            if (grupo.minSeleccion == 1 && !grupo.opciones.isNullOrEmpty()) {
                // Preselecciona la primera opción del grupo (ej. Leche Entera)
                extrasSeleccionadosTemporalmente.add(grupo.opciones.first())
            }
        }
    }

    fun cerrarPersonalizacion() {
        productoAEditar = null
        extrasSeleccionadosTemporalmente.clear()
        cantidadTemporal = 1
    }

    fun incrementarCantidad() { cantidadTemporal++ }
    fun decrementarCantidad() { if (cantidadTemporal > 1) cantidadTemporal-- }

    // Lógica inteligente para Checkbox y RadioButton
    fun alternarOpcion(opcion: OpcionExtraResponse, maxSeleccion: Int, grupoId: Int, todasLasOpcionesDelGrupo: List<OpcionExtraResponse>) {
        if (maxSeleccion == 1) {
            // 🔘 COMPORTAMIENTO RADIO BUTTON: Quita cualquier otra opción de este grupo y pone la nueva
            extrasSeleccionadosTemporalmente.removeAll { elExtra ->
                todasLasOpcionesDelGrupo.any { it.id == elExtra.id }
            }
            extrasSeleccionadosTemporalmente.add(opcion)
        } else {
            // ☑️ COMPORTAMIENTO CHECKBOX: Si ya existe la quita, si no existe la agrega (respetando el máximo)
            if (extrasSeleccionadosTemporalmente.any { it.id == opcion.id }) {
                extrasSeleccionadosTemporalmente.removeAll { it.id == opcion.id }
            } else {
                val actualesDelGrupo = extrasSeleccionadosTemporalmente.count { elExtra ->
                    todasLasOpcionesDelGrupo.any { it.id == elExtra.id }
                }
                if (actualesDelGrupo < maxSeleccion) {
                    extrasSeleccionadosTemporalmente.add(opcion)
                }
            }
        }
    }

    fun agregarAlCarritoConfirmado() {
        val producto = productoAEditar ?: return

        val productoPersonalizadoFinal = ProductoModificado(
            productoBase = producto,
            extrasSeleccionados = extrasSeleccionadosTemporalmente.toList(),
            cantidad = cantidadTemporal
        )

        println("[CARRITO] Agregado: ${productoPersonalizadoFinal.productoBase.nombre} x${productoPersonalizadoFinal.cantidad}")
        println("[CARRITO] Precio Combinación: $${productoPersonalizadoFinal.precioTotal}")

        CarritoRepository.agregar(productoPersonalizadoFinal)
        cerrarPersonalizacion()
    }

    fun enviarPedidoAlServidor(usuarioId: String, metodoPago: String, onIdGenerado: (String) -> Unit) {
        val itemsCarrito = CarritoRepository.items
        if (itemsCarrito.isEmpty()) return

        // 1. Cambiamos el estado a cargando (para mostrar un spinner si es necesario)
        pedidoNetworkState = PedidoNetworkState.Loading

        viewModelScope.launch {
            try {

                val payload = itemsCarrito.toNetworkPayload(
                    usuarioId = usuarioId,
                    metodoPago = metodoPago
                )
                println("🚀 [FRONTEND] ENVIANDO PEDIDO A FASTAPI VIA NFC")
                println("👤 Usuario ID: ${payload.usuarioId}")
                println("💳 Método de Pago: ${payload.metodoPago}")
                println("📦 Cantidad de productos en el carrito: ${payload.items.size}")

                payload.items.forEachIndexed { index, item ->
                    println("   🛒 Producto [${index + 1}]: ${item.nombreProducto} (ID: ${item.productoId}) x${item.cantidad}")
                    println("      💰 Precio Base Unitario: $${item.precioUnitarioBase}")

                    if (item.extras.isNotEmpty()) {
                        println("      ➕ Extras seleccionados (${item.extras.size}):")
                        item.extras.forEach { extra ->
                            println("         - ${extra.nombreExtra} (ID: ${extra.extraId}) -> +$${extra.precioAdicional}")
                        }
                    } else {
                        println("      ➕ Sin extras seleccionados.")
                    }
                }
                println("🏁 [FRONTEND] FIN DEL PAYLOAD DE ENVÍO")

                val respuesta = RetrofitClient.apiService.crearPedido(payload)

                pedidoNetworkState = PedidoNetworkState.Success(respuesta.pedidoId)

                // Pasamos el ID del pedido a la UI para que inicie el Polling o el flujo correspondiente
                onIdGenerado(respuesta.pedidoId)

            } catch (e: Exception) {
                // 5. Si el servidor se cae, no hay internet o FastAPI tira un error 422/500
                pedidoNetworkState = PedidoNetworkState.Error(e.localizedMessage ?: "Error de conexión")
                println("❌ [API ERROR] No se pudo subir el pedido: ${e.message}")
            }
        }
    }
    fun resetPedidoState() {
        pedidoNetworkState = PedidoNetworkState.Idle
    }

    private val _estadoPagoEfectivo = MutableStateFlow<String>("PENDIENTE_PAGO")
    val estadoPagoEfectivo: StateFlow<String> = _estadoPagoEfectivo

    fun iniciarMonitoreoPedido(pedidoId: String) {
        viewModelScope.launch {
            var pagoConfirmado = false
            _estadoPagoEfectivo.value = "PENDIENTE_PAGO"

            while (!pagoConfirmado) {
                delay(4000)
                try {
                    val respuesta = RetrofitClient.apiService.verificarEstadoPedido(pedidoId)
                    if (respuesta.isSuccessful && respuesta.body() != null) {
                        val estadoActual = respuesta.body()!!.estado

                        if (estadoActual == "PROCESANDO") {
                            pagoConfirmado = true
                            _estadoPagoEfectivo.value = "PROCESANDO"
                        } else if (estadoActual == "CANCELADO" || estadoActual == "RECHAZADO") {
                            pagoConfirmado = true
                            _estadoPagoEfectivo.value = estadoActual
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Error en polling de ViewModel: ${e.message}")
                }
            }
        }
    }
}

